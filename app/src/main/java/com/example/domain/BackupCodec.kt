package com.example.domain

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayrollSettings
import org.json.JSONObject

object BackupCodec {

  private val dateKeyRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

  fun exportCsv(period: PayrollPeriod, records: Map<String, AttendanceRecord>): String {
    val sb = StringBuilder("\uFEFF")
    sb.append("\"Date\",\"Attendance\",\"OT Hours\"\r\n")
    for (date in period.dates) {
      val key = date.toIsoKey()
      val rec = records[key]
      val status = rec?.status ?: ""
      val ot = rec?.otHours ?: 0.0
      val otText = if (ot % 1.0 == 0.0) ot.toInt().toString() else ot.toString()
      sb.append("\"$key\",\"$status\",\"$otText\"\r\n")
    }
    return sb.toString()
  }

  fun exportBackupJson(
    settings: PayrollSettings,
    records: Map<String, AttendanceRecord>,
  ): String {
    val attendanceJson = JSONObject()
    val overtimeJson = JSONObject()

    records.toSortedMap().forEach { (key, record) ->
      if (record.status.isNotBlank()) {
        attendanceJson.put(key, record.status)
      }
      if (record.otHours > 0.0) {
        overtimeJson.put(key, record.otHours)
      }
    }

    val settingsJson = JSONObject().apply {
      put("startDay", settings.startDay)
      put("salary", settings.salary)
      put("otRate", settings.otRate)
      put("pf", settings.pf)
      put("esi", settings.esi)
      put("advance", settings.advance)
    }

    return JSONObject().apply {
      put("attendance", attendanceJson)
      put("overtime", overtimeJson)
      put("settings", settingsJson)
    }.toString(2)
  }

  fun parseBackupJson(
    jsonText: String,
    userId: String,
  ): Pair<PayrollSettings, Map<String, Pair<AttendanceStatus, Double>>> {
    val root = JSONObject(jsonText)
    if (!root.has("attendance") || !root.has("overtime")) {
      throw IllegalArgumentException("Invalid backup file: missing attendance or overtime fields.")
    }

    val attendanceObj = root.getJSONObject("attendance")
    val overtimeObj = root.getJSONObject("overtime")
    val settingsObj = root.optJSONObject("settings") ?: JSONObject()

    val settings = PayrollSettings(
      userId = userId,
      startDay = settingsObj.optLong("startDay", 25L).coerceIn(1L, 31L),
      salary = settingsObj.optDouble("salary", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
      otRate = settingsObj.optDouble("otRate", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
      pf = settingsObj.optDouble("pf", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
      esi = settingsObj.optDouble("esi", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
      advance = settingsObj.optDouble("advance", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
    )

    val allKeys = mutableSetOf<String>()
    attendanceObj.keys().forEach { if (dateKeyRegex.matches(it)) allKeys.add(it) }
    overtimeObj.keys().forEach { if (dateKeyRegex.matches(it)) allKeys.add(it) }

    val entries = mutableMapOf<String, Pair<AttendanceStatus, Double>>()
    for (key in allKeys) {
      val status = AttendanceStatus.fromCode(attendanceObj.optString(key, ""))
      val otRaw = overtimeObj.optDouble(key, 0.0)
      val ot = if (otRaw.isFinite()) otRaw.coerceIn(0.0, 24.0) else 0.0
      if (status != AttendanceStatus.NONE || ot > 0.0) {
        entries[key] = status to ot
      }
    }

    return settings to entries
  }
}
