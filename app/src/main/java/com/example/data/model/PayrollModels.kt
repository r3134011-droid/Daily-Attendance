package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AttendanceStatus(val code: String, val label: String) {
  NONE("", "Not Set"),
  PRESENT("P", "Present"),
  ABSENT("A", "Absent"),
  HOLIDAY("H", "Holiday"),
  WEEKLY_OFF("W", "Weekly Off");

  companion object {
    fun fromCode(code: String?): AttendanceStatus =
      entries.firstOrNull { it.code == code } ?: NONE
  }
}

@IgnoreExtraProperties
data class PayrollSettings(
  val userId: String = "",
  val startDay: Long = 25L,
  val salary: Double = 0.0,
  val otRate: Double = 0.0,
  val pf: Double = 0.0,
  val esi: Double = 0.0,
  val advance: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AttendanceRecord(
  @DocumentId val dateKey: String = "",
  val userId: String = "",
  val date: String = "",
  val status: String = "",
  val otHours: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val statusEnum: AttendanceStatus
    get() = AttendanceStatus.fromCode(status)
}
