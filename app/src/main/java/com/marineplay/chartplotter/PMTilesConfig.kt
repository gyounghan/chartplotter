package com.marineplay.chartplotter

import android.graphics.Color

/**
 * PMTiles 파일의 설정을 정의하는 데이터 클래스
 */
data class PMTilesConfig(
    val fileName: String,
    val sourceName: String,
    val sourceLayer: String,
    val layerType: LayerType,
    val colorMapping: Map<Int, Int> = emptyMap(),
    val hasTextLayer: Boolean = false,
    val textField: String = "VALUE"
)

/**
 * 레이어 타입 정의
 */
enum class LayerType {
    LINE,      // 선 레이어
    AREA,      // 면 레이어
    SYMBOL     // 심볼/텍스트 레이어
}

/**
 * PMTiles 설정 관리자
 */
object PMTilesManager {
    
    /**
     * 기본 색상 매핑 설정
     */
    private val defaultColorMappings = mapOf(
        // lineTiles용 색상 매핑
        "lineTiles" to mapOf(
            98 to Color.parseColor("#E53935"),   // 빨강
            96 to Color.parseColor("#1E88E5"),   // 파랑
            12 to Color.parseColor("#FB8C00")    // 주황
        ),
        
        // areaTiles용 색상 매핑
        "areaTiles" to mapOf(
            160 to Color.parseColor("#C3FFFD"),
            11 to Color.parseColor("#00FFFA"),
            62 to Color.parseColor("#079CFF"),
            68 to Color.parseColor("#07E0FF"),
            74 to Color.parseColor("#079CFF80"),
            131 to Color.parseColor("#07FDFF91")
        )
    )
    
    /**
     * PMTiles 파일들의 기본 설정
     */
    val pmtilesConfigs = listOf(
        PMTilesConfig(
            fileName = "lineTiles.pmtiles",
            sourceName = "lineTiles-source",
            sourceLayer = "line_map",
            layerType = LayerType.LINE,
            colorMapping = defaultColorMappings["lineTiles"] ?: emptyMap()
        ),
        PMTilesConfig(
            fileName = "areaTiles.pmtiles",
            sourceName = "areaTiles-source",
            sourceLayer = "area_map",
            layerType = LayerType.AREA,
            colorMapping = defaultColorMappings["areaTiles"] ?: emptyMap()
        ),
        PMTilesConfig(
            fileName = "p_1_129_soundg.pmtiles",
            sourceName = "depthTiles-source",
            sourceLayer = "P_1_129_SOUNDG",
            layerType = LayerType.SYMBOL,
            hasTextLayer = true,
            textField = "VALUE"
        )
    )
    
    /**
     * assets 폴더에서 PMTiles 파일 목록을 가져오는 함수
     */
    fun getPMTilesFilesFromAssets(context: android.content.Context): List<String> {
        return try {
            context.assets.list("pmtiles")?.filter { it.endsWith(".pmtiles") } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("[PMTilesManager]", "assets 폴더에서 PMTiles 파일 목록을 가져오는데 실패: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 파일명으로 PMTiles 설정을 찾는 함수
     */
    fun findConfigByFileName(fileName: String): PMTilesConfig? {
        return pmtilesConfigs.find { it.fileName == fileName }
    }
    
    /**
     * 기본 색상 매핑을 가져오는 함수
     */
    fun getDefaultColorMapping(sourceName: String): Map<Int, Int> {
        return defaultColorMappings[sourceName] ?: emptyMap()
    }
}

