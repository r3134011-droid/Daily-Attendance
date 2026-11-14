package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayrollSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AttendanceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun settingsDoc(userId: String) =
    db.collection("users").document(userId).collection("payroll").document("settings")

  private fun attendanceCollection(userId: String) =
    db.collection("users").document(userId).collection("attendance")

  fun observeSettings(userId: String): Flow<PayrollSettings> = flow {
    val docRef = settingsDoc(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          snapshot.toObject(
            PayrollSettings::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ) ?: PayrollSettings(userId = userId)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  fun observeAttendance(userId: String): Flow<Map<String, AttendanceRecord>> = flow {
    val colRef = attendanceCollection(userId)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AttendanceRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ).associateBy { it.dateKey.ifBlank { it.date } }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun getSettings(userId: String): Result<PayrollSettings?> {
    val docRef = settingsDoc(userId)
    return try {
      val snapshot = docRef.get().await()
      val settings =
        snapshot.toObject(
          PayrollSettings::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      Result.success(settings)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveSettings(settings: PayrollSettings): Result<Unit> {
    val uid = requireUserId()
    val docRef = settingsDoc(uid)
    val startDay = settings.startDay.coerceIn(1L, 31L)
    val salary = settings.salary.coerceAtLeast(0.0)
    val otRate = settings.otRate.coerceAtLeast(0.0)
    val pf = settings.pf.coerceAtLeast(0.0)
    val esi = settings.esi.coerceAtLeast(0.0)
    val advance = settings.advance.coerceAtLeast(0.0)

    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "startDay" to startDay,
            "salary" to salary,
            "otRate" to otRate,
            "pf" to pf,
            "esi" to esi,
            "advance" to advance,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "startDay" to startDay,
            "salary" to salary,
            "otRate" to otRate,
            "pf" to pf,
            "esi" to esi,
            "advance" to advance,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveAttendanceDay(
    dateKey: String,
    status: AttendanceStatus,
    otHours: Double,
  ): Result<Unit> {
    val uid = requireUserId()
    val cleanOt = otHours.coerceIn(0.0, 24.0)
    val docRef = attendanceCollection(uid).document(dateKey)

    if (status == AttendanceStatus.NONE && cleanOt <= 0.0) {
      return deleteAttendanceDay(dateKey)
    }

    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "status" to status.code,
            "otHours" to cleanOt,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "date" to dateKey,
            "status" to status.code,
            "otHours" to cleanOt,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAttendanceDay(dateKey: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = attendanceCollection(uid).document(dateKey)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun clearPeriod(dateKeys: List<String>): Result<Unit> {
    val uid = requireUserId()
    if (dateKeys.isEmpty()) return Result.success(Unit)
    val colRef = attendanceCollection(uid)
    return try {
      dateKeys.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { key ->
          batch.delete(colRef.document(key))
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun restoreBackup(
    settings: PayrollSettings,
    entries: Map<String, Pair<AttendanceStatus, Double>>,
  ): Result<Unit> {
    val uid = requireUserId()
    val colRef = attendanceCollection(uid)
    return try {
      saveSettings(settings).getOrThrow()

      val existingDocs = colRef.get().await().documents.map { it.id }.toSet()
      val ops = mutableListOf<Pair<String, Boolean>>() // dateKey to exists
      val allKeys = (existingDocs + entries.keys).toSet()

      // Delete entries not in backup, upsert entries in backup
      allKeys.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { key ->
          val docRef = colRef.document(key)
          val value = entries[key]
          if (value == null || (value.first == AttendanceStatus.NONE && value.second <= 0.0)) {
            if (key in existingDocs) {
              batch.delete(docRef)
            }
          } else {
            val cleanOt = value.second.coerceIn(0.0, 24.0)
            if (key in existingDocs) {
              batch.update(
                docRef,
                mapOf(
                  "status" to value.first.code,
                  "otHours" to cleanOt,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            } else {
              batch.set(
                docRef,
                mapOf(
                  "userId" to uid,
                  "date" to key,
                  "status" to value.first.code,
                  "otHours" to cleanOt,
                  "createdAt" to FieldValue.serverTimestamp(),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
          }
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun getRecord(userId: String, dateKey: String): Result<AttendanceRecord?> {
    val docRef = attendanceCollection(userId).document(dateKey)
    return try {
      val snapshot = docRef.get().await()
      Result.success(
        snapshot.toObject(
          AttendanceRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }
}
