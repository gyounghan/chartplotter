package com.kumhomarine.chartplotter.data.export

import android.util.Log
import com.kumhomarine.chartplotter.data.models.SavedPoint
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 목적지(저장 포인트) GPX waypoint 내보내기
 */
class PointExporter {

    /**
     * 저장 포인트 목록을 GPX waypoint 형식으로 내보내기
     * @param points 저장 포인트 목록
     * @param outputDir 출력 디렉토리
     * @return 생성된 파일, 실패 시 null
     */
    fun exportPoints(points: List<SavedPoint>, outputDir: File): File? {
        if (points.isEmpty()) {
            Log.w("[PointExporter]", "내보낼 목적지가 없습니다")
            return null
        }
        return try {
            outputDir.mkdirs()
            val fileName = "destinations_${System.currentTimeMillis()}.gpx"
            val file = File(outputDir, fileName)
            FileWriter(file).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                writer.write("<gpx version=\"1.1\" creator=\"ChartPlotter\">\n")
                writer.write("  <metadata>\n")
                writer.write("    <time>${formatGpxTime(System.currentTimeMillis())}</time>\n")
                writer.write("  </metadata>\n")
                points.forEach { point ->
                    val nameEscaped = escapeXml(point.name)
                    writer.write("  <wpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                    writer.write("    <name>$nameEscaped</name>\n")
                    writer.write("    <time>${formatGpxTime(point.timestamp)}</time>\n")
                    writer.write("  </wpt>\n")
                }
                writer.write("</gpx>\n")
            }
            Log.d("[PointExporter]", "목적지 내보내기 완료: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("[PointExporter]", "목적지 내보내기 실패: ${e.message}")
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
