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
    val textField: String = "VALUE",
    val isDynamicSymbol: Boolean = false, // 동적 심볼 사용 여부
    val iconMapping: Map<String, String> = emptyMap() // ICON 값과 drawable 리소스명 매핑
)

/**
 * 레이어 타입 정의
 */
enum class LayerType {
    LINE,      // 선 레이어
    AREA,      // 면 레이어
    TEXT,     // 텍스트 레이어
    SYMBOL     // 심볼 레이어
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
            131 to Color.parseColor("#07FDFF91"),
            14 to Color.parseColor("#FFFFF8CA")
        )
    )
    
    /**
     * BDR_COLOR 색상 매핑 설정
     */
    private val bdrColorMappings = mapOf(
        0 to Color.parseColor("#000000"),   // COLOR_BLACK
        1 to Color.parseColor("#000080"),   // COLOR_BLUE
        2 to Color.parseColor("#008000"),   // COLOR_GREEN
        3 to Color.parseColor("#008080"),   // COLOR_CYAN
        4 to Color.parseColor("#800000"),   // COLOR_RED
        5 to Color.parseColor("#800080"),   // COLOR_MAGENTA
        6 to Color.parseColor("#808000"),   // COLOR_YELLOW
        7 to Color.parseColor("#C0C0C0"),   // COLOR_LIGHT_GRAY
        8 to Color.parseColor("#808080"),   // COLOR_DARK_GRAY
        9 to Color.parseColor("#0000FF"),   // COLOR_LIGHT_BLUE
        10 to Color.parseColor("#00FF00"),  // COLOR_LIGHT_GREEN
        11 to Color.parseColor("#00FFFF"),  // COLOR_LIGHT_CYAN
        12 to Color.parseColor("#FF0000"),  // COLOR_LIGHT_RED
        13 to Color.parseColor("#FF00FF"),  // COLOR_LIGHT_MAGENTA
        14 to Color.parseColor("#FFFF00"),  // COLOR_LIGHT_YELLOW
        15 to Color.parseColor("#FFFFFF"),  // COLOR_WHITE
        16 to Color.parseColor("#8CC8FF"),  // COLOR_PALE_BLUE
        17 to Color.parseColor("#B4F0B4"),  // COLOR_PALE_GREEN
        18 to Color.parseColor("#FFC8C8"),  // COLOR_PALE_PINK
        19 to Color.parseColor("#FF8000"),  // COLOR_ORANGE
        20 to Color.parseColor("#FFE696"),  // COLOR_PALE_ORANGE
        21 to Color.parseColor("#FFFFC0"),  // COLOR_PALE_YELLOW
        22 to Color.parseColor("#E0FFB0"),
        23 to Color.parseColor("#FFFF60"),
        24 to Color.parseColor("#D0D040"),
        25 to Color.parseColor("#80FF60"),
        26 to Color.parseColor("#000040"),
        27 to Color.parseColor("#004000"),
        28 to Color.parseColor("#807346"),
        29 to Color.parseColor("#404000"),
        30 to Color.parseColor("#400000"),
        40 to Color.parseColor("#000033"),
        41 to Color.parseColor("#000066"),
        42 to Color.parseColor("#000099"),
        43 to Color.parseColor("#0000CC"),
        44 to Color.parseColor("#0000FF"),  // Same As 9
        50 to Color.parseColor("#0033FF"),
        56 to Color.parseColor("#0066FF"),
        62 to Color.parseColor("#0099FF"),
        68 to Color.parseColor("#00CCFF"),
        74 to Color.parseColor("#00FFFF"),  // Same As 11
        125 to Color.parseColor("#666666"),
        131 to Color.parseColor("#669966"),
        134 to Color.parseColor("#6699FF"),
        143 to Color.parseColor("#66FF66"),
        160 to Color.parseColor("#996633"),
        168 to Color.parseColor("#999999"),
        174 to Color.parseColor("#99CC99"),
        176 to Color.parseColor("#99CCFF"),
        193 to Color.parseColor("#CC33CC"),
        210 to Color.parseColor("#CCCC99"),
        218 to Color.parseColor("#CCFFFF"),
        226 to Color.parseColor("#FF3333"),
        233 to Color.parseColor("#FF6666"),
        239 to Color.parseColor("#FF9966"),
        248 to Color.parseColor("#FFCCFF"),
        255 to Color.TRANSPARENT  // COLOR_TRANSPARENT
    )
    
    /**
     * PMTiles 파일들의 기본 설정
     */
    val pmtilesConfigs: List<PMTilesConfig>
        get() = listOf(
            PMTilesConfig(
                fileName = "lineTiles.pmtiles",
                sourceName = "lineTiles-source",
                sourceLayer = "line_map",
                layerType = LayerType.LINE,
                colorMapping = defaultColorMappings["lineTiles"] ?: emptyMap()
            ),
            PMTilesConfig(
                fileName = "areaMapTiles.pmtiles",
                sourceName = "areaTiles-source",
                sourceLayer = "area_map",
                layerType = LayerType.AREA,
                colorMapping = defaultColorMappings["areaTiles"] ?: emptyMap()
            ),
            PMTilesConfig(
                fileName = "a_fishfarm.pmtiles",
                sourceName = "a_fishfarm-source",
                sourceLayer = "a_fishfarm",
                layerType = LayerType.AREA,
                colorMapping = defaultColorMappings["areaTiles"] ?: emptyMap()
            ),
            PMTilesConfig(
                fileName = "a_achare_4.pmtiles",
                sourceName = "areAchare-source",
                sourceLayer = "a_achare_4",
                layerType = LayerType.AREA,
                colorMapping = defaultColorMappings["areaTiles"] ?: emptyMap()
            ),
            PMTilesConfig(
                fileName = "p_soundg_1.pmtiles",
                sourceName = "depthTiles1-source",
                sourceLayer = "soundg",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "ELEVATION"
            ),
            PMTilesConfig(
                fileName = "p_soundg_2.pmtiles",
                sourceName = "depthTiles2-source",
                sourceLayer = "soundg",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "ELEVATION"
            ),
            PMTilesConfig(
                fileName = "p_soundg_3.pmtiles",
                sourceName = "depthTiles3-source",
                sourceLayer = "soundg",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "ELEVATION"
            ),
            PMTilesConfig(
                fileName = "p_soundg_4.pmtiles",
                sourceName = "depthTiles4-source",
                sourceLayer = "soundg",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "ELEVATION"
            ),
            PMTilesConfig(
                fileName = "p_soundg_5.pmtiles",
                sourceName = "depthTiles5-source",
                sourceLayer = "soundg",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "ELEVATION"
            ),
            PMTilesConfig(
                fileName = "p_soundg_6.pmtiles",
                sourceName = "depthTiles6-source",
                sourceLayer = "soundg",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "ELEVATION"
            ),
            PMTilesConfig(
                fileName = "p_sbdare_1.pmtiles",
                sourceName = "sbdareTiles1-source",
                sourceLayer = "sbdare",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "NATSUR_Nat"
            ),
            PMTilesConfig(
                fileName = "p_sbdare_2.pmtiles",
                sourceName = "sbdareTiles2-source",
                sourceLayer = "sbdare",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "NATSUR_Nat"
            ),
            PMTilesConfig(
                fileName = "p_sbdare_3.pmtiles",
                sourceName = "sbdareTiles3-source",
                sourceLayer = "sbdare",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "NATSUR_Nat"
            ),
            PMTilesConfig(
                fileName = "p_sbdare_4.pmtiles",
                sourceName = "sbdareTiles4-source",
                sourceLayer = "sbdare",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "NATSUR_Nat"
            ),
            PMTilesConfig(
                fileName = "p_sbdare_5.pmtiles",
                sourceName = "sbdareTiles5-source",
                sourceLayer = "sbdare",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "NATSUR_Nat"
            ),
            PMTilesConfig(
                fileName = "p_sbdare_6.pmtiles",
                sourceName = "sbdareTiles6-source",
                sourceLayer = "sbdare",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "NATSUR_Nat"
            ),
            PMTilesConfig(
                fileName = "name_level.pmtiles",
                sourceName = "localTiles-source",
                sourceLayer = "name_level",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "VALUE"
            ),
            PMTilesConfig(
                fileName = "p_fishfarm.pmtiles",
                sourceName = "p_fishfarm-source",
                sourceLayer = "p_fishfarm",
                layerType = LayerType.TEXT,
                hasTextLayer = true,
                textField = "VALUE"
            ),
            PMTilesConfig(
                fileName = "p_wrecks.pmtiles",
                sourceName = "wrecks-source",
                sourceLayer = "wrecks",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                isDynamicSymbol = true,
                iconMapping = mapOf(
                    "wrecks1" to "wrecks1",
                    "wrecks2" to "wrecks2",
                    "wrecks3" to "wrecks3",
                    "wrecks5" to "wrecks5"
                )
            ),
            PMTilesConfig(
                fileName = "p_wreck_159_ex.pmtiles",
                sourceName = "wrecks-ex-source",
                sourceLayer = "p_wreck_159_ex",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                textField = "wrecks"
            ),
            PMTilesConfig(
                fileName = "lighthouse.pmtiles",
                sourceName = "lighthouse-source",
                sourceLayer = "lighthouse",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                textField = "lighthouse"
            ),
            PMTilesConfig(
                fileName = "p_bcnlat_6.pmtiles",
                sourceName = "bcnlat-source",
                sourceLayer = "p_bcnlat_6",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                textField = "bcnlat"
            ),
            PMTilesConfig(
                fileName = "p_obstrn_86.pmtiles",
                sourceName = "obstrn-source",
                sourceLayer = "p_obstrn_86",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                textField = "obstrn"
            ),
            PMTilesConfig(
                fileName = "p_achare_4.pmtiles",
                sourceName = "achare-source",
                sourceLayer = "p_achare_4",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                textField = "achare"
            ),
            PMTilesConfig(
                fileName = "p_reef.pmtiles",
                sourceName = "reef-source",
                sourceLayer = "p_reef",
                layerType = LayerType.SYMBOL,
                isDynamicSymbol = true,
                iconMapping = mapOf(
                    "reef_1" to "reef_1",
                    "reef_2" to "reef_2",
                    "reef_3" to "reef_3",
                    "reef_4" to "reef_4",
                    "reef_5" to "reef_5",
                    "reef_6" to "reef_6",
                    "reef_8" to "reef_8",
                    "reef_9" to "reef_9",
                    "reef_10" to "reef_10",
                    "reef_11" to "reef_11",
                    "reef_12" to "reef_12",
                    "reef_13" to "reef_13",
                    "reef_14" to "reef_14",
                    "reef_15" to "reef_15",
                    "reef_16" to "reef_16",
                    "reef_17" to "reef_17",
                    "reef_18" to "reef_18",
                    "reef_19" to "reef_19",
                    "reef_20" to "reef_20",
                    "reef_21" to "reef_21",
                    "reef_22" to "reef_22",
                    "reef_23" to "reef_23",
                    "reef_24" to "reef_24",
                    "reef_25" to "reef_25",
                    "reef_26" to "reef_26",
                    "reef_27" to "reef_27",
                    "reef_28" to "reef_28",
                    "reef_29" to "reef_29",
                    "reef_30" to "reef_30",
                    "reef_31" to "reef_31",
                    "reef_32" to "reef_32",
                    "reef_33" to "reef_33",
                    "reef_34" to "reef_34",
                    "reef_35" to "reef_35",
                    "reef_36" to "reef_36",
                    "reef_37" to "reef_37",
                    "reef_38" to "reef_38",
                    "reef_39" to "reef_39",
                    "reef_40" to "reef_40",
                    "reef_41" to "reef_41",
                    "reef_42" to "reef_42",
                    "reef_43" to "reef_43",
                    "reef_45" to "reef_45",
                    "reef_46" to "reef_46",
                    "reef_47" to "reef_47",
                    "reef_48" to "reef_48",
                    "reef_49" to "reef_49",
                    "reef_50" to "reef_50",
                    "reef_51" to "reef_51",
                    "reef_52" to "reef_52",
                    "reef_53" to "reef_53",
                    "reef_54" to "reef_54",
                    "reef_55" to "reef_55",
                    "reef_56" to "reef_56",
                    "reef_60" to "reef_60",
                    "light" to "light_icon",
                    "beacon" to "beacon_icon"
                )
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
    
    /**
     * BDR_COLOR 색상 매핑을 가져오는 함수
     */
    fun getBdrColorMapping(): Map<Int, Int> {
        return bdrColorMappings
    }
}

