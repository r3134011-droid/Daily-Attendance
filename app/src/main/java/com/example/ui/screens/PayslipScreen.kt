package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PayrollCalculator
import com.example.domain.PayrollDate
import com.example.ui.AttendanceDashboardState
import com.example.ui.theme.LocalAttendanceColors

@Composable
fun PayslipScreen(
  state: AttendanceDashboardState,
  onRecalculate: () -> Unit,
  onPrintPdf: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalAttendanceColors.current
  val summary = state.summary
  val monthLabel = "${PayrollDate.MONTH_NAMES[state.selectedMonth - 1]} ${state.selectedYear}"

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
      // Attendance Summary Card (2x2 Grid + OT Banner)
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
            text = "${stringResource(R.string.attendance_summary_title)} • $monthLabel",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            SummaryCountBox(
              title = stringResource(R.string.legend_present),
              count = summary.presentCount,
              accent = semantic.present,
              container = semantic.presentContainer,
              modifier = Modifier.weight(1f),
            )
            SummaryCountBox(
              title = stringResource(R.string.legend_absent),
              count = summary.absentCount,
              accent = semantic.absent,
              container = semantic.absentContainer,
              modifier = Modifier.weight(1f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            SummaryCountBox(
              title = stringResource(R.string.legend_holiday),
              count = summary.holidayCount,
              accent = semantic.holiday,
              container = semantic.holidayContainer,
              modifier = Modifier.weight(1f),
            )
            SummaryCountBox(
              title = stringResource(R.string.legend_weekly_off),
              count = summary.weeklyOffCount,
              accent = semantic.weeklyOff,
              container = semantic.weeklyOffContainer,
              modifier = Modifier.weight(1f),
            )
          }

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Schedule,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "Total OT Hours",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              Text(
                text = "${PayrollCalculator.formatHours(summary.totalOtHours)} Hours",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // Salary Calculator Card
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
            text = stringResource(R.string.salary_calculator_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Surface(
            shape = MaterialTheme.shapes.medium,
            color = semantic.presentContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              BreakdownRow("Payroll Days", "${summary.totalDays}")
              BreakdownRow("Paid Days (P + H + W)", "${summary.paidDays}")
              BreakdownRow("Per Day Salary", PayrollCalculator.formatInr(summary.perDaySalary))

              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

              BreakdownRow("Basic Earned Salary", PayrollCalculator.formatInr(summary.basicEarnedSalary))
              BreakdownRow(
                "OT Amount (${PayrollCalculator.formatHours(summary.totalOtHours)} hrs)",
                PayrollCalculator.formatInr(summary.otAmount),
              )
              BreakdownRow("Gross Salary", PayrollCalculator.formatInr(summary.grossSalary), bold = true)

              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

              BreakdownRow("PF Deduction", "- ${PayrollCalculator.formatInr(summary.pfDeduction)}")
              BreakdownRow("ESI Deduction", "- ${PayrollCalculator.formatInr(summary.esiDeduction)}")
              BreakdownRow("Other / Advance", "- ${PayrollCalculator.formatInr(summary.advanceDeduction)}")
              BreakdownRow("Total Deductions", "- ${PayrollCalculator.formatInr(summary.totalDeductions)}", bold = true)

              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

              Text(
                text = "Estimated Net Salary",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = PayrollCalculator.formatInr(summary.netSalary),
                style = MaterialTheme.typography.displaySmall,
                color = semantic.present,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("net_salary_text"),
              )
              Text(
                text = stringResource(R.string.payroll_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Button(
            onClick = onRecalculate,
            colors = ButtonDefaults.buttonColors(
              containerColor = semantic.present,
              contentColor = Color.White,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("recalculate_salary_button"),
          ) {
            Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.calculate_salary))
          }

          OutlinedButton(
            onClick = onPrintPdf,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("print_pdf_button"),
          ) {
            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.print_save_pdf))
          }
        }
      }
    }
  }
}

@Composable
private fun SummaryCountBox(
  title: String,
  count: Int,
  accent: Color,
  container: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = container,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.headlineMedium,
        color = accent,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun BreakdownRow(
  label: String,
  value: String,
  bold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = value,
      style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
