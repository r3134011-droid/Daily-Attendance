package com.example.domain

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayrollSettings
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class PayrollPeriod(
  val start: PayrollDate,
  val end: PayrollDate,
  val dates: List<PayrollDate>,
)

data class PayrollSummary(
  val totalDays: Int = 0,
  val presentCount: Int = 0,
  val absentCount: Int = 0,
  val holidayCount: Int = 0,
  val weeklyOffCount: Int = 0,
  val paidDays: Int = 0,
  val totalOtHours: Double = 0.0,
  val perDaySalary: Double = 0.0,
  val basicEarnedSalary: Double = 0.0,
  val otAmount: Double = 0.0,
  val grossSalary: Double = 0.0,
  val pfDeduction: Double = 0.0,
  val esiDeduction: Double = 0.0,
  val advanceDeduction: Double = 0.0,
  val totalDeductions: Double = 0.0,
  val netSalary: Double = 0.0,
)

object PayrollCalculator {

  fun computePeriod(year: Int, month: Int, startDay: Int): PayrollPeriod {
    val safeStartDay = startDay.coerceIn(1, 31)
    val lastOfMonth = PayrollDate.daysInMonth(year, month)
    val start = PayrollDate(year, month, min(safeStartDay, lastOfMonth))
    val end = start.plusMonths(1).plusDays(-1)

    val dates = mutableListOf<PayrollDate>()
    var cursor = start
    var guard = 0
    while (cursor <= end && guard < 40) {
      dates.add(cursor)
      cursor = cursor.plusDays(1)
      guard++
    }
    return PayrollPeriod(start = start, end = end, dates = dates)
  }

  fun computeSummary(
    period: PayrollPeriod,
    settings: PayrollSettings,
    records: Map<String, AttendanceRecord>,
  ): PayrollSummary {
    var present = 0
    var absent = 0
    var holiday = 0
    var weeklyOff = 0
    var totalOt = 0.0

    for (date in period.dates) {
      val rec = records[date.toIsoKey()]
      if (rec != null) {
        when (rec.statusEnum) {
          AttendanceStatus.PRESENT -> present++
          AttendanceStatus.ABSENT -> absent++
          AttendanceStatus.HOLIDAY -> holiday++
          AttendanceStatus.WEEKLY_OFF -> weeklyOff++
          AttendanceStatus.NONE -> Unit
        }
        totalOt += rec.otHours.coerceAtLeast(0.0)
      }
    }

    val totalDays = period.dates.size
    val paidDays = present + holiday + weeklyOff
    val salary = settings.salary.coerceAtLeast(0.0)
    val otRate = settings.otRate.coerceAtLeast(0.0)
    val pf = settings.pf.coerceAtLeast(0.0)
    val esi = settings.esi.coerceAtLeast(0.0)
    val advance = settings.advance.coerceAtLeast(0.0)

    val perDay = if (totalDays > 0) salary / totalDays.toDouble() else 0.0
    val earned = perDay * paidDays.toDouble()
    val otMoney = totalOt * otRate
    val gross = earned + otMoney
    val deductions = pf + esi + advance
    val net = max(0.0, gross - deductions)

    return PayrollSummary(
      totalDays = totalDays,
      presentCount = present,
      absentCount = absent,
      holidayCount = holiday,
      weeklyOffCount = weeklyOff,
      paidDays = paidDays,
      totalOtHours = totalOt,
      perDaySalary = perDay,
      basicEarnedSalary = earned,
      otAmount = otMoney,
      grossSalary = gross,
      pfDeduction = pf,
      esiDeduction = esi,
      advanceDeduction = advance,
      totalDeductions = deductions,
      netSalary = net,
    )
  }

  fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
      minimumFractionDigits = 2
      maximumFractionDigits = 2
    }
    return "₹${formatter.format(amount)}"
  }

  fun formatHours(hours: Double): String {
    return if (hours % 1.0 == 0.0) {
      hours.toInt().toString()
    } else {
      String.format(Locale.US, "%.1f", hours)
    }
  }
}
