package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

data class PayrollDate(
  val year: Int,
  val month: Int, // 1..12
  val day: Int,   // 1..31
) : Comparable<PayrollDate> {

  fun toIsoKey(): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

  /** 0 = Sunday, 1 = Monday, ..., 6 = Saturday */
  fun dayOfWeekSundayZero(): Int {
    val cal = GregorianCalendar(year, month - 1, day)
    return cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
  }

  fun plusDays(days: Int): PayrollDate {
    val cal = GregorianCalendar(year, month - 1, day)
    cal.add(Calendar.DAY_OF_MONTH, days)
    return PayrollDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun plusMonths(months: Int): PayrollDate {
    val cal = GregorianCalendar(year, month - 1, day)
    cal.add(Calendar.MONTH, months)
    return PayrollDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun formatDisplayLong(): String {
    val cal = GregorianCalendar(year, month - 1, day)
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("en", "IN"))
    return sdf.format(cal.time)
  }

  fun formatShortDayMonth(): String {
    val cal = GregorianCalendar(year, month - 1, day)
    val sdf = SimpleDateFormat("d MMM", Locale("en", "IN"))
    return sdf.format(cal.time)
  }

  override fun compareTo(other: PayrollDate): Int {
    if (year != other.year) return year.compareTo(other.year)
    if (month != other.month) return month.compareTo(other.month)
    return day.compareTo(other.day)
  }

  companion object {
    val MONTH_NAMES = listOf(
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    )

    val DAY_HEADERS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun today(): PayrollDate {
      val cal = Calendar.getInstance()
      return PayrollDate(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
      )
    }

    fun daysInMonth(year: Int, month: Int): Int {
      val cal = GregorianCalendar(year, month - 1, 1)
      return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun parse(isoKey: String): PayrollDate? {
      val parts = isoKey.split("-")
      if (parts.size != 3) return null
      val y = parts[0].toIntOrNull() ?: return null
      val m = parts[1].toIntOrNull() ?: return null
      val d = parts[2].toIntOrNull() ?: return null
      if (m !in 1..12 || d !in 1..daysInMonth(y, m)) return null
      return PayrollDate(y, m, d)
    }
  }
}
