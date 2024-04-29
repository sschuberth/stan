package dev.schuberth.stan.plugins.exporters.excel

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.Statement

import org.apache.poi.ss.util.DateFormatConverter
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.io.OutputStream
import java.time.LocalDate
import java.util.Locale

import kotlin.reflect.full.memberProperties

const val MAX_CHAR_WIDTH_SERIF = 1.14388
const val MAX_VISIBLE_CHARS = 100
const val EXCEL_DEFAULT_COLUMN_WIDTH = (MAX_VISIBLE_CHARS * MAX_CHAR_WIDTH_SERIF * 256).toInt()

/**
 * See https://en.wikipedia.org/wiki/Microsoft_Excel.
 */
class ExcelExporter : Exporter {
    override val name = "Excel"
    override val extension = "xlsx"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        val bookingProps = BookingItem::class.memberProperties.filterNot { it.name == "info" }

        val locale = options["locale"]?.let { Locale.Builder().setLanguage(it).build() } ?: Locale.getDefault()
        val datePattern = DateFormatConverter.convert(locale, "yyyy-mm-dd")

        XSSFWorkbook().use { workbook ->
            val currencyStyle = workbook.createCellStyle().apply {
                // See "BuiltinFormats".
                setDataFormat(8)
            }

            val dateStyle = workbook.createCellStyle().apply {
                val format = workbook.createDataFormat()
                dataFormat = format.getFormat(datePattern)
            }

            val sheet = workbook.createSheet(statement.filename)

            var rowIndex = 0
            val header = sheet.createRow(rowIndex++)

            var headerIndex = 0
            bookingProps.forEach { prop ->
                header.createCell(headerIndex++).setCellValue(prop.name)
            }

            val noAutoSizeColumns = mutableSetOf<Int>()

            statement.bookings.forEach { booking ->
                val row = sheet.createRow(rowIndex++)

                var columnIndex = 0
                bookingProps.forEach { prop ->
                    val cell = row.createCell(columnIndex)
                    val value = prop.get(booking)

                    cell.setAnyCellValue(value)

                    when {
                        prop.name == "amount" -> cell.cellStyle = currencyStyle
                        prop.name == "joinedInfo" -> noAutoSizeColumns += columnIndex
                        value is LocalDate -> cell.cellStyle = dateStyle
                    }

                    ++columnIndex
                }
            }

            repeat(header.lastCellNum.toInt()) {
                if (it in noAutoSizeColumns) {
                    sheet.setColumnWidth(it, EXCEL_DEFAULT_COLUMN_WIDTH)
                } else {
                    sheet.autoSizeColumn(it)
                }
            }

            output.use { workbook.write(it) }
        }
    }
}

private fun XSSFCell.setAnyCellValue(value: Any?) =
    when (value) {
        null -> {}
        is Float -> setCellValue(value.toDouble())
        is LocalDate -> setCellValue(value)
        else -> setCellValue(value.toString())
    }
