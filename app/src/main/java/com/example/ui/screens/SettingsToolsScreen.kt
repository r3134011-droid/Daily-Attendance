package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AttendanceDashboardState
import com.example.ui.theme.LocalAttendanceColors

@Composable
fun SettingsToolsScreen(
  state: AttendanceDashboardState,
  userEmail: String?,
  userName: String?,
  onSelectYear: (Int) -> Unit,
  onUpdateStartDay: (Int) -> Unit,
  onUpdateSalary: (String) -> Unit,
  onUpdateOtRate: (String) -> Unit,
  onUpdatePf: (String) -> Unit,
  onUpdateEsi: (String) -> Unit,
  onUpdateAdvance: (String) -> Unit,
  onSaveSettings: () -> Unit,
  onExportCsv: () -> Unit,
  onBackupJson: () -> Unit,
  onRestoreJson: () -> Unit,
  onPrintReport: () -> Unit,
  onClearPeriod: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalAttendanceColors.current
  val draft = state.settingsDraft
  var showYearDropdown by remember { mutableStateOf(false) }
  var showStartDayDropdown by remember { mutableStateOf(false) }
  var showClearConfirm by remember { mutableStateOf(false) }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear Payroll Period Data?") },
      text = {
        Text(
          "Delete attendance and overtime records for ${state.period.start.toIsoKey()} to ${state.period.end.toIsoKey()}?"
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showClearConfirm = false
            onClearPeriod()
          }
        ) {
          Text("Delete", color = semantic.absent)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Payroll Settings Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.payroll_settings_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(modifier = Modifier.weight(1f)) {
              OutlinedTextField(
                value = state.selectedYear.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_year_label)) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
              )
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .clickable { showYearDropdown = true }
              )
              DropdownMenu(
                expanded = showYearDropdown,
                onDismissRequest = { showYearDropdown = false },
              ) {
                (2026..2040).forEach { y ->
                  DropdownMenuItem(
                    text = { Text(y.toString()) },
                    onClick = {
                      showYearDropdown = false
                      onSelectYear(y)
                    },
                  )
                }
              }
            }

            Box(modifier = Modifier.weight(1f)) {
              OutlinedTextField(
                value = draft.startDay.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.payroll_start_day_label)) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
              )
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .clickable { showStartDayDropdown = true }
              )
              DropdownMenu(
                expanded = showStartDayDropdown,
                onDismissRequest = { showStartDayDropdown = false },
              ) {
                (1..31).forEach { d ->
                  DropdownMenuItem(
                    text = { Text(d.toString()) },
                    onClick = {
                      showStartDayDropdown = false
                      onUpdateStartDay(d)
                    },
                  )
                }
              }
            }
          }

          OutlinedTextField(
            value = draft.salaryText,
            onValueChange = onUpdateSalary,
            label = { Text(stringResource(R.string.monthly_basic_salary_label)) },
            placeholder = { Text("e.g. 25000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("salary_input"),
          )

          OutlinedTextField(
            value = draft.otRateText,
            onValueChange = onUpdateOtRate,
            label = { Text(stringResource(R.string.ot_rate_per_hour_label)) },
            placeholder = { Text("e.g. 100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("ot_rate_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = draft.pfText,
              onValueChange = onUpdatePf,
              label = { Text(stringResource(R.string.pf_deduction_label)) },
              placeholder = { Text("0") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("pf_input"),
            )
            OutlinedTextField(
              value = draft.esiText,
              onValueChange = onUpdateEsi,
              label = { Text(stringResource(R.string.esi_deduction_label)) },
              placeholder = { Text("0") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("esi_input"),
            )
          }

          OutlinedTextField(
            value = draft.advanceText,
            onValueChange = onUpdateAdvance,
            label = { Text(stringResource(R.string.advance_deduction_label)) },
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("advance_input"),
          )

          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Active Payroll Period: ${state.period.start.toIsoKey()} to ${state.period.end.toIsoKey()}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(12.dp),
            )
          }

          Button(
            onClick = onSaveSettings,
            enabled = !state.isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = semantic.present,
              contentColor = Color.White,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("save_settings_button"),
          ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.save_payroll_settings))
          }
        }
      }

      // Data Tools Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = stringResource(R.string.data_tools_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onExportCsv,
              colors = ButtonDefaults.buttonColors(
                containerColor = semantic.present,
                contentColor = Color.White,
              ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("export_csv_button"),
            ) {
              Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.export_csv))
            }

            Button(
              onClick = onBackupJson,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("backup_json_button"),
            ) {
              Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.backup_data))
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onRestoreJson,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("restore_json_button"),
            ) {
              Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.restore_backup))
            }

            OutlinedButton(
              onClick = onPrintReport,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("print_report_button"),
            ) {
              Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.print_report))
            }
          }

          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = stringResource(R.string.cloud_storage_note),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(12.dp),
            )
          }

          Button(
            onClick = { showClearConfirm = true },
            enabled = !state.isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = semantic.absent,
              contentColor = Color.White,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("clear_period_button"),
          ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.clear_period_data))
          }
        }
      }

      // Account Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Filled.AccountCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Column {
              Text(
                text = userName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (!userEmail.isNullOrBlank()) {
                Text(
                  text = userEmail,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.sign_out))
          }
        }
      }

      Text(
        text = stringResource(R.string.app_footer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
      )
    }
  }
}
