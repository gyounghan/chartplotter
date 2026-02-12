package com.marineplay.chartplotter

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

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
    val iconMapping: Map<String, String> = emptyMap(), // ICON 값과 drawable 리소스명 매핑
    val iconSize: Float = 1.0f // 아이콘 크기 배율 (1.0 = 기본, 0.5 = 절반, 2.0 = 두배)
) {
    /** PMTilesConfig → JSONObject 변환 */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("fileName", fileName)
            put("sourceName", sourceName)
            put("sourceLayer", sourceLayer)
            put("layerType", layerType.name)
            
            // colorMapping: Int→Int 를 String→String(hex) 으로
            if (colorMapping.isNotEmpty()) {
                put("colorMapping", JSONObject().apply {
                    colorMapping.forEach { (key, color) ->
                        put(key.toString(), String.format("#%08X", color))
                    }
                })
            }
            
            put("hasTextLayer", hasTextLayer)
            put("textField", textField)
            put("isDynamicSymbol", isDynamicSymbol)
            
            if (iconMapping.isNotEmpty()) {
                put("iconMapping", JSONObject().apply {
                    iconMapping.forEach { (k, v) -> put(k, v) }
                })
            }
            
            put("iconSize", iconSize.toDouble())
        }
    }
    
    companion object {
        /** JSONObject → PMTilesConfig 변환 */
        fun fromJson(json: JSONObject): PMTilesConfig {
            val colorMapping = mutableMapOf<Int, Int>()
            json.optJSONObject("colorMapping")?.let { obj ->
                obj.keys().forEach { key ->
                    try {
                        colorMapping[key.toInt()] = Color.parseColor(obj.getString(key))
                    } catch (_: Exception) {}
                }
            }
            
            val iconMapping = mutableMapOf<String, String>()
            json.optJSONObject("iconMapping")?.let { obj ->
                obj.keys().forEach { key ->
                    iconMapping[key] = obj.getString(key)
                }
            }
            
            return PMTilesConfig(
                fileName = json.getString("fileName"),
                sourceName = json.getString("sourceName"),
                sourceLayer = json.getString("sourceLayer"),
                layerType = try { LayerType.valueOf(json.getString("layerType")) } catch (_: Exception) { LayerType.TEXT },
                colorMapping = colorMapping,
                hasTextLayer = json.optBoolean("hasTextLayer", false),
                textField = json.optString("textField", "VALUE"),
                isDynamicSymbol = json.optBoolean("isDynamicSymbol", false),
                iconMapping = iconMapping,
                iconSize = json.optDouble("iconSize", 1.0).toFloat()
            )
        }
    }
}

/**
 * PMTiles 설정 파일 전체를 표현하는 데이터 클래스
 */
data class PMTilesConfigFile(
    val version: Int = 1,
    val configs: List<PMTilesConfig>
) {
    /** 전체 설정 → JSON 문자열 (pretty print) */
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        val arr = JSONArray()
        configs.forEach { arr.put(it.toJson()) }
        root.put("configs", arr)
        return root.toString(2) // 들여쓰기 2칸
    }
    
    companion object {
        /** JSON 문자열 → PMTilesConfigFile */
        fun fromJsonString(jsonString: String): PMTilesConfigFile {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val arr = root.getJSONArray("configs")
            val configs = (0 until arr.length()).map { PMTilesConfig.fromJson(arr.getJSONObject(it)) }
            return PMTilesConfigFile(version = version, configs = configs)
        }
    }
}

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
 * 
 * 로딩 우선순위:
 * 1. 외부 저장소 (getExternalFilesDir/charts/) + pmtiles_config.json
 * 2. 내부 assets/pmtiles/ + 하드코딩 설정 (기존 방식, fallback)
 */
object PMTilesManager {
    
    private const val TAG = "[PMTilesManager]"
    
