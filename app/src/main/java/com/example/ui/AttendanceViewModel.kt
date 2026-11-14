package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.PayrollSettings
import com.example.data.repository.AttendanceRepository
import com.example.domain.BackupCodec
import com.example.domain.PayrollCalculator
import com.example.domain.PayrollDate
import com.example.domain.PayrollPeriod
import com.example.domain.PayrollSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StreamState<out T> {
  data object Loading : StreamState<Nothing>
  data class Success<T>(val data: T) : StreamState<T>
  data class Error(val message: String) : StreamState<Nothing>
}

data class SettingsFormDraft(
  val startDay: Int = 25,
  val salaryText: String = "",
  val otRateText: String = "",
  val pfText: String = "",
  val esiText: String = "",
  val advanceText: String = "",
  val initializedFromServer: Boolean = false,
) {
  fun toPayrollSettings(userId: String): PayrollSettings = PayrollSettings(
    userId = userId,
    startDay = startDay.coerceIn(1, 31).toLong(),
    salary = salaryText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
    otRate = otRateText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
    pf = pfText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
    esi = esiText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
    advance = advanceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
  )

  companion object {
    fun from(settings: PayrollSettings): SettingsFormDraft = SettingsFormDraft(
      startDay = settings.startDay.toInt().coerceIn(1, 31),
      salaryText = formatPlainNumber(settings.salary),
      otRateText = formatPlainNumber(settings.otRate),
      pfText = formatPlainNumber(settings.pf),
      esiText = formatPlainNumber(settings.esi),
      advanceText = formatPlainNumber(settings.advance),
      initializedFromServer = true,
    )

    private fun formatPlainNumber(value: Double): String {
      if (value <= 0.0) return ""
      return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
  }
}

data class AttendanceDashboardState(
  val today: PayrollDate = PayrollDate.today(),
  val selectedYear: Int = PayrollDate.today().year.coerceIn(2026, 2100),
  val selectedMonth: Int = PayrollDate.today().month,
  val settings: PayrollSettings = PayrollSettings(),
  val settingsDraft: SettingsFormDraft = SettingsFormDraft(),
  val records: Map<String, AttendanceRecord> = emptyMap(),
  val period: PayrollPeriod = PayrollCalculator.computePeriod(
    PayrollDate.today().year.coerceIn(2026, 2100),
    PayrollDate.today().month,
    25,
  ),
  val summary: PayrollSummary = PayrollSummary(),
  val isLoading: Boolean = true,
  val streamError: String? = null,
  val isSaving: Boolean = false,
  val editingDateKey: String? = null,
)

class AttendanceViewModel(
  private val repository: AttendanceRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val today = PayrollDate.today()
  private val selectedYearFlow = MutableStateFlow(today.year.coerceIn(2026, 2100))
  private val selectedMonthFlow = MutableStateFlow(today.month)
  private val draftFlow = MutableStateFlow(SettingsFormDraft())
  private val editingDateKeyFlow = MutableStateFlow<String?>(null)
  private val isSavingFlow = MutableStateFlow(false)

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val settingsStream: StateFlow<StreamState<PayrollSettings>> =
    repository.observeSettings(currentUserId)
      .map<PayrollSettings, StreamState<PayrollSettings>> { serverSettings ->
        if (!draftFlow.value.initializedFromServer) {
          draftFlow.value = SettingsFormDraft.from(serverSettings)
        }
        StreamState.Success(serverSettings)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing payroll settings", rootError)
        emit(StreamState.Error(rootError.message ?: "Failed to load payroll settings"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StreamState.Loading,
      )

  private val attendanceStream: StateFlow<StreamState<Map<String, AttendanceRecord>>> =
    repository.observeAttendance(currentUserId)
      .map<Map<String, AttendanceRecord>, StreamState<Map<String, AttendanceRecord>>> { records ->
        StreamState.Success(records)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing attendance records", rootError)
        emit(StreamState.Error(rootError.message ?: "Failed to load attendance records"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StreamState.Loading,
      )

  val uiState: StateFlow<AttendanceDashboardState> = combine(
    selectedYearFlow,
    selectedMonthFlow,
    draftFlow,
    settingsStream,
    attendanceStream,
    editingDateKeyFlow,
    isSavingFlow,
  ) { values ->
    val year = values[0] as Int
    val month = values[1] as Int
    val draft = values[2] as SettingsFormDraft
    @Suppress("UNCHECKED_CAST")
    val sState = values[3] as StreamState<PayrollSettings>
    @Suppress("UNCHECKED_CAST")
    val aState = values[4] as StreamState<Map<String, AttendanceRecord>>
    val editingKey = values[5] as String?
    val isSaving = values[6] as Boolean

    val activeSettings = when (sState) {
      is StreamState.Success -> {
        if (draft.initializedFromServer) draft.toPayrollSettings(currentUserId) else sState.data
      }
      else -> draft.toPayrollSettings(currentUserId)
    }
    val records = (aState as? StreamState.Success)?.data ?: emptyMap()
    val period = PayrollCalculator.computePeriod(year, month, activeSettings.startDay.toInt())
    val summary = PayrollCalculator.computeSummary(period, activeSettings, records)

    val streamErr = when {
      sState is StreamState.Error -> sState.message
      aState is StreamState.Error -> aState.message
      else -> null
    }

    AttendanceDashboardState(
      today = PayrollDate.today(),
      selectedYear = year,
      selectedMonth = month,
      settings = activeSettings,
      settingsDraft = draft,
      records = records,
      period = period,
      summary = summary,
      isLoading = (sState is StreamState.Loading || aState is StreamState.Loading),
      streamError = streamErr,
      isSaving = isSaving,
      editingDateKey = editingKey,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = AttendanceDashboardState(),
  )

  fun goToday() {
    val now = PayrollDate.today()
    selectedYearFlow.value = now.year.coerceIn(2026, 2100)
    selectedMonthFlow.value = now.month
  }

  fun selectYear(year: Int) {
    selectedYearFlow.value = year.coerceIn(2026, 2100)
  }

  fun selectMonth(month: Int) {
    selectedMonthFlow.value = month.coerceIn(1, 12)
  }

  fun openDayEditor(dateKey: String) {
    editingDateKeyFlow.value = dateKey
  }

  fun closeDayEditor() {
    editingDateKeyFlow.value = null
  }

  fun saveDayEntry(dateKey: String, status: AttendanceStatus, otHours: Double) {
    if (!otHours.isFinite() || otHours < 0.0 || otHours > 24.0) {
      _feedbackMessage.value = "Please enter valid overtime hours (0 to 24)."
      return
    }
    viewModelScope.launch {
      isSavingFlow.value = true
      val res = repository.saveAttendanceDay(dateKey, status, otHours)
      isSavingFlow.value = false
      res.onSuccess {
        editingDateKeyFlow.value = null
        _feedbackMessage.value = "Saved entry for $dateKey"
      }.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "Failed to save attendance entry."
      }
    }
  }

  fun deleteDayEntry(dateKey: String) {
    viewModelScope.launch {
      isSavingFlow.value = true
      val res = repository.deleteAttendanceDay(dateKey)
      isSavingFlow.value = false
      res.onSuccess {
        editingDateKeyFlow.value = null
        _feedbackMessage.value = "Deleted entry for $dateKey"
      }.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "Failed to delete attendance entry."
      }
    }
  }

  fun updateDraftStartDay(startDay: Int) {
    draftFlow.update { it.copy(startDay = startDay.coerceIn(1, 31), initializedFromServer = true) }
  }

  fun updateDraftSalary(text: String) {
    draftFlow.update { it.copy(salaryText = text, initializedFromServer = true) }
  }

  fun updateDraftOtRate(text: String) {
    draftFlow.update { it.copy(otRateText = text, initializedFromServer = true) }
  }

  fun updateDraftPf(text: String) {
    draftFlow.update { it.copy(pfText = text, initializedFromServer = true) }
  }

  fun updateDraftEsi(text: String) {
    draftFlow.update { it.copy(esiText = text, initializedFromServer = true) }
  }

  fun updateDraftAdvance(text: String) {
    draftFlow.update { it.copy(advanceText = text, initializedFromServer = true) }
  }

  fun saveSettings() {
    val payload = draftFlow.value.toPayrollSettings(currentUserId)
    viewModelScope.launch {
      isSavingFlow.value = true
      val res = repository.saveSettings(payload)
      isSavingFlow.value = false
      res.onSuccess {
        _feedbackMessage.value = "Payroll settings saved to cloud!"
      }.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "Failed to save payroll settings."
      }
    }
  }

  fun clearCurrentPeriod() {
    val dateKeys = uiState.value.period.dates.map { it.toIsoKey() }
    viewModelScope.launch {
      isSavingFlow.value = true
      val res = repository.clearPeriod(dateKeys)
      isSavingFlow.value = false
      res.onSuccess {
        _feedbackMessage.value = "Cleared attendance records for this payroll period."
      }.onFailure { err ->
        _feedbackMessage.value = err.localizedMessage ?: "Failed to clear payroll period."
      }
    }
  }

  fun restoreBackup(jsonText: String) {
    viewModelScope.launch {
      isSavingFlow.value = true
      try {
        val (restoredSettings, entries) = BackupCodec.parseBackupJson(jsonText, currentUserId)
        val res = repository.restoreBackup(restoredSettings, entries)
        res.onSuccess {
          draftFlow.value = SettingsFormDraft.from(restoredSettings)
          _feedbackMessage.value = "Backup restored (${entries.size} day entries synced)."
        }.onFailure { err ->
          _feedbackMessage.value = err.localizedMessage ?: "Failed to sync restored backup."
        }
      } catch (e: Exception) {
        _feedbackMessage.value = "Could not restore: invalid backup file."
      } finally {
        isSavingFlow.value = false
      }
    }
  }

  fun buildCsvContent(): String {
    val state = uiState.value
    return BackupCodec.exportCsv(state.period, state.records)
  }

  fun buildBackupJsonContent(): String {
    val state = uiState.value
    return BackupCodec.exportBackupJson(state.settings, state.records)
  }

  fun showFeedback(message: String) {
    _feedbackMessage.value = message
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  private companion object {
    const val TAG = "AttendanceVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
