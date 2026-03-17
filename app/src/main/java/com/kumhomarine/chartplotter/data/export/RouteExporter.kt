package com.kumhomarine.chartplotter.data.export

import android.util.Log
import com.kumhomarine.chartplotter.data.models.Route
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 경로 GPX 내보내기
 */
class RouteExporter {

    fun exportRoutes(routes: List<Route>, outputDir: File): File? {
        val validRoutes = routes.filter { it.points.isNotEmpty() }
        if (validRoutes.isEmpty()) {
            Log.w("[RouteExporter]", "내보낼 경로가 없습니다")
            return null
        }
        return try {
            outputDir.mkdirs()
            val fileName = "routes_${System.currentTimeMillis()}.gpx"
            val file = File(outputDir, fileName)
            FileWriter(file).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                writer.write("<gpx version=\"1.1\" creator=\"ChartPlotter\">\n")
                writer.write("  <metadata>\n")
                writer.write("    <time>${formatGpxTime(System.currentTimeMillis())}</time>\n")
                writer.write("  </metadata>\n")
                validRoutes.forEach { route ->
                    val nameEscaped = escapeXml(route.name)
                    writer.write("  <rte>\n")
                    writer.write("    <name>$nameEscaped</name>\n")
                    route.points.sortedBy { it.order }.forEach { pt ->
                        writer.write("    <rtept lat=\"${pt.latitude}\" lon=\"${pt.longitude}\">\n")
                        if (pt.name.isNotBlank()) {
                            writer.write("      <name>${escapeXml(pt.name)}</name>\n")
                        }
                        writer.write("    </rtept>\n")
                    }
                    writer.write("  </rte>\n")
                }
                writer.write("</gpx>\n")
            }
            Log.d("[RouteExporter]", "경로 내보내기 완료: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("[RouteExporter]", "경로 내보내기 실패: ${e.message}")
            null
        }
    }

    private fun formatGpxTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
