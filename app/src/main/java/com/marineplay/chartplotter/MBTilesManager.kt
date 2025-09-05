package com.marineplay.chartplotter

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MBTilesManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MBTilesManager"
        private const val ASSETS_FOLDER = "mbtiles"
    }
    
    /**
     * MBTiles 파일을 assets에서 내부 저장소로 복사
     */
    fun copyMBTilesFromAssets() {
        try {
            val assetManager = context.assets
            val assetFiles = assetManager.list(ASSETS_FOLDER) ?: return
            
            for (fileName in assetFiles) {
                if (fileName.endsWith(".mbtiles")) {
                    copyAssetFile(ASSETS_FOLDER, fileName)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying MBTiles from assets", e)
        }
    }
    
    /**
     * 개별 MBTiles 파일 복사
     */
    private fun copyAssetFile(assetFolder: String, fileName: String) {
        try {
            val inputStream = context.assets.open("$assetFolder/$fileName")
            val outputFile = File(context.filesDir, fileName)
            
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            Log.d(TAG, "Copied $fileName to ${outputFile.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error copying $fileName", e)
        }
    }
    
    /**
     * MBTiles 파일 경로 반환
     */
    fun getMBTilesFilePath(fileName: String): String? {
        val mbtilesFile = File(context.filesDir, fileName)
        
        if (!mbtilesFile.exists()) {
            Log.w(TAG, "MBTiles file not found: ${mbtilesFile.absolutePath}")
            return null
        }
        
        return mbtilesFile.absolutePath
    }
    
    /**
     * 사용 가능한 MBTiles 파일 목록 반환
     */
    fun getAvailableMBTilesFiles(): List<String> {
        val mbtilesDir = context.filesDir
        return mbtilesDir.listFiles()
            ?.filter { it.name.endsWith(".mbtiles") }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * MBTiles 파일이 존재하는지 확인
     */
    fun isMBTilesFileAvailable(fileName: String): Boolean {
        val mbtilesFile = File(context.filesDir, fileName)
        return mbtilesFile.exists()
    }
    
    /**
     * MBTiles 파일 크기 반환
     */
    fun getMBTilesFileSize(fileName: String): Long {
        val mbtilesFile = File(context.filesDir, fileName)
        return if (mbtilesFile.exists()) mbtilesFile.length() else 0L
    }
}
