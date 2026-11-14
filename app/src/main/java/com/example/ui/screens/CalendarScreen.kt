package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.domain.PayrollCalculator
import com.example.domain.PayrollDate
import com.example.ui.AttendanceDashboardState
import com.example.ui.components.AttendanceLegendRow
import com.example.ui.theme.LocalAttendanceColors

@Composable
fun CalendarScreen(
  state: AttendanceDashboardState,
  onSelectYear: (Int) -> Unit,
  onSelectMonth: (Int) -> Unit,
  onGoToday: () -> Unit,
  onDateClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalAttendanceColors.current
  val monthsListState = rememberLazyListState()
  var yearMenuExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(state.selectedMonth) {
    monthsListState.animateScrollToItem((state.selectedMonth - 1).coerceIn(0, 11))
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
      // Today Card
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
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.CalendarToday,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Column {
              Text(
                text = stringResource(R.string.today_section_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = state.today.formatDisplayLong(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          OutlinedButton(
            onClick = onGoToday,
            modifier = Modifier.testTag("go_today_button"),
          ) {
            Text("Current")
          }
        }
      }

      // Quick Summary Strip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        QuickStatTile(
          label = "Present",
          value = "${state.summary.presentCount}",
          color = semantic.present,
          modifier = Modifier.weight(1f),
        )
        QuickStatTile(
          label = "Absent",
          value = "${state.summary.absentCount}",
          color = semantic.absent,
          modifier = Modifier.weight(1f),
        )
        QuickStatTile(
          label = "OT Hrs",
          value = PayrollCalculator.formatHours(state.summary.totalOtHours),
          color = semantic.holiday,
          modifier = Modifier.weight(1f),
        )
        QuickStatTile(
          label = "Est. Net",
          value = PayrollCalculator.formatInr(state.summary.netSalary),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1.35f),
        )
      }

      // Year & Month Selector Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.select_payroll_month),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { onSelectYear((state.selectedYear - 1).coerceAtLeast(2026)) },
                enabled = state.selectedYear > 2026,
              ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Year")
              }

              Box {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { yearMenuExpanded = true }
                    .testTag("year_selector_button"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  ) {
                    Text(
                      text = state.selectedYear.toString(),
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Icon(
                      imageVector = Icons.Filled.ArrowDropDown,
                      contentDescription = "Choose Year",
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                }

                DropdownMenu(
                  expanded = yearMenuExpanded,
                  onDismissRequest = { yearMenuExpanded = false },
                ) {
                  (2026..2040).forEach { yr ->
                    DropdownMenuItem(
                      text = { Text(yr.toString()) },
                      onClick = {
                        yearMenuExpanded = false
                        onSelectYear(yr)
                      },
                    )
                  }
                }
              }

              IconButton(
                onClick = { onSelectYear((state.selectedYear + 1).coerceAtMost(2100)) },
                enabled = state.selectedYear < 2100,
              ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Year")
              }
            }
          }

          LazyRow(
            state = monthsListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
          ) {
            itemsIndexed(PayrollDate.MONTH_NAMES) { idx, monthName ->
              val monthNum = idx + 1
              val active = state.selectedMonth == monthNum
              Surface(
                shape = RoundedCornerShape(50),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (active) 3.dp else 0.dp,
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .clickable { onSelectMonth(monthNum) }
                  .testTag("month_pill_$monthNum"),
              ) {
                Text(
                  text = monthName,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
              }
            }
          }
        }
      }

      // Attendance Calendar Grid Card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Column {
            Text(
              text = stringResource(R.string.attendance_calendar_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.attendance_calendar_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Weekday Headers
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            PayrollDate.DAY_HEADERS.forEach { dayName ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f),
              ) {
                Text(
                  text = dayName,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 8.dp),
                )
              }
            }
          }

          // Calendar Cells grouped into 7-column weeks
          val leadingBlanks = state.period.start.dayOfWeekSundayZero()
          val cells: List<PayrollDate?> =
            List(leadingBlanks) { null } + state.period.dates
          val rows = cells.chunked(7)

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { week ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                for (col in 0 until 7) {
                  val date = week.getOrNull(col)
                  if (date == null) {
                    Spacer(modifier = Modifier.weight(1f).height(64.dp))
                  } else {
                    val key = date.toIsoKey()
                    val record = state.records[key]
                    val isToday = date == state.today
                    CalendarDayCell(
                      date = date,
                      record = record,
                      isToday = isToday,
                      onClick = { onDateClick(key) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))
          AttendanceLegendRow()
        }
      }
    }
  }
}

@Composable
private fun QuickStatTile(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CalendarDayCell(
  date: PayrollDate,
  record: AttendanceRecord?,
  isToday: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalAttendanceColors.current
  val status = record?.statusEnum ?: AttendanceStatus.NONE
  val otHours = record?.otHours ?: 0.0

  val bgColor = when (status) {
    AttendanceStatus.PRESENT -> semantic.present
    AttendanceStatus.ABSENT -> semantic.absent
    AttendanceStatus.HOLIDAY -> semantic.holiday
    AttendanceStatus.WEEKLY_OFF -> semantic.weeklyOff
    AttendanceStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
  }

  val fgColor = if (status == AttendanceStatus.NONE) {
    MaterialTheme.colorScheme.onSurface
  } else {
    Color.White
  }

  val borderMod = if (isToday) {
    Modifier.border(2.dp, semantic.todayRing, RoundedCornerShape(12.dp))
  } else {
    Modifier
  }

  Box(
    modifier = modifier
      .height(64.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .then(borderMod)
      .clickable(onClick = onClick)
      .padding(horizontal = 2.dp, vertical = 4.dp)
      .testTag("calendar_cell_${date.toIsoKey()}"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = date.day.toString(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = fgColor,
      )
      Text(
        text = if (status.code.isEmpty()) "—" else status.code,
        style = MaterialTheme.typography.labelSmall,
        color = fgColor.copy(alpha = 0.92f),
      )
      if (otHours > 0.0) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (status == AttendanceStatus.NONE) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
          } else {
            Color.White.copy(alpha = 0.26f)
          },
        ) {
          Text(
            text = "${PayrollCalculator.formatHours(otHours)}h",
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }
    }
  }
}
