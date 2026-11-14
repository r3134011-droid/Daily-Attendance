package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayrollSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveSettings_authenticatedOwner_createsAndUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = AttendanceRepository(firestore, auth)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveSettings(
        PayrollSettings(
          userId = uid,
          startDay = 25L,
          salary = 25000.0,
          otRate = 120.0,
          pf = 1800.0,
          esi = 200.0,
          advance = 500.0,
        )
      )
    }
    assertTrue(createResult.isSuccess)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveSettings(
        PayrollSettings(
          userId = uid,
          startDay = 1L,
          salary = 30000.0,
          otRate = 150.0,
          pf = 1800.0,
          esi = 200.0,
          advance = 0.0,
        )
      )
    }
    assertTrue(updateResult.isSuccess)

    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getSettings(uid).getOrThrow() }
    assertNotNull(fetched)
    assertEquals(1L, fetched?.startDay)
    assertEquals(30000.0, fetched?.salary ?: 0.0, 0.01)
  }

  @Test
  fun saveAndObserveAttendance_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = AttendanceRepository(firestore, auth)

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveAttendanceDay("2026-11-13", AttendanceStatus.PRESENT, 2.5)
    }
    assertTrue(saveResult.isSuccess)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAttendance(uid).first { it.containsKey("2026-11-13") }
    }
    val record = observed["2026-11-13"]
    assertNotNull(record)
    assertEquals("P", record?.status)
    assertEquals(2.5, record?.otHours ?: 0.0, 0.01)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveAttendanceDay("2026-11-13", AttendanceStatus.HOLIDAY, 4.0)
    }
    assertTrue(updateResult.isSuccess)

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteAttendanceDay("2026-11-13")
    }
    assertTrue(deleteResult.isSuccess)

    val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.getRecord(uid, "2026-11-13").getOrThrow()
    }
    assertNull(afterDelete)
  }

  @Test
  fun getRecord_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = AttendanceRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.saveAttendanceDay("2026-11-14", AttendanceStatus.PRESENT, 1.5).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = AttendanceRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getRecord(aliceUid, "2026-11-14")
    }
    assertTrue(result.isFailure)
    val fsException = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(fsException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
  }

  @Test
  fun observeAttendance_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = AttendanceRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAttendance("some_user_id").first()
      }
    } catch (t: Throwable) {
      caught = t
    }

    assertNotNull(caught)
    val fsException = caught?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_attendance@test.com"
    const val BOB_EMAIL = "bob_attendance@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