    /** 외부 차트 디렉토리 이름 */
    const val EXTERNAL_CHARTS_DIR = "charts"
    /** 외부 PMTiles 디렉토리 이름 */
    const val EXTERNAL_PMTILES_DIR = "pmtiles"
    /** 외부 아이콘 디렉토리 이름 */
    const val EXTERNAL_ICONS_DIR = "icons"
    /** 외부 설정 파일 이름 */
    const val CONFIG_FILE_NAME = "pmtiles_config.json"
    
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
                fileName = "p_lights.pmtiles",
                sourceName = "p_lights-source",
                sourceLayer = "p_lights",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                isDynamicSymbol = true,
                iconMapping = mapOf(
                    "lights" to "lights",
                    "lights_red" to "lights_red",
                    "lights_white" to "lights_white",
                    "lights_yellow" to "lights_yellow",
                    "lights_green" to "lights_green",
                ),
                iconSize = 2f,
            ),
            PMTilesConfig(
                fileName = "p_boylat.pmtiles",
                sourceName = "p_boylat-source",
                sourceLayer = "p_boylat",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                isDynamicSymbol = true,
                iconMapping = mapOf(
                    "boylat_red" to "boylat_red",
                    "boylat_green" to "boylat_green",
                ),
                iconSize = 2f,
            ),
            PMTilesConfig(
                fileName = "p_boyspp.pmtiles",
                sourceName = "p_boyspp-source",
                sourceLayer = "p_boyspp",
                layerType = LayerType.SYMBOL,
                hasTextLayer = true,
                isDynamicSymbol = true,
                iconMapping = mapOf(
                    "boyspp_conical" to "boyspp_conical",
                    "boyspp_cylindrical" to "boyspp_cylindrical",
                    "boyspp_spherical" to "boyspp_spherical",
                    "boyspp_pillar" to "boyspp_pillar",
                ),
                iconSize = 2f,
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
     * 파일명으로부터 기본 설정을 자동 생성
     * 규칙:
     * - l_로 시작 -> LINE 타입
     * - a_로 시작 -> AREA 타입
     * - p_로 시작 -> TEXT 타입 (기본값)
     */
    fun createDefaultConfigFromFileName(fileName: String): PMTilesConfig {
        val baseName = fileName.removeSuffix(".pmtiles")
        
        // 파일명 규칙에 따라 레이어 타입 결정
        val (layerType, sourceLayer, textField) = when {
            // l_로 시작 -> LINE
            baseName.startsWith("l_") -> {
                val layerName = baseName.removePrefix("l_")
                Triple(
                    LayerType.LINE,
                    layerName.ifEmpty { "line_map" },
                    "VALUE"
                )
            }
            // a_로 시작 -> AREA
            baseName.startsWith("a_") -> {
                val layerName = baseName.removePrefix("a_")
                Triple(
                    LayerType.AREA,
                    layerName.ifEmpty { "area_map" },
                    "VALUE"
                )
            }
            // p_로 시작 -> TEXT (기본값)
            baseName.startsWith("p_") -> {
                val layerName = baseName.removePrefix("p_")
                // p_ 다음 부분에서 실제 레이어명 추출 (예: p_soundg_1 -> soundg)
                val actualLayerName = layerName.split("_").firstOrNull() ?: layerName
                Triple(
                    LayerType.TEXT,
                    actualLayerName,
                    when {
                        actualLayerName.contains("soundg") -> "ELEVATION"
                        actualLayerName.contains("sbdare") -> "NATSUR_Nat"
                        else -> "VALUE"
                    }
                )
            }
            // 기존 파일명 호환성 (fallback)
            baseName.startsWith("line") -> Triple(
                LayerType.LINE,
                "line_map",
                "VALUE"
            )
            baseName.startsWith("area") -> Triple(
                LayerType.AREA,
                "area_map",
                "VALUE"
            )
            // 기본값: TEXT
            else -> Triple(
                LayerType.TEXT,
                baseName,
                "VALUE"
            )
        }
        
        // sourceName 생성
        val sourceName = "${baseName}-source"
        
        // 색상 매핑 결정
        val colorMapping = when (layerType) {
            LayerType.LINE -> defaultColorMappings["lineTiles"] ?: emptyMap()
            LayerType.AREA -> defaultColorMappings["areaTiles"] ?: emptyMap()
            else -> emptyMap()
        }
        
        return PMTilesConfig(
            fileName = fileName,
            sourceName = sourceName,
            sourceLayer = sourceLayer,
            layerType = layerType,
            colorMapping = colorMapping,
            hasTextLayer = layerType == LayerType.TEXT || layerType == LayerType.SYMBOL,
            textField = textField
        )
    }
    
    /**
     * 파일명으로 PMTiles 설정을 찾는 함수 (내부 하드코딩 설정 기준)
     * 기존 설정이 있으면 우선 사용하고, 없으면 파일명 규칙에 따라 자동 생성
     */
    fun findConfigByFileName(fileName: String): PMTilesConfig? {
        // 1. 기존 설정에서 찾기 (우선순위 1)
        pmtilesConfigs.find { it.fileName == fileName }?.let { 
            return it
        }
        
        // 2. 없으면 파일명 규칙에 따라 자동 생성
        android.util.Log.d(TAG, "설정이 없어 파일명 규칙으로 자동 생성: $fileName")
        return createDefaultConfigFromFileName(fileName)
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
    
    // ========================================================================
    // 외부 저장소 로딩 관련 함수들
    // ========================================================================
    
    /**
     * 외부 차트 디렉토리 경로를 반환
     * 경로: /sdcard/Android/data/{packageName}/files/charts/
     */
    fun getExternalChartsDir(context: android.content.Context): java.io.File {
        return java.io.File(context.getExternalFilesDir(null), EXTERNAL_CHARTS_DIR)
    }
    
    /**
     * 외부 PMTiles 디렉토리 경로를 반환
     */
    fun getExternalPMTilesDir(context: android.content.Context): java.io.File {
        return java.io.File(getExternalChartsDir(context), EXTERNAL_PMTILES_DIR)
    }
    
    /**
     * 외부 아이콘 디렉토리 경로를 반환
     */
    fun getExternalIconsDir(context: android.content.Context): java.io.File {
        return java.io.File(getExternalChartsDir(context), EXTERNAL_ICONS_DIR)
    }
    
    /**
     * 외부 설정 파일 경로를 반환
     */
    fun getExternalConfigFile(context: android.content.Context): java.io.File {
        return java.io.File(getExternalChartsDir(context), CONFIG_FILE_NAME)
    }
    
    /**
     * 외부 디렉토리 구조를 초기화 (없으면 생성)
     */
    fun ensureExternalDirectories(context: android.content.Context) {
        getExternalPMTilesDir(context).mkdirs()
        getExternalIconsDir(context).mkdirs()
        android.util.Log.d(TAG, "외부 디렉토리 확인/생성: ${getExternalChartsDir(context).absolutePath}")
    }
    
    /**
     * 외부 저장소에 PMTiles 파일이 있는지 확인
     */
    fun hasExternalPMTiles(context: android.content.Context): Boolean {
        val dir = getExternalPMTilesDir(context)
        if (!dir.exists()) return false
        return dir.listFiles { f -> f.extension == "pmtiles" }?.isNotEmpty() == true
    }
    
    /**
     * 외부 저장소에 설정 파일이 있는지 확인
     */
    fun hasExternalConfig(context: android.content.Context): Boolean {
        return getExternalConfigFile(context).exists()
    }
    
    /**
     * 외부 PMTiles 파일 목록을 가져오는 함수
     * @return 파일명 리스트 (예: ["lineTiles.pmtiles", "p_soundg_1.pmtiles"])
     */
    fun getExternalPMTilesFiles(context: android.content.Context): List<String> {
        val dir = getExternalPMTilesDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "pmtiles" }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }
    
    /**
     * 외부 PMTiles 파일들의 File 객체를 가져오는 함수
     */
    fun getExternalPMTilesFileObjects(context: android.content.Context): List<java.io.File> {
        val dir = getExternalPMTilesDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "pmtiles" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }
    
    /**
     * 외부 JSON 설정 파일을 로드
     * @return PMTilesConfigFile 또는 null (파일 없음/파싱 실패)
     */
    fun loadExternalConfig(context: android.content.Context): PMTilesConfigFile? {
        val configFile = getExternalConfigFile(context)
        if (!configFile.exists()) {
            android.util.Log.d(TAG, "외부 설정 파일 없음: ${configFile.absolutePath}")
            return null
        }
        
        return try {
            val jsonString = configFile.readText(Charsets.UTF_8)
            val configFileObj = PMTilesConfigFile.fromJsonString(jsonString)
            android.util.Log.d(TAG, "외부 설정 로드 성공: ${configFileObj.configs.size}개 설정 (v${configFileObj.version})")
            configFileObj
        } catch (e: Exception) {
            android.util.Log.e(TAG, "외부 설정 파싱 실패: ${e.message}")
            null
        }
    }
    
    /**
     * 외부 아이콘 파일 경로를 반환 (없으면 null)
     * @param iconName 아이콘 이름 (확장자 제외)
     */
    fun getExternalIconFile(context: android.content.Context, iconName: String): java.io.File? {
        val iconsDir = getExternalIconsDir(context)
        if (!iconsDir.exists()) return null
        
        // png, jpg, bmp 순서로 탐색
        for (ext in listOf("png", "jpg", "bmp")) {
            val file = java.io.File(iconsDir, "$iconName.$ext")
            if (file.exists()) return file
        }
        return null
    }
    
    /**
     * 현재 하드코딩된 설정을 JSON 파일로 내보내기
     * 외부 charts/ 디렉토리에 pmtiles_config.json 생성
     */
    fun exportDefaultConfigToExternal(context: android.content.Context): Boolean {
        return try {
            ensureExternalDirectories(context)
            val configFile = getExternalConfigFile(context)
            val configData = PMTilesConfigFile(version = 1, configs = pmtilesConfigs)
            configFile.writeText(configData.toJsonString(), Charsets.UTF_8)
            android.util.Log.d(TAG, "기본 설정 내보내기 성공: ${configFile.absolutePath}")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "기본 설정 내보내기 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 통합 설정 로딩: 외부 설정 우선, 없으면 내부 하드코딩 사용
     * @return Pair<List<PMTilesConfig>, Boolean> - (설정 리스트, 외부 설정 사용 여부)
     */
    fun loadConfigs(context: android.content.Context): Pair<List<PMTilesConfig>, Boolean> {
        // 1. 외부 설정 시도
        val externalConfig = loadExternalConfig(context)
        if (externalConfig != null && externalConfig.configs.isNotEmpty()) {
            android.util.Log.d(TAG, "✅ 외부 설정 사용: ${externalConfig.configs.size}개")
            return Pair(externalConfig.configs, true)
        }
        
        // 2. Fallback: 내부 하드코딩 설정
        android.util.Log.d(TAG, "📦 내부 하드코딩 설정 사용 (fallback): ${pmtilesConfigs.size}개")
        return Pair(pmtilesConfigs, false)
    }
    
    /**
     * 통합 PMTiles 파일 정보 로딩
     * 외부 파일 우선, 없으면 assets 사용
     * @return Triple<List<파일명>, Boolean(외부 사용 여부), List<File>?(외부 파일 객체, 외부 사용 시)>
     */
    data class PMTilesSource(
        val fileNames: List<String>,
        val isExternal: Boolean,
        val externalFiles: List<java.io.File> = emptyList()
    )
    
    fun loadPMTilesSource(context: android.content.Context): PMTilesSource {
        // 1. 외부 파일 확인
        if (hasExternalPMTiles(context)) {
            val files = getExternalPMTilesFileObjects(context)
            val names = files.map { it.name }
            android.util.Log.d(TAG, "✅ 외부 PMTiles 사용: ${files.size}개 파일")
            return PMTilesSource(fileNames = names, isExternal = true, externalFiles = files)
        }
        
        // 2. Fallback: assets
        val assetFiles = getPMTilesFilesFromAssets(context)
        android.util.Log.d(TAG, "📦 내부 assets PMTiles 사용 (fallback): ${assetFiles.size}개 파일")
        return PMTilesSource(fileNames = assetFiles, isExternal = false)
    }
}

