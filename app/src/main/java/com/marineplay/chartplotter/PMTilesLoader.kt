package com.marineplay.chartplotter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.VectorSource
import java.io.File

/**
 * PMTiles 파일을 자동으로 로드하고 MapLibre에 적용하는 유틸리티 클래스
 */
object PMTilesLoader {
    
    /**
     * assets 폴더에서 PMTiles 파일을 복사하는 함수
     */
    private fun copyPmtilesFromAssets(context: Context, assetPath: String, outName: String): File {
        val outDir = File(context.filesDir, "pmtiles").apply { mkdirs() }
        val out = File(outDir, outName)
        context.assets.open(assetPath).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }
    
    /**
     * PMTiles 파일을 자동으로 로드하고 MapLibre에 적용하는 메인 함수
     */
    fun loadPMTilesFromAssets(context: Context, map: MapLibreMap) {
        try {
            // assets 폴더에서 PMTiles 파일 목록 가져오기
            val pmtilesFiles = PMTilesManager.getPMTilesFilesFromAssets(context)
            Log.d("[PMTilesLoader]", "발견된 PMTiles 파일들: $pmtilesFiles")
            
            if (pmtilesFiles.isEmpty()) {
                Log.w("[PMTilesLoader]", "PMTiles 파일이 없습니다. 기본 스타일을 사용합니다.")
                loadDefaultStyle(map)
                return
            }
            
            // PMTiles 파일들을 복사하고 설정 정보 수집
            val pmtilesConfigs = mutableListOf<PMTilesConfig>()
            val copiedFiles = mutableListOf<File>()
            
            for (fileName in pmtilesFiles) {
                try {
                    val copiedFile = copyPmtilesFromAssets(context, "pmtiles/$fileName", fileName)
                    copiedFiles.add(copiedFile)
                    
                    // 설정 정보 찾기
                    val config = PMTilesManager.findConfigByFileName(fileName)
                    if (config != null) {
                        pmtilesConfigs.add(config)
                        Log.d("[PMTilesLoader]", "PMTiles 설정 로드됨: $fileName")
                    } else {
                        Log.w("[PMTilesLoader]", "설정을 찾을 수 없음: $fileName")
                    }
                } catch (e: Exception) {
                    Log.e("[PMTilesLoader]", "파일 복사 실패: $fileName, ${e.message}")
                }
            }
            
            if (pmtilesConfigs.isEmpty()) {
                Log.w("[PMTilesLoader]", "유효한 PMTiles 설정이 없습니다. 기본 스타일을 사용합니다.")
                loadDefaultStyle(map)
                return
            }
            
            // MapLibre에 PMTiles 적용
            applyPMTilesToMap(map, pmtilesConfigs, copiedFiles, context)
            
        } catch (e: Exception) {
            Log.e("[PMTilesLoader]", "PMTiles 로드 중 오류: ${e.message}")
            e.printStackTrace()
            loadDefaultStyle(map)
        }
    }
    
