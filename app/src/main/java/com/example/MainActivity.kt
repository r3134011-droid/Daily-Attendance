package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.AttendanceRepository
import com.example.domain.PayrollDate
import com.example.ui.AttendanceViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.DayEditorBottomSheet
import com.example.ui.components.MahadevHeaderBanner
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.PayslipScreen
import com.example.ui.screens.SettingsToolsScreen
import com.example.ui.theme.MahadevAttendanceTheme
import com.example.util.PayslipPrinter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class DashboardTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  CALENDAR(
    labelRes = R.string.nav_calendar,
    selectedIcon = Icons.Filled.CalendarMonth,
    unselectedIcon = Icons.Outlined.CalendarMonth,
    testTag = "nav_calendar",
  ),
  PAYSLIP(
    labelRes = R.string.nav_payslip,
    selectedIcon = Icons.Filled.AccountBalanceWallet,
    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
    testTag = "nav_payslip",
  ),
  SETTINGS(
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Filled.Tune,
    unselectedIcon = Icons.Outlined.Tune,
    testTag = "nav_settings",
  ),
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MahadevAttendanceTheme {
        AppRootNavigation()
      }
    }
  }
}

@Composable
fun AppRootNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(
      onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ }
    )
  } else {
    val uid = user.uid
    key(uid) {
      MainDashboardScreen(
        currentUserId = uid,
        userEmail = user.email,
        userName = user.displayName,
      )
    }
  }
}

@Composable
fun MainDashboardScreen(
  currentUserId: String,
  userEmail: String?,
  userName: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: AttendanceViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AttendanceViewModel(
          repository = AttendanceRepository(db),
          currentUserId = currentUserId,
        )
      }
    },
  )

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var activeTab by rememberSaveable { mutableStateOf(DashboardTab.CALENDAR) }
  var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(feedback) {
    val msg = feedback
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedback()
    }
  }

  val exportCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(viewModel.buildCsvContent().toByteArray(Charsets.UTF_8))
        }
      }.onSuccess {
        viewModel.showFeedback("CSV exported successfully.")
      }.onFailure { err ->
        viewModel.showFeedback("CSV export failed: ${err.localizedMessage ?: "Unknown error"}")
      }
    }
  }

  val backupJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(viewModel.buildBackupJsonContent().toByteArray(Charsets.UTF_8))
        }
      }.onSuccess {
        viewModel.showFeedback("Backup JSON saved.")
      }.onFailure { err ->
        viewModel.showFeedback("Backup failed: ${err.localizedMessage ?: "Unknown error"}")
      }
    }
  }

  val restoreJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
      }.onSuccess { content ->
        if (!content.isNullOrBlank()) {
          pendingRestoreJson = content
        } else {
          viewModel.showFeedback("Selected backup file is empty.")
        }
      }.onFailure {
        viewModel.showFeedback("Could not read backup file.")
      }
    }
  }

  val triggerPrint = {
    val monthLabel = "${PayrollDate.MONTH_NAMES[state.selectedMonth - 1]} ${state.selectedYear}"
    PayslipPrinter.printPayslip(
      context = context,
      monthLabel = monthLabel,
      period = state.period,
      summary = state.summary,
      records = state.records,
      userLabel = userEmail ?: userName ?: "",
    )
  }

  if (pendingRestoreJson != null) {
    AlertDialog(
      onDismissRequest = { pendingRestoreJson = null },
      title = { Text("Restore Backup?") },
      text = { Text("This will replace your current attendance records and payroll settings with the selected backup.") },
      confirmButton = {
        TextButton(
          onClick = {
            val payload = pendingRestoreJson
            pendingRestoreJson = null
            if (payload != null) viewModel.restoreBackup(payload)
          }
        ) {
          Text("Restore")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingRestoreJson = null }) {
          Text("Cancel")
        }
      },
    )
  }

  val editingKey = state.editingDateKey
  if (editingKey != null) {
    DayEditorBottomSheet(
      dateKey = editingKey,
      existingRecord = state.records[editingKey],
      isSaving = state.isSaving,
      onSave = { status, ot -> viewModel.saveDayEntry(editingKey, status, ot) },
      onDelete = { viewModel.deleteDayEntry(editingKey) },
      onDismiss = { viewModel.closeDayEditor() },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            DashboardTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { activeTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavRail) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            DashboardTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { activeTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
          MahadevHeaderBanner(
            period = state.period,
            onGoToday = {
              viewModel.goToday()
              activeTab = DashboardTab.CALENDAR
            },
          )

          if (state.isLoading || state.isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          }

          if (state.streamError != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = state.streamError ?: "",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
              DashboardTab.CALENDAR -> CalendarScreen(
                state = state,
                onSelectYear = viewModel::selectYear,
                onSelectMonth = viewModel::selectMonth,
                onGoToday = viewModel::goToday,
                onDateClick = viewModel::openDayEditor,
              )

              DashboardTab.PAYSLIP -> PayslipScreen(
                state = state,
                onRecalculate = {
                  viewModel.showFeedback("Salary updated for ${state.period.start.toIsoKey()} to ${state.period.end.toIsoKey()}.")
                },
                onPrintPdf = triggerPrint,
              )

              DashboardTab.SETTINGS -> SettingsToolsScreen(
                state = state,
                userEmail = userEmail,
                userName = userName,
                onSelectYear = viewModel::selectYear,
                onUpdateStartDay = viewModel::updateDraftStartDay,
                onUpdateSalary = viewModel::updateDraftSalary,
                onUpdateOtRate = viewModel::updateDraftOtRate,
                onUpdatePf = viewModel::updateDraftPf,
                onUpdateEsi = viewModel::updateDraftEsi,
                onUpdateAdvance = viewModel::updateDraftAdvance,
                onSaveSettings = viewModel::saveSettings,
                onExportCsv = {
                  val filename = "attendance-${state.period.start.toIsoKey()}-to-${state.period.end.toIsoKey()}.csv"
                  exportCsvLauncher.launch(filename)
                },
                onBackupJson = {
                  backupJsonLauncher.launch("mahadev-attendance-backup.json")
                },
                onRestoreJson = {
                  restoreJsonLauncher.launch(arrayOf("application/json", "*/*"))
                },
                onPrintReport = triggerPrint,
                onClearPeriod = viewModel::clearCurrentPeriod,
                onSignOut = {
                  signOutUser(
                    credentialManager = credentialManager,
                    scope = scope,
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}
