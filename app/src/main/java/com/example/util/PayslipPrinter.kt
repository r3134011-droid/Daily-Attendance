package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.model.AttendanceRecord
import com.example.domain.PayrollCalculator
import com.example.domain.PayrollPeriod
import com.example.domain.PayrollSummary
import java.io.FileOutputStream

object PayslipPrinter {

  fun printPayslip(
    context: Context,
    monthLabel: String,
    period: PayrollPeriod,
    summary: PayrollSummary,
    records: Map<String, AttendanceRecord>,
    userLabel: String,
  ) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val jobName = "Mahadev_Attendance_${period.start.toIsoKey()}_to_${period.end.toIsoKey()}"
    val adapter = PayslipPrintAdapter(
      context = context,
      jobName = jobName,
      monthLabel = monthLabel,
      period = period,
      summary = summary,
      records = records,
      userLabel = userLabel,
    )
    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
  }

  private class PayslipPrintAdapter(
    private val context: Context,
    private val jobName: String,
    private val monthLabel: String,
    private val period: PayrollPeriod,
    private val summary: PayrollSummary,
    private val records: Map<String, AttendanceRecord>,
    private val userLabel: String,
  ) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
      oldAttributes: PrintAttributes?,
      newAttributes: PrintAttributes,
      cancellationSignal: CancellationSignal?,
      callback: LayoutResultCallback,
      extras: Bundle?,
    ) {
      pdfDocument = PrintedPdfDocument(context, newAttributes)
      if (cancellationSignal?.isCanceled == true) {
        callback.onLayoutCancelled()
        return
      }
      val info = PrintDocumentInfo.Builder("$jobName.pdf")
        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
        .setPageCount(2)
        .build()
      callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
      pages: Array<out PageRange>,
      destination: ParcelFileDescriptor,
      cancellationSignal: CancellationSignal?,
      callback: WriteResultCallback,
    ) {
      val doc = pdfDocument ?: run {
        callback.onWriteFailed("PDF document unavailable")
        return
      }

      try {
        // Page 1: Payslip Summary
        val page1 = doc.startPage(0)
        drawSummaryPage(page1.canvas)
        doc.finishPage(page1)

        if (cancellationSignal?.isCanceled == true) {
          callback.onWriteCancelled()
          doc.close()
          pdfDocument = null
          return
        }

        // Page 2: Daily Attendance & Overtime Log
        val page2 = doc.startPage(1)
        drawDailyLogPage(page2.canvas)
        doc.finishPage(page2)

        FileOutputStream(destination.fileDescriptor).use { out ->
          doc.writeTo(out)
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
      } catch (e: Exception) {
        callback.onWriteFailed(e.message)
      } finally {
        doc.close()
        pdfDocument = null
      }
    }

    private fun drawSummaryPage(canvas: android.graphics.Canvas) {
      val margin = 42f
      var y = 52f

      val titlePaint = Paint().apply {
        color = Color.rgb(22, 43, 102)
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val subPaint = Paint().apply {
        color = Color.rgb(90, 105, 135)
        textSize = 11f
      }
      val sectionPaint = Paint().apply {
        color = Color.rgb(38, 63, 131)
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val labelPaint = Paint().apply {
        color = Color.rgb(45, 55, 72)
        textSize = 12f
      }
      val valuePaint = Paint().apply {
        color = Color.rgb(23, 35, 61)
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }
      val netPaint = Paint().apply {
        color = Color.rgb(8, 126, 74)
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }
      val linePaint = Paint().apply {
        color = Color.rgb(210, 220, 235)
        strokeWidth = 1f
      }

      val rightX = canvas.width - margin

      canvas.drawText("MAHADEV ATTENDANCE & SALARY MANAGER", margin, y, titlePaint)
      y += 18f
      canvas.drawText("Payroll Report: $monthLabel (${period.start.toIsoKey()} to ${period.end.toIsoKey()})", margin, y, subPaint)
      y += 15f
      if (userLabel.isNotBlank()) {
        canvas.drawText("Account: $userLabel", margin, y, subPaint)
        y += 15f
      }
      canvas.drawLine(margin, y, rightX, y, linePaint)
      y += 24f

      canvas.drawText("Attendance Summary", margin, y, sectionPaint)
      y += 20f

      val summaryRows = listOf(
        "Payroll Days" to "${summary.totalDays}",
        "Present (P)" to "${summary.presentCount}",
        "Absent (A)" to "${summary.absentCount}",
        "Holiday (H)" to "${summary.holidayCount}",
        "Weekly Off (W)" to "${summary.weeklyOffCount}",
        "Paid Days" to "${summary.paidDays}",
        "Total Overtime Hours" to "${PayrollCalculator.formatHours(summary.totalOtHours)} hrs",
      )
      summaryRows.forEach { (k, v) ->
        canvas.drawText(k, margin, y, labelPaint)
        canvas.drawText(v, rightX, y, valuePaint)
        y += 20f
      }

      y += 8f
      canvas.drawLine(margin, y, rightX, y, linePaint)
      y += 24f

      canvas.drawText("Earnings & Deductions", margin, y, sectionPaint)
      y += 20f

      val salaryRows = listOf(
        "Per Day Salary" to PayrollCalculator.formatInr(summary.perDaySalary),
        "Basic Earned Salary" to PayrollCalculator.formatInr(summary.basicEarnedSalary),
        "Overtime Amount" to PayrollCalculator.formatInr(summary.otAmount),
        "Gross Salary" to PayrollCalculator.formatInr(summary.grossSalary),
        "PF Deduction" to "- ${PayrollCalculator.formatInr(summary.pfDeduction)}",
        "ESI Deduction" to "- ${PayrollCalculator.formatInr(summary.esiDeduction)}",
        "Other / Advance" to "- ${PayrollCalculator.formatInr(summary.advanceDeduction)}",
        "Total Deductions" to "- ${PayrollCalculator.formatInr(summary.totalDeductions)}",
      )
      salaryRows.forEach { (k, v) ->
        canvas.drawText(k, margin, y, labelPaint)
        canvas.drawText(v, rightX, y, valuePaint)
        y += 20f
      }

      y += 10f
      canvas.drawLine(margin, y, rightX, y, linePaint)
      y += 28f

      canvas.drawText("Estimated Net Salary", margin, y, sectionPaint)
      canvas.drawText(PayrollCalculator.formatInr(summary.netSalary), rightX, y, netPaint)
      y += 24f

      canvas.drawText(
        "Note: Confirm payroll rules and actual statutory deductions with your employer.",
        margin,
        y,
        subPaint,
      )
    }

    private fun drawDailyLogPage(canvas: android.graphics.Canvas) {
      val margin = 42f
      var y = 52f
      val rightX = canvas.width - margin

      val titlePaint = Paint().apply {
        color = Color.rgb(22, 43, 102)
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val headerPaint = Paint().apply {
        color = Color.rgb(38, 63, 131)
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val rowPaint = Paint().apply {
        color = Color.rgb(35, 45, 65)
        textSize = 10.5f
      }
      val linePaint = Paint().apply {
        color = Color.rgb(210, 220, 235)
        strokeWidth = 1f
      }

      canvas.drawText("Daily Attendance & Overtime Log (${period.start.toIsoKey()} to ${period.end.toIsoKey()})", margin, y, titlePaint)
      y += 22f

      val col1 = margin
      val col2 = margin + 150f
      val col3 = margin + 320f

      canvas.drawText("Date", col1, y, headerPaint)
      canvas.drawText("Status", col2, y, headerPaint)
      canvas.drawText("Overtime (Hours)", col3, y, headerPaint)
      y += 8f
      canvas.drawLine(margin, y, rightX, y, linePaint)
      y += 16f

      period.dates.forEach { date ->
        val key = date.toIsoKey()
        val rec = records[key]
        val statusLabel = rec?.statusEnum?.label ?: "Not Set"
        val otStr = if ((rec?.otHours ?: 0.0) > 0.0) "${PayrollCalculator.formatHours(rec!!.otHours)} h" else "—"
        canvas.drawText(key, col1, y, rowPaint)
        canvas.drawText(statusLabel, col2, y, rowPaint)
        canvas.drawText(otStr, col3, y, rowPaint)
        y += 18f
      }
    }
  }
}
