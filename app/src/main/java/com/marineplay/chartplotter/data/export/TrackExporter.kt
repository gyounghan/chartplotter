package com.marineplay.chartplotter.data.export

import android.content.Context
import android.util.Log
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.repositories.TrackRepository
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 항적 내보내기 클래스
 */
class TrackExporter(
    private val context: Context,
    private val trackRepository: TrackRepository
) {
    
    /**
     * 날짜별 항적 포인트를 GPX 파일로 내보내기
     */
    fun exportTracksByDate(date: String, outputDir: File): File? {
        return try {
            val pointsByTrack = runBlocking { trackRepository.getPointsByDate(date) }
            if (pointsByTrack.isEmpty()) {
                Log.w("[TrackExporter]", "날짜별 항적 포인트가 없습니다: $date")
                return null
            }
            
            val fileName = "tracks_$date.gpx"
            val file = File(outputDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                writer.write("<gpx version=\"1.1\" creator=\"ChartPlotter\">\n")
                writer.write("  <metadata>\n")
                writer.write("    <time>${formatGpxTime(System.currentTimeMillis())}</time>\n")
                writer.write("  </metadata>\n")
                
                // 항적별로 그룹화하여 트랙으로 저장
                val tracks = runBlocking { trackRepository.getAllTracks() }
                pointsByTrack.groupBy { it.first }.forEach { (trackId, trackPoints) ->
                    val track = tracks.find { it.id == trackId }
                    val trackName = track?.name ?: "Track_$trackId"
                    
                    writer.write("  <trk>\n")
                    writer.write("    <name>$trackName</name>\n")
                    writer.write("    <trkseg>\n")
                    
                    trackPoints.forEach { (_, point) ->
                        writer.write("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                        writer.write("        <time>${formatGpxTime(point.timestamp)}</time>\n")
                        writer.write("      </trkpt>\n")
                    }
                    
                    writer.write("    </trkseg>\n")
                    writer.write("  </trk>\n")
                }
                
                writer.write("</gpx>\n")
            }
            
            Log.d("[TrackExporter]", "날짜별 항적 내보내기 완료: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("[TrackExporter]", "날짜별 항적 내보내기 실패: ${e.message}")
            null
        }
    }
    
    /**
     * 특정 항적의 모든 포인트를 GPX 파일로 내보내기
     */
    fun exportTrackById(trackId: String, outputDir: File): File? {
        return try {
            val track = runBlocking { trackRepository.getAllTracks().find { it.id == trackId } }
            if (track == null || track.points.isEmpty()) {
                Log.w("[TrackExporter]", "항적이 없거나 포인트가 없습니다: $trackId")
                return null
            }
            
            val fileName = "track_${track.name.replace(" ", "_")}_${System.currentTimeMillis()}.gpx"
            val file = File(outputDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                writer.write("<gpx version=\"1.1\" creator=\"ChartPlotter\">\n")
                writer.write("  <metadata>\n")
                writer.write("    <name>${track.name}</name>\n")
                writer.write("    <time>${formatGpxTime(System.currentTimeMillis())}</time>\n")
                writer.write("  </metadata>\n")
                
                writer.write("  <trk>\n")
                writer.write("    <name>${track.name}</name>\n")
                writer.write("    <trkseg>\n")
                
                track.points.forEach { point ->
                    writer.write("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                    writer.write("        <time>${formatGpxTime(point.timestamp)}</time>\n")
                    writer.write("      </trkpt>\n")
                }
                
                writer.write("    </trkseg>\n")
                writer.write("  </trk>\n")
                writer.write("</gpx>\n")
            }
            
            Log.d("[TrackExporter]", "항적별 내보내기 완료: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("[TrackExporter]", "항적별 내보내기 실패: ${e.message}")
            null
        }
    }
    
    /**
     * 모든 항적을 GPX 파일로 내보내기
     */
    fun exportAllTracks(outputDir: File): File? {
        return try {
            val tracks = runBlocking { trackRepository.getAllTracks() }
            if (tracks.isEmpty()) {
                Log.w("[TrackExporter]", "내보낼 항적이 없습니다")
                return null
            }
            
            val fileName = "all_tracks_${System.currentTimeMillis()}.gpx"
            val file = File(outputDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                writer.write("<gpx version=\"1.1\" creator=\"ChartPlotter\">\n")
                writer.write("  <metadata>\n")
                writer.write("    <time>${formatGpxTime(System.currentTimeMillis())}</time>\n")
                writer.write("  </metadata>\n")
                
                tracks.forEach { track ->
                    if (track.points.isNotEmpty()) {
                        writer.write("  <trk>\n")
                        writer.write("    <name>${track.name}</name>\n")
                        writer.write("    <trkseg>\n")
                        
                        track.points.forEach { point ->
                            writer.write("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                            writer.write("        <time>${formatGpxTime(point.timestamp)}</time>\n")
                            writer.write("      </trkpt>\n")
                        }
                        
                        writer.write("    </trkseg>\n")
                        writer.write("  </trk>\n")
                    }
                }
                
                writer.write("</gpx>\n")
            }
            
            Log.d("[TrackExporter]", "모든 항적 내보내기 완료: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("[TrackExporter]", "모든 항적 내보내기 실패: ${e.message}")
            null
        }
    }
    
    /**
     * GPX 시간 형식으로 변환 (ISO 8601)
     */
    private fun formatGpxTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }
}

