package com.marineplay.chartplotter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
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
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
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
                        LayerType.SYMBOL -> {
                            if (config.isDynamicSymbol) {
                                addDynamicSymbolLayer(style, config, context, config.iconMapping)
                            } else {
                                addSymbolLayer(style, config, context)
                            }
                        }
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
     * 비트맵에서 특정 색상을 투명하게 만드는 함수
     */
    private fun makeTransparent(bitmap: android.graphics.Bitmap, colorToReplace: Int): android.graphics.Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val transparentBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        
        // 픽셀별로 색상 확인하여 투명 처리
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            if (pixels[i] == colorToReplace) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            }
        }
        
        transparentBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return transparentBitmap
    }
    
    /**
     * 채우기 색상 표현식 생성 함수 (면의 내부 색상)
     */
    private fun createFillColorExpression(): Expression {
        val colorMapping = PMTilesManager.getBdrColorMapping()
        return match(
            toNumber(coalesce(get("BFR_COLOR"), get("COLOR"), get("LAYER"))),
            *colorMapping.entries.flatMap { 
                listOf(literal(it.key), color(it.value)) 
            }.toTypedArray(),
            color(Color.parseColor("#FFFFFFFF")) // 기본 색상: 흰색
        )
    }
    
    /**
     * 테두리 색상 표현식 생성 함수 (선의 색상)
     */
    private fun createBorderColorExpression(): Expression {
        val colorMapping = PMTilesManager.getBdrColorMapping()
        return match(
            toNumber(coalesce(get("BDR_COLOR"))),
            *colorMapping.entries.flatMap { 
                listOf(literal(it.key), color(it.value)) 
            }.toTypedArray(),
            color(Color.parseColor("#000000")) // 기본 색상: 검정
        )
    }
    
    /**
     * 선 레이어를 추가하는 함수
     */
    private fun addLineLayer(style: Style, config: PMTilesConfig) {
        val lineLayer = LineLayer("${config.sourceName}-lines", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            setMinZoom(0f)
            setMaxZoom(24f)
            
            // 테두리 색상 표현식 사용 (선의 색상)
            val colorExpr = createFillColorExpression()
            
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
            
            // 채우기 색상 표현식 사용 (면의 내부 색상)
            val colorExpr = createFillColorExpression()
            
            setProperties(
                PropertyFactory.fillColor(colorExpr),
                PropertyFactory.fillOpacity(0.6f)
            )
        }
        
        style.addLayer(fillLayer)
        
        // 면 경계선 레이어
        val lineLayer = LineLayer("${config.sourceName}-lines", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            
            // BDR_COLOR 매핑을 사용한 색상 표현식 생성
            val bdrColorMapping = PMTilesManager.getBdrColorMapping()
            val lineColorExpr = createBorderColorExpression()
            
            setProperties(
                PropertyFactory.lineColor(lineColorExpr),
                PropertyFactory.lineWidth(0.2f)
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
            minZoom = 7f
            maxZoom = 32f
            
            setProperties(
                // 텍스트 필드 설정
                PropertyFactory.textField(
                    if (config.sourceName.contains("ad", ignoreCase = true)) {
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
                PropertyFactory.textColor(
                    match(
                        toNumber(coalesce(get("COLOR"))),
                        *PMTilesManager.getBdrColorMapping().entries.flatMap { 
                            listOf(literal(it.key), color(it.value)) 
                        }.toTypedArray(),
                        color(Color.BLACK) // 기본 색상: 검정
                    )
                ),
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
        val iconName = config.textField
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
            minZoom = 13f
            maxZoom = 24f

            setProperties(
                iconImage(iconId),
                iconAllowOverlap(true),
                iconIgnorePlacement(false),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                // 확대할수록 살짝 키우기
                iconSize(
                    interpolate(
                        exponential(0.15f), zoom(),
                        stop(14, 0.15f),
                        stop(16, 0.3f)
                    )
                )
            )
        }

        style.addLayer(layer)
        Log.d("[PMTilesLoader]", "심볼 레이어 추가: ${config.sourceName}-symbols ($iconName)")
    }

    /**
     * ICON 속성에 따라 동적으로 심볼을 표시하는 레이어를 추가합니다.
     * @param style MapLibre 스타일
     * @param config PMTiles 설정
     * @param context 컨텍스트
     * @param iconMapping ICON 값과 drawable 리소스명의 매핑
     */
    private fun addDynamicSymbolLayer(
        style: Style, 
        config: PMTilesConfig, 
        context: Context,
        iconMapping: Map<String, String> = emptyMap()
    ) {
        // 기본 아이콘 매핑 (필요에 따라 수정)
        val defaultIconMapping = mapOf(
            "lighthouse" to "lighthouse_icon",
            "buoy" to "buoy_icon", 
            "beacon" to "beacon_icon",
            "light" to "light_icon",
            "marker" to "marker_icon"
        )
        
        val finalIconMapping = if (iconMapping.isEmpty()) defaultIconMapping else iconMapping
        
        // 1) 모든 아이콘을 스타일에 등록 (파일 확장자에 따라 다르게 처리)
        finalIconMapping.forEach { (iconValue, drawableName) ->
            val iconId = "${config.sourceName}-${iconValue}-icon"
            if (style.getImage(iconId) == null) {
                try {
                    val resourceId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                    if (resourceId != 0) {
                        val bitmap = when {
                            // BMP 파일인 경우 drawable을 직접 사용하고 흰색을 투명하게 처리
                            drawableName.endsWith(".bmp", ignoreCase = true) -> {
                                val drawable = context.resources.getDrawable(resourceId, null)
                                val originalBitmap = drawable.toBitmap()
                                
                                // 흰색을 투명하게 변환
                                val transparentBitmap = makeTransparent(originalBitmap, Color.WHITE)
                                transparentBitmap
                            }
                            // PNG, JPG 등 다른 이미지 파일인 경우 BitmapFactory로 변환
                            else -> {
                                BitmapFactory.decodeResource(context.resources, resourceId)
                            }
                        }
                        
                        if (bitmap != null) {
                            style.addImage(iconId, bitmap)
                            Log.d("[PMTilesLoader]", "동적 아이콘 로드 완료: $iconValue -> $drawableName (${if (drawableName.endsWith(".bmp", ignoreCase = true)) "BMP 직접 사용" else "BitmapFactory 변환"})")
                        } else {
                            Log.w("[PMTilesLoader]", "아이콘 비트맵 생성 실패: $drawableName")
                        }
                    } else {
                        Log.w("[PMTilesLoader]", "동적 아이콘 리소스를 찾을 수 없음: $drawableName")
                    }
                } catch (e: Exception) {
                    Log.e("[PMTilesLoader]", "동적 아이콘 로드 실패: $iconValue -> $drawableName, ${e.message}")
                }
            }
        }

        // 2) 동적 아이콘을 사용하는 SymbolLayer 생성
        val layer = SymbolLayer("${config.sourceName}-dynamic-symbols", config.sourceName).apply {
            setSourceLayer(config.sourceLayer)
            minZoom = 13f
            maxZoom = 24f

            setProperties(
                // ICON 속성값에 따라 동적으로 아이콘 선택
                iconImage(
                    match(
                        get("ICON"), // ICON 속성값을 가져옴
                        literal("default"), // 기본값
                        *finalIconMapping.map { (iconValue, _) ->
                            stop(iconValue, literal("${config.sourceName}-${iconValue}-icon"))
                        }.toTypedArray()
                    )
                ),
                iconAllowOverlap(true),
                iconIgnorePlacement(false),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                // 확대할수록 살짝 키우기
                iconSize(
                    interpolate(
                        exponential(1f), zoom(),
                        stop(14, 1f),
                        stop(16, 2f)
                    )
                )
            )
        }

        style.addLayer(layer)
        Log.d("[PMTilesLoader]", "동적 심볼 레이어 추가: ${config.sourceName}-dynamic-symbols")
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
    
    /**
     * 목적지 마커를 지도에 추가하는 함수
     */
    fun addDestinationMarkers(map: MapLibreMap, destinations: List<Destination>) {
        map.getStyle { style ->
            try {
                // 목적지가 없으면 마커 추가하지 않음
                if (destinations.isEmpty()) {
                    Log.d("[PMTilesLoader]", "목적지가 없어서 마커 추가하지 않음")
                    return@getStyle
                }
                
                // 기존 목적지 마커 제거
                if (style.getLayer("destination-layer") != null) {
                    style.removeLayer("destination-layer")
                }
                if (style.getSource("destination-source") != null) {
                    style.removeSource("destination-source")
                }
                
                Log.d("[PMTilesLoader]", "목적지 마커 추가 시작: ${destinations.size}개")
                
                // 목적지 마커 아이콘 추가
                val destinationIcon = createDestinationIcon()
                style.addImage("destination-marker", destinationIcon)
            
            // 목적지 GeoJSON 데이터 생성
            val features = destinations.map { destination ->
                """
                {
                    "type": "Feature",
                    "properties": {
                        "name": "${destination.name}",
                        "id": "${destination.name}"
                    },
                    "geometry": {
                        "type": "Point",
                        "coordinates": [${destination.longitude}, ${destination.latitude}]
                    }
                }
                """.trimIndent()
            }.joinToString(",", "[", "]")
            
            val geoJsonData = """
            {
                "type": "FeatureCollection",
                "features": $features
            }
            """.trimIndent()
            
            // GeoJSON 소스 추가
            style.addSource(GeoJsonSource("destination-source", geoJsonData))
            
            // 목적지 마커 레이어 추가
            style.addLayer(
                SymbolLayer("destination-layer", "destination-source")
                    .withProperties(
                        iconImage("destination-marker"),
                        iconSize(1.0f),
                        iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)
                    )
            )
            
            // 목적지 이름 레이어는 제거 (클릭 시에만 표시)
            
            Log.d("[PMTilesLoader]", "목적지 마커 ${destinations.size}개 추가 완료")
            
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "목적지 마커 추가 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 목적지 마커 아이콘 생성
     */
    private fun createDestinationIcon(): android.graphics.Bitmap {
        val size = 40
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // 외곽 원 (검은색)
        val outerPaint = android.graphics.Paint().apply {
            color = Color.BLACK
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, outerPaint)
        
        // 내부 원 (빨간색)
        val innerPaint = android.graphics.Paint().apply {
            color = Color.RED
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, innerPaint)
        
        // 중앙 점 (흰색)
        val centerPaint = android.graphics.Paint().apply {
            color = Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 4f, centerPaint)
        
        return bitmap
    }
    
    /**
     * 코스업 모드에서 목적지와 현재 위치를 연결하는 선을 그립니다
     */
    fun addCourseLine(map: MapLibreMap, currentLocation: LatLng, destination: LatLng) {
        map.getStyle { style ->
            try {
                // 기존 코스업 선 제거
                removeCourseLine(map)
                
                // GeoJSON LineString 생성
                val courseLineGeoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "LineString",
                                "coordinates": [
                                    [${currentLocation.longitude}, ${currentLocation.latitude}],
                                    [${destination.longitude}, ${destination.latitude}]
                                ]
                            },
                            "properties": {
                                "name": "course_line"
                            }
                        }
                    ]
                }
                """.trimIndent()
                
                // GeoJsonSource 추가
                val courseLineSource = GeoJsonSource("course_line_source", courseLineGeoJson)
                style.addSource(courseLineSource)
                
                // LineLayer 추가
                val courseLineLayer = LineLayer("course_line_layer", "course_line_source")
                    .withProperties(
                        PropertyFactory.lineColor(Color.BLACK),
                        PropertyFactory.lineWidth(1.5f),
                        PropertyFactory.lineOpacity(0.9f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                style.addLayer(courseLineLayer)
                
                Log.d("[PMTilesLoader]", "코스업 선 추가됨: ${currentLocation} -> ${destination}")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "코스업 선 추가 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 코스업 선을 제거합니다
     */
    fun removeCourseLine(map: MapLibreMap) {
        map.getStyle { style ->
            try {
                // 레이어 제거
                if (style.getLayer("course_line_layer") != null) {
                    style.removeLayer("course_line_layer")
                }
                
                // 소스 제거
                if (style.getSource("course_line_source") != null) {
                    style.removeSource("course_line_source")
                }
                
                Log.d("[PMTilesLoader]", "코스업 선 제거됨")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "코스업 선 제거 실패: ${e.message}")
            }
        }
    }
}

