package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.domain.PayrollDate
import com.example.domain.PayrollPeriod
import com.example.ui.theme.LocalAttendanceColors

@Composable
fun MahadevHeaderBanner(
  period: PayrollPeriod,
  onGoToday: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalAttendanceColors.current
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.linearGradient(
          colors = listOf(semantic.headerStart, semantic.headerEnd)
        )
      )
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 16.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.14f),
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = semantic.goldAccent,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Column {
            Text(
              text = stringResource(R.string.app_header_title),
              style = MaterialTheme.typography.titleMedium,
              color = semantic.goldAccent,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stringResource(R.string.app_credit_badge),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.8f),
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = Color.White.copy(alpha = 0.14f),
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onGoToday)
            .testTag("header_today_button"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Today,
              contentDescription = stringResource(R.string.go_to_current_month),
              tint = semantic.goldAccent,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Today",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Payroll Period",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
          )
          Text(
            text = "${period.start.toIsoKey()}  →  ${period.end.toIsoKey()}",
            style = MaterialTheme.typography.labelMedium,
            color = semantic.goldAccent,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceLegendRow(modifier: Modifier = Modifier) {
  val semantic = LocalAttendanceColors.current
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    LegendDot(color = semantic.present, label = stringResource(R.string.legend_present))
    LegendDot(color = semantic.absent, label = stringResource(R.string.legend_absent))
    LegendDot(color = semantic.holiday, label = stringResource(R.string.legend_holiday))
    LegendDot(color = semantic.weeklyOff, label = stringResource(R.string.legend_weekly_off))
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayEditorBottomSheet(
  dateKey: String,
  existingRecord: AttendanceRecord?,
  isSaving: Boolean,
  onSave: (AttendanceStatus, Double) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedStatus by remember(dateKey) {
    mutableStateOf(existingRecord?.statusEnum ?: AttendanceStatus.NONE)
  }
  var otHoursText by remember(dateKey) {
    val existingOt = existingRecord?.otHours ?: 0.0
    mutableStateOf(
      if (existingOt > 0.0) {
        if (existingOt % 1.0 == 0.0) existingOt.toInt().toString() else existingOt.toString()
      } else {
        ""
      }
    )
  }
  var validationError by remember(dateKey) { mutableStateOf<String?>(null) }

  val parsedDate = remember(dateKey) { PayrollDate.parse(dateKey) }
  val semantic = LocalAttendanceColors.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.daily_entry_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = parsedDate?.formatDisplayLong() ?: dateKey,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = "Key: $dateKey",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = stringResource(R.string.attendance_status_label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AttendanceStatus.entries.forEach { status ->
          val isSelected = selectedStatus == status
          val accent = when (status) {
            AttendanceStatus.PRESENT -> semantic.present
            AttendanceStatus.ABSENT -> semantic.absent
            AttendanceStatus.HOLIDAY -> semantic.holiday
            AttendanceStatus.WEEKLY_OFF -> semantic.weeklyOff
            AttendanceStatus.NONE -> MaterialTheme.colorScheme.outline
          }
          FilterChip(
            selected = isSelected,
            onClick = { selectedStatus = status },
            label = {
              Text(
                text = if (status.code.isEmpty()) status.label else "${status.label} (${status.code})",
                style = MaterialTheme.typography.labelLarge,
              )
            },
            leadingIcon = if (isSelected) {
              {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = accent.copy(alpha = 0.16f),
              selectedLabelColor = MaterialTheme.colorScheme.onSurface,
              selectedLeadingIconColor = accent,
            ),
            modifier = Modifier.testTag("status_chip_${status.name.lowercase()}"),
          )
        }
      }

      OutlinedTextField(
        value = otHoursText,
        onValueChange = { input ->
          validationError = null
          otHoursText = input
        },
        label = { Text(stringResource(R.string.overtime_hours_label)) },
        placeholder = { Text(stringResource(R.string.overtime_hours_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = validationError != null,
        supportingText = validationError?.let { msg -> { Text(msg) } },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("day_ot_input"),
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        listOf(0.5, 1.0, 2.0).forEach { delta ->
          AssistChip(
            onClick = {
              validationError = null
              val current = otHoursText.toDoubleOrNull() ?: 0.0
              val updated = (current + delta).coerceAtMost(24.0)
              otHoursText = if (updated % 1.0 == 0.0) updated.toInt().toString() else updated.toString()
            },
            label = { Text("+${if (delta % 1.0 == 0.0) delta.toInt() else delta}h") },
          )
        }
        AssistChip(
          onClick = {
            validationError = null
            otHoursText = ""
          },
          label = { Text("Clear OT") },
        )
      }

      Button(
        onClick = {
          val ot = if (otHoursText.isBlank()) 0.0 else otHoursText.toDoubleOrNull()
          if (ot == null || !ot.isFinite() || ot < 0.0 || ot > 24.0) {
            validationError = "Enter a valid number between 0 and 24."
            return@Button
          }
          onSave(selectedStatus, ot)
        },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(
          containerColor = semantic.present,
          contentColor = Color.White,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_day_button"),
      ) {
        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.save_attendance_ot))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDelete,
          enabled = !isSaving,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.absent),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("delete_day_button"),
        ) {
          Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete")
        }

        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("close_day_sheet_button"),
        ) {
          Text(stringResource(R.string.close_label))
        }
      }
    }
  }
}
