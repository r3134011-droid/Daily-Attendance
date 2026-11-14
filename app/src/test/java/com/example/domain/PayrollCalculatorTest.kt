package com.example.domain

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayrollSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PayrollCalculatorTest {

  @Test
  fun computePeriod_startDay25_matchesExpectedRange() {
    val period = PayrollCalculator.computePeriod(year = 2026, month = 11, startDay = 25)
    assertEquals("2026-11-25", period.start.toIsoKey())
    assertEquals("2026-12-24", period.end.toIsoKey())
    assertEquals(30, period.dates.size)
  }

  @Test
  fun computeSummary_calculatesPaidDaysOtAndNetSalaryAccurately() {
    val period = PayrollCalculator.computePeriod(year = 2026, month = 11, startDay = 25)
    val settings = PayrollSettings(
      userId = "u1",
      startDay = 25L,
      salary = 30000.0, // 1000 per day for 30 days
      otRate = 150.0,
      pf = 1800.0,
      esi = 200.0,
      advance = 500.0,
    )
    val records = mapOf(
      "2026-11-25" to AttendanceRecord(dateKey = "2026-11-25", status = "P", otHours = 2.0),
      "2026-11-26" to AttendanceRecord(dateKey = "2026-11-26", status = "H", otHours = 0.0),
      "2026-11-27" to AttendanceRecord(dateKey = "2026-11-27", status = "W", otHours = 0.0),
      "2026-11-28" to AttendanceRecord(dateKey = "2026-11-28", status = "A", otHours = 0.0),
    )

    val summary = PayrollCalculator.computeSummary(period, settings, records)
    assertEquals(30, summary.totalDays)
    assertEquals(1, summary.presentCount)
    assertEquals(1, summary.absentCount)
    assertEquals(1, summary.holidayCount)
    assertEquals(1, summary.weeklyOffCount)
    assertEquals(3, summary.paidDays)
    assertEquals(2.0, summary.totalOtHours, 0.01)
    assertEquals(1000.0, summary.perDaySalary, 0.01)
    assertEquals(3000.0, summary.basicEarnedSalary, 0.01)
    assertEquals(300.0, summary.otAmount, 0.01)
    assertEquals(3300.0, summary.grossSalary, 0.01)
    assertEquals(2500.0, summary.totalDeductions, 0.01)
    assertEquals(800.0, summary.netSalary, 0.01)
  }

  @Test
  fun backupCodec_roundTripsJsonAndExportsBomCsv() {
    val settings = PayrollSettings(userId = "u1", startDay = 25L, salary = 25000.0, otRate = 100.0)
    val records = mapOf(
      "2026-11-25" to AttendanceRecord(dateKey = "2026-11-25", date = "2026-11-25", status = "P", otHours = 2.5)
    )
    val json = BackupCodec.exportBackupJson(settings, records)
    val (parsedSettings, parsedEntries) = BackupCodec.parseBackupJson(json, "u1")

    assertEquals(25L, parsedSettings.startDay)
    assertEquals(25000.0, parsedSettings.salary, 0.01)
    assertEquals(AttendanceStatus.PRESENT, parsedEntries["2026-11-25"]?.first)
    assertEquals(2.5, parsedEntries["2026-11-25"]?.second ?: 0.0, 0.01)

    val period = PayrollCalculator.computePeriod(2026, 11, 25)
    val csv = BackupCodec.exportCsv(period, records)
    assertTrue(csv.startsWith("\uFEFF\"Date\",\"Attendance\",\"OT Hours\""))
    assertTrue(csv.contains("\"2026-11-25\",\"P\",\"2.5\""))
  }
}