    /**
     * PMTiles를 MapLibre에 적용하는 함수
     */
    private fun applyPMTilesToMap(map: MapLibreMap, configs: List<PMTilesConfig>, files: List<File>, context: Context) {
        val styleJson = """
{
  "version": 8,
  "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
  "sources": {},
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": { "background-color": "#FFFFFF" }
    }
  ]
}
""".trimIndent()

        map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
            try {
                // 각 PMTiles 파일을 소스로 추가
                for (i in configs.indices) {
                    val config = configs[i]
                    val file = files[i]
                    
                    if (file.exists()) {
                        val pmtilesUrl = "pmtiles://file://${file.absolutePath}"
                        val source = VectorSource(config.sourceName, pmtilesUrl)
                        style.addSource(source)
                        Log.d("[PMTilesLoader]", "소스 추가됨: ${config.sourceName}")
                    }
                }
                
                // 각 설정에 따라 레이어 추가
                for (config in configs) {
                    when (config.layerType) {
                        LayerType.LINE -> addLineLayer(style, config)
                        LayerType.AREA -> addAreaLayer(style, config)
                        LayerType.TEXT -> addTextLayer(style, config)
                        LayerType.SYMBOL -> addSymbolLayer(style, config, context)

                    }
                }
                
                Log.d("[PMTilesLoader]", "모든 PMTiles 레이어가 성공적으로 추가되었습니다.")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "레이어 추가 중 오류: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 선 레이어를 추가하는 함수
     */
    private fun addLineLayer(style: Style, config: PMTilesConfig) {
        val lineLayer = LineLayer("${config.sourceName}-lines", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            setMinZoom(0f)
            setMaxZoom(24f)
            
            // 색상 표현식 생성
            val colorExpr = if (config.colorMapping.isNotEmpty()) {
                match(
                    toNumber(coalesce(get("COLOR"), get("BFR_COLOR"), get("LAYER"))),
                    *config.colorMapping.entries.flatMap { 
                        listOf(literal(it.key), color(it.value)) 
                    }.toTypedArray(),
                    color(Color.parseColor("#666666")) // 기본 색상
                )
            } else {
                color(Color.parseColor("#666666"))
            }
            
            // 두께 표현식 생성
            val widthExpr = coalesce(
                toNumber(get("WIDTH")),
                toNumber(get("BFR_WIDTH")),
                literal(1.0f)
            )
            
            setProperties(
                PropertyFactory.lineColor(colorExpr),
                PropertyFactory.lineWidth(widthExpr)
            )
        }
        
        style.addLayer(lineLayer)
        Log.d("[PMTilesLoader]", "선 레이어 추가됨: ${config.sourceName}-lines")
    }
    
    /**
     * 면 레이어를 추가하는 함수
     */
    private fun addAreaLayer(style: Style, config: PMTilesConfig) {
        // 면 채우기 레이어
        val fillLayer = FillLayer("${config.sourceName}-areas", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            setMinZoom(0f)
            setMaxZoom(24f)
            
            // 색상 표현식 생성
            val colorExpr = if (config.colorMapping.isNotEmpty()) {
                match(
                    toNumber(coalesce(get("COLOR"), get("BFR_COLOR"), get("LAYER"))),
                    *config.colorMapping.entries.flatMap { 
                        listOf(literal(it.key), color(it.value)) 
                    }.toTypedArray(),
                    color(Color.parseColor("#FFFFF8CA")) // 기본 색상
                )
            } else {
                color(Color.parseColor("#FFFFF8CA"))
            }
            
            setProperties(
                PropertyFactory.fillColor(colorExpr),
                PropertyFactory.fillOpacity(0.6f)
            )
        }
        
        style.addLayer(fillLayer)
        
        // 면 경계선 레이어
        val lineLayer = LineLayer("${config.sourceName}-lines", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            setProperties(
                PropertyFactory.lineColor(Color.parseColor("#666666")),
                PropertyFactory.lineWidth(1.0f)
            )
        }
        
        style.addLayer(lineLayer)
        Log.d("[PMTilesLoader]", "면 레이어 추가됨: ${config.sourceName}-areas")
    }
    
    /**
     * 심볼/텍스트 레이어를 추가하는 함수
     */
    private fun addTextLayer(style: Style, config: PMTilesConfig) {
        if (!config.hasTextLayer) return
        val isDepth = config.sourceName.contains("depth", ignoreCase = true)

        val symbolLayer = SymbolLayer("${config.sourceName}-labels", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            minZoom = 0f
            maxZoom = 32f
            
            setProperties(
                // 텍스트 필드 설정
                PropertyFactory.textField(
                    if (config.sourceName.contains("depth", ignoreCase = true)) {
                        // depth가 포함된 파일: 숫자로 처리하고 "m" 단위 추가
                        concat(
                            toString(round(toNumber(get(config.textField)))),
                            literal(" m")
                        )
                    } else {
                        // 일반 텍스트 필드: 그대로 표시
                        get(config.textField)
                    }
                ),
                PropertyFactory.textSize(
                    interpolate(
                        exponential(1.2f), zoom(),
                        stop(12, 10f), stop(16, 14f), stop(20, 18f)
                    )
                ),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textHaloColor(Color.WHITE),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textAllowOverlap(false),
                PropertyFactory.textIgnorePlacement(false)
            )
            // ✅ 레이어별 필터 분기
            if (isDepth) {
                // 숫자(0 초과)만 표시
                setFilter(all(
                    has(config.textField),
                    gt(toNumber(get(config.textField)), literal(0))
                ))
            } else {
                // 문자열(빈 값 제외) 표시
                setFilter(all(
                    has(config.textField),
                    neq(get(config.textField), literal(""))
                    // 또는: gt(length(get(config.textField)), literal(0))
                ))
            }
            // 0 또는 값 없음은 숨기기
//            setFilter(all(has(config.textField), gt(toNumber(get(config.textField)), literal(0))))
        }
        
        style.addLayer(symbolLayer)
        Log.d("[PMTilesLoader]", "심볼 레이어 추가됨: ${config.sourceName}-labels")
    }


    private fun addSymbolLayer(style: Style, config: PMTilesConfig, context: Context) {
        // 파일명에 따라 아이콘 결정
        val iconName = when {
            config.fileName.contains("lighthouse", ignoreCase = true) -> "lighthouse"
            config.fileName.contains("local", ignoreCase = true) -> "wreck_symbol"
            else -> "wreck_symbol" // 기본값
        }
        val iconId = "${iconName}-icon"
        
        // 1) drawable PNG를 스타일 이미지로 등록
        if (style.getImage(iconId) == null) {
            try {
                val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                if (resourceId != 0) {
                    val bmp = BitmapFactory.decodeResource(context.resources, resourceId)
                    style.addImage(iconId, bmp)
                    Log.d("[PMTilesLoader]", "아이콘 로드 완료: $iconName")
                } else {
                    Log.w("[PMTilesLoader]", "아이콘 리소스를 찾을 수 없음: $iconName")
                    return
                }
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "아이콘 로드 실패: $iconName, ${e.message}")
                return
            }
        }

        // 2) 해당 이미지를 쓰는 SymbolLayer 생성
        val layer = SymbolLayer("${config.sourceName}-symbols", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            minZoom = 16f
            maxZoom = 24f

            setProperties(
                iconImage(iconId),
                iconAllowOverlap(true),
                iconIgnorePlacement(false),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                // 확대할수록 살짝 키우기
                iconSize(
                    interpolate(
                        exponential(0.1f), zoom(),
                        stop( 0, 0.1f),
                        stop(12, 0.1f),
                        stop(16, 0.1f)
                    )
                )
            )
        }

        style.addLayer(layer)
        Log.d("[PMTilesLoader]", "심볼 레이어 추가: ${config.sourceName}-symbols ($iconName)")
    }

    /**
     * 기본 스타일을 로드하는 함수
     */
    private fun loadDefaultStyle(map: MapLibreMap) {
        val styleJson = """
        {
          "version": 8,
          "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",  // ✅ 추가
          "sources": {},
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "#FFFFFF" }
            }
          ]
        }
        """.trimIndent()
        map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
            Log.d("[PMTilesLoader]", "기본 스타일 로드 완료")
        }
    }
}

