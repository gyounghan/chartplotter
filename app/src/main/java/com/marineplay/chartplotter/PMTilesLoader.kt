package com.marineplay.chartplotter

import android.content.Context
import android.graphics.Bitmap
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
        val startTime = System.currentTimeMillis()
        val outDir = File(context.filesDir, "pmtiles").apply { mkdirs() }
        val out = File(outDir, outName)
        
        // 이미 파일이 존재하면 스킵
        if (out.exists() && out.length() > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.d("[PMTilesLoader]", "⏱️ [파일 스킵] $outName (이미 존재, ${out.length()} bytes) - ${elapsed}ms")
            return out
        }
        
        context.assets.open(assetPath).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d("[PMTilesLoader]", "⏱️ [파일 복사] $outName (${out.length()} bytes) - ${elapsed}ms")
        return out
    }
    
    /**
     * PMTiles 파일을 자동으로 로드하고 MapLibre에 적용하는 메인 함수
     */
    fun loadPMTilesFromAssets(context: Context, map: MapLibreMap) {
        val totalStartTime = System.currentTimeMillis()
        Log.d("[PMTilesLoader]", "🚀 [시작] loadPMTilesFromAssets")
        
        try {
            // assets 폴더에서 PMTiles 파일 목록 가져오기
            val listStartTime = System.currentTimeMillis()
            val pmtilesFiles = PMTilesManager.getPMTilesFilesFromAssets(context)
            val listElapsed = System.currentTimeMillis() - listStartTime
            Log.d("[PMTilesLoader]", "⏱️ [파일 목록 가져오기] ${pmtilesFiles.size}개 파일 발견 - ${listElapsed}ms")
            Log.d("[PMTilesLoader]", "📋 발견된 PMTiles 파일들: $pmtilesFiles")
            
            if (pmtilesFiles.isEmpty()) {
                Log.w("[PMTilesLoader]", "PMTiles 파일이 없습니다. 기본 스타일을 사용합니다.")
                val defaultStartTime = System.currentTimeMillis()
                loadDefaultStyle(map)
                val defaultElapsed = System.currentTimeMillis() - defaultStartTime
                Log.d("[PMTilesLoader]", "⏱️ [기본 스타일 로드] - ${defaultElapsed}ms")
                val totalElapsed = System.currentTimeMillis() - totalStartTime
                Log.d("[PMTilesLoader]", "✅ [완료] loadPMTilesFromAssets (총 ${totalElapsed}ms)")
                return
            }
            
            // PMTiles 파일들을 복사하고 설정 정보 수집
            val copyStartTime = System.currentTimeMillis()
            val pmtilesConfigs = mutableListOf<PMTilesConfig>()
            val copiedFiles = mutableListOf<File>()
            
            for ((index, fileName) in pmtilesFiles.withIndex()) {
                try {
                    val fileStartTime = System.currentTimeMillis()
                    val copiedFile = copyPmtilesFromAssets(context, "pmtiles/$fileName", fileName)
                    copiedFiles.add(copiedFile)
                    val fileCopyElapsed = System.currentTimeMillis() - fileStartTime
                    
                    // 설정 정보 찾기 (기존 설정 우선, 없으면 파일명 규칙으로 자동 생성)
                    val configStartTime = System.currentTimeMillis()
                    val config = PMTilesManager.findConfigByFileName(fileName)
                    val configElapsed = System.currentTimeMillis() - configStartTime
                    
                    if (config != null) {
                        pmtilesConfigs.add(config)
                        // 기존 설정인지 자동 생성인지 확인
                        val isAutoGenerated = PMTilesManager.pmtilesConfigs.none { it.fileName == fileName }
                        val configSource = if (isAutoGenerated) "자동 생성" else "기존 설정"
                        Log.d("[PMTilesLoader]", "📝 [${index + 1}/${pmtilesFiles.size}] $fileName ($configSource, 타입: ${config.layerType}) - 복사: ${fileCopyElapsed}ms, 설정: ${configElapsed}ms")
                    } else {
                        Log.w("[PMTilesLoader]", "⚠️ 설정 생성 실패: $fileName")
                    }
                } catch (e: Exception) {
                    Log.e("[PMTilesLoader]", "❌ 파일 복사 실패: $fileName, ${e.message}")
                }
            }
            
            val copyElapsed = System.currentTimeMillis() - copyStartTime
            Log.d("[PMTilesLoader]", "⏱️ [파일 복사 완료] 총 ${pmtilesFiles.size}개 파일 - ${copyElapsed}ms (평균: ${copyElapsed / pmtilesFiles.size}ms/파일)")
            
            if (pmtilesConfigs.isEmpty()) {
                Log.w("[PMTilesLoader]", "유효한 PMTiles 설정이 없습니다. 기본 스타일을 사용합니다.")
                val defaultStartTime = System.currentTimeMillis()
                loadDefaultStyle(map)
                val defaultElapsed = System.currentTimeMillis() - defaultStartTime
                Log.d("[PMTilesLoader]", "⏱️ [기본 스타일 로드] - ${defaultElapsed}ms")
                val totalElapsed = System.currentTimeMillis() - totalStartTime
                Log.d("[PMTilesLoader]", "✅ [완료] loadPMTilesFromAssets (총 ${totalElapsed}ms)")
                return
            }
            
            // MapLibre에 PMTiles 적용
            val applyStartTime = System.currentTimeMillis()
            applyPMTilesToMap(map, pmtilesConfigs, copiedFiles, context)
            val applyElapsed = System.currentTimeMillis() - applyStartTime
            Log.d("[PMTilesLoader]", "⏱️ [PMTiles 적용] - ${applyElapsed}ms")
            
            val totalElapsed = System.currentTimeMillis() - totalStartTime
            Log.d("[PMTilesLoader]", "✅ [완료] loadPMTilesFromAssets (총 ${totalElapsed}ms)")
            
        } catch (e: Exception) {
            val totalElapsed = System.currentTimeMillis() - totalStartTime
            Log.e("[PMTilesLoader]", "❌ [오류] PMTiles 로드 중 오류 (${totalElapsed}ms): ${e.message}")
            e.printStackTrace()
            val defaultStartTime = System.currentTimeMillis()
            loadDefaultStyle(map)
            val defaultElapsed = System.currentTimeMillis() - defaultStartTime
            Log.d("[PMTilesLoader]", "⏱️ [기본 스타일 로드] - ${defaultElapsed}ms")
        }
    }
    
    /**
     * PMTiles를 MapLibre에 적용하는 함수
     */
    private fun applyPMTilesToMap(map: MapLibreMap, configs: List<PMTilesConfig>, files: List<File>, context: Context) {
        val applyStartTime = System.currentTimeMillis()
        Log.d("[PMTilesLoader]", "🎨 [시작] applyPMTilesToMap (${configs.size}개 설정)")
        
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

        val setStyleStartTime = System.currentTimeMillis()
        map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
            val setStyleElapsed = System.currentTimeMillis() - setStyleStartTime
            Log.d("[PMTilesLoader]", "⏱️ [setStyle 콜백] - ${setStyleElapsed}ms")
            
            try {
                // 각 PMTiles 파일을 소스로 추가
                val sourceStartTime = System.currentTimeMillis()
                for ((index, i) in configs.indices.withIndex()) {
                    val config = configs[i]
                    val file = files[i]

                    if (file.exists()) {
                        val sourceAddStartTime = System.currentTimeMillis()
                        val pmtilesUrl = "pmtiles://file://${file.absolutePath}"
                        val source = VectorSource(config.sourceName, pmtilesUrl)
                        style.addSource(source)
                        val sourceAddElapsed = System.currentTimeMillis() - sourceAddStartTime
                        Log.d("[PMTilesLoader]", "📦 [${index + 1}/${configs.size}] 소스 추가: ${config.sourceName} - ${sourceAddElapsed}ms")
                    }
                }
                val sourceElapsed = System.currentTimeMillis() - sourceStartTime
                Log.d("[PMTilesLoader]", "⏱️ [소스 추가 완료] 총 ${configs.size}개 - ${sourceElapsed}ms (평균: ${sourceElapsed / configs.size}ms/소스)")

                // 각 설정에 따라 레이어 추가
                val layerStartTime = System.currentTimeMillis()
                for ((index, config) in configs.withIndex()) {
                    val layerAddStartTime = System.currentTimeMillis()
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
                    val layerAddElapsed = System.currentTimeMillis() - layerAddStartTime
                    Log.d("[PMTilesLoader]", "🎨 [${index + 1}/${configs.size}] 레이어 추가: ${config.sourceName} (${config.layerType}) - ${layerAddElapsed}ms")
                }
                val layerElapsed = System.currentTimeMillis() - layerStartTime
                Log.d("[PMTilesLoader]", "⏱️ [레이어 추가 완료] 총 ${configs.size}개 - ${layerElapsed}ms (평균: ${layerElapsed / configs.size}ms/레이어)")
                
                val callbackElapsed = System.currentTimeMillis() - setStyleStartTime
                Log.d("[PMTilesLoader]", "✅ [완료] applyPMTilesToMap 콜백 (${callbackElapsed}ms)")
                
            } catch (e: Exception) {
                val callbackElapsed = System.currentTimeMillis() - setStyleStartTime
                Log.e("[PMTilesLoader]", "❌ [오류] 레이어 추가 중 오류 (${callbackElapsed}ms): ${e.message}")
                e.printStackTrace()
            }
        }
        
        val applyElapsed = System.currentTimeMillis() - applyStartTime
        Log.d("[PMTilesLoader]", "⏱️ [applyPMTilesToMap 완료] - ${applyElapsed}ms (setStyle 호출까지)")
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
            
            // FONT 속성 기반 동적 텍스트 크기
            // FONT 형식: "24OB" (24 크기, 굵게), "110" (110 크기), "120B" (120 크기, 굵게)
            // 기본 크기 110을 12sp로 가정하고 비례 계산
            // toNumber()를 사용하여 문자열에서 숫자 부분만 추출 (예: "240B" -> 240)
            val baseSize = 10f

            setProperties(
                // 텍스트 필드 설정
                PropertyFactory.textField(
                    get(config.textField)
                ),
                PropertyFactory.textSize(
                    coalesce(
                    min(
                        max(
                           toNumber(get("FONT")), literal(baseSize)
                    ), literal(15)
                    )
                    )),
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
                PropertyFactory.textAllowOverlap(true), 
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
        val targetSizePx = 40
        // 1) drawable PNG를 스타일 이미지로 등록
        if (style.getImage(iconId) == null) {
            try {
                val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                if (resourceId != 0) {
                    val bmp = BitmapFactory.decodeResource(context.resources, resourceId)

                    // 🌟 비트맵을 targetSizePx로 리사이즈
                    val resizedBitmap = Bitmap.createScaledBitmap(
                        bmp,
                        targetSizePx,
                        targetSizePx,
                        true
                    )

                    style.addImage(iconId, resizedBitmap)
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
                        exponential(1f), zoom(),
                        stop(10, 0.8f),
                        stop(14, 1.0f),
                        stop(18, 1.8f)
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
        
        // 아이콘별 스케일 비율 저장용
        val iconScaleMap = mutableMapOf<String, Float>()
        val targetSizePx: Int = 40 // 목표 크기 (px)
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
                             // 아이콘 비트맵의 최대 변 기준으로 스케일 계산
                            val maxDim: Int = kotlin.math.max(bitmap.width, bitmap.height)
                            val scale: Float = targetSizePx.toFloat() / maxDim.toFloat()
                            iconScaleMap[iconId] = scale

                            val resizedBitmap = Bitmap.createScaledBitmap(
                                bitmap,
                                targetSizePx,
                                targetSizePx,
                                true // 부드럽게 스케일링
                            )

//                            style.addImage(iconId, bitmap, true)
                             style.addImage(iconId, resizedBitmap)
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
                    product(
                    interpolate(
                        exponential(1f), zoom(),
                        stop(10, 0.8f),
                        stop(14, 1.0f),
                        stop(18, 1.8f)
                    ),
                     match(
                         get("ICON"),
                         literal(1.0f), // 기본값
                         *finalIconMapping.map { (iconValue, _) ->
                             val iconId = "${config.sourceName}-${iconValue}-icon"
                             val scale = iconScaleMap[iconId] ?: 1.0f
                             stop(iconValue, literal(scale))
                         }.toTypedArray()
                     )
                    )

//                    interpolate( exponential(1f), zoom(), stop(14, 1f), stop(16, 2f) )
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
        val startTime = System.currentTimeMillis()
        Log.d("[PMTilesLoader]", "🎨 [시작] loadDefaultStyle")
        
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
        
        val setStyleStartTime = System.currentTimeMillis()
        map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
            val setStyleElapsed = System.currentTimeMillis() - setStyleStartTime
            val totalElapsed = System.currentTimeMillis() - startTime
            Log.d("[PMTilesLoader]", "⏱️ [setStyle 콜백] 기본 스타일 - ${setStyleElapsed}ms")
            Log.d("[PMTilesLoader]", "✅ [완료] loadDefaultStyle (총 ${totalElapsed}ms)")
        }
        
        val callElapsed = System.currentTimeMillis() - startTime
        Log.d("[PMTilesLoader]", "⏱️ [setStyle 호출] 기본 스타일 - ${callElapsed}ms")
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
     * 항해 모드에서 목적지와 현재 위치를 연결하는 선을 그립니다
     */
    fun addNavigationLine(map: MapLibreMap, currentLocation: LatLng, destination: LatLng) {
        addNavigationRoute(map, currentLocation, emptyList(), destination)
    }
    
    /**
     * 경유지를 포함한 항해 경로를 추가합니다
     * @param currentLocation 현재 위치
     * @param waypoints 경유지 리스트
     * @param destination 최종 목적지
     */
    fun addNavigationRoute(map: MapLibreMap, currentLocation: LatLng, waypoints: List<LatLng>, destination: LatLng) {
        map.getStyle { style ->
            try {
                // 기존 항해 선 및 화살표 제거
                removeNavigationLine(map)
                
                // 경로 점 리스트 생성: 현재 위치 -> 경유지들 -> 목적지
                val routePoints = mutableListOf<LatLng>()
                routePoints.add(currentLocation)
                routePoints.addAll(waypoints)
                routePoints.add(destination)
                
                // GeoJSON LineString 좌표 생성
                val coordinates = routePoints.map { "[${it.longitude}, ${it.latitude}]" }.joinToString(",\n                                    ")
                
                val navigationLineGeoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "LineString",
                                "coordinates": [
                                    $coordinates
                                ]
                            },
                            "properties": {
                                "name": "navigation_line"
                            }
                        }
                    ]
                }
                """.trimIndent()
                
                // GeoJsonSource 추가
                val navigationLineSource = GeoJsonSource("navigation_line_source", navigationLineGeoJson)
                style.addSource(navigationLineSource)
                
                // LineLayer 추가 (파란색으로 구분)
                val navigationLineLayer = LineLayer("navigation_line_layer", "navigation_line_source")
                    .withProperties(
                        PropertyFactory.lineColor(Color.BLUE),
                        PropertyFactory.lineWidth(2.0f),
                        PropertyFactory.lineOpacity(0.8f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                style.addLayer(navigationLineLayer)
                
                // 화살표 추가: 선을 따라 일정 간격으로 화살표 배치
                addNavigationArrows(style, routePoints)
                
                Log.d("[PMTilesLoader]", "항해 경로 추가됨: 현재 위치 -> ${waypoints.size}개 경유지 -> 목적지")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항해 경로 추가 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 항해 경로에 방향 화살표를 추가합니다
     */
    private fun addNavigationArrows(style: Style, routePoints: List<LatLng>) {
        try {
            // 화살표 아이콘 생성 (간단한 삼각형 화살표)
            val arrowBitmap = createArrowIcon()
            style.addImage("navigation_arrow", arrowBitmap)
            
            // 선을 따라 화살표 포인트 생성 (각 세그먼트의 중간 지점)
            val arrowFeatures = mutableListOf<org.json.JSONObject>()
            
            for (i in 0 until routePoints.size - 1) {
                val start = routePoints[i]
                val end = routePoints[i + 1]
                
                // 세그먼트의 중간 지점 계산
                val midLat = (start.latitude + end.latitude) / 2.0
                val midLon = (start.longitude + end.longitude) / 2.0
                
                // 방향(베어링) 계산
                val bearing = calculateBearing(start.latitude, start.longitude, end.latitude, end.longitude)
                
                // 화살표 피처 생성
                val arrowFeature = org.json.JSONObject().apply {
                    put("type", "Feature")
                    put("geometry", org.json.JSONObject().apply {
                        put("type", "Point")
                        put("coordinates", org.json.JSONArray(listOf(midLon, midLat)))
                    })
                    put("properties", org.json.JSONObject().apply {
                        put("bearing", bearing)
                    })
                }
                arrowFeatures.add(arrowFeature)
            }
            
            // 화살표 소스 생성
            val arrowFeaturesArray = org.json.JSONArray(arrowFeatures)
            val arrowGeoJson = org.json.JSONObject().apply {
                put("type", "FeatureCollection")
                put("features", arrowFeaturesArray)
            }
            
            // 기존 화살표 소스/레이어 제거
            if (style.getSource("navigation_arrow_source") != null) {
                style.removeSource("navigation_arrow_source")
            }
            if (style.getLayer("navigation_arrow_layer") != null) {
                style.removeLayer("navigation_arrow_layer")
            }
            
            // 화살표 소스 추가
            val arrowSource = GeoJsonSource("navigation_arrow_source", arrowGeoJson.toString())
            style.addSource(arrowSource)
            
            // 화살표 레이어 추가
            val arrowLayer = SymbolLayer("navigation_arrow_layer", "navigation_arrow_source")
                .withProperties(
                    PropertyFactory.iconImage("navigation_arrow"),
                    PropertyFactory.iconSize(0.8f),
                    PropertyFactory.iconRotate(get("bearing")), // 방향에 따라 회전
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
            style.addLayer(arrowLayer)
            
            Log.d("[PMTilesLoader]", "항해 경로 화살표 추가됨: ${arrowFeatures.size}개")
            
        } catch (e: Exception) {
            Log.e("[PMTilesLoader]", "화살표 추가 실패: ${e.message}")
        }
    }
    
    /**
     * 두 지점 간의 방향(베어링)을 계산합니다 (0~360도)
     */
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        val y = Math.sin(deltaLonRad) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)
        
        val bearingRad = Math.atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        
        return ((bearingDeg % 360) + 360) % 360
    }
    
    /**
     * 화살표 아이콘을 생성합니다
     */
    private fun createArrowIcon(): android.graphics.Bitmap {
        val size = 32
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            color = Color.BLUE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        
        // 위쪽을 향한 삼각형 화살표 그리기
        val path = android.graphics.Path()
        path.moveTo(size / 2f, 0f) // 위쪽 꼭짓점
        path.lineTo(0f, size.toFloat()) // 왼쪽 아래
        path.lineTo(size.toFloat(), size.toFloat()) // 오른쪽 아래
        path.close()
        
        canvas.drawPath(path, paint)
        
        return bitmap
    }
    
    /**
     * 항해 선을 제거합니다
     */
    fun removeNavigationLine(map: MapLibreMap) {
        map.getStyle { style ->
            try {
                // 기존 항해 선 레이어 제거
                if (style.getLayer("navigation_line_layer") != null) {
                    style.removeLayer("navigation_line_layer")
                }
                
                // 기존 항해 선 소스 제거
                if (style.getSource("navigation_line_source") != null) {
                    style.removeSource("navigation_line_source")
                }
                
                // 기존 화살표 레이어 제거
                if (style.getLayer("navigation_arrow_layer") != null) {
                    style.removeLayer("navigation_arrow_layer")
                }
                
                // 기존 화살표 소스 제거
                if (style.getSource("navigation_arrow_source") != null) {
                    style.removeSource("navigation_arrow_source")
                }
                
                Log.d("[PMTilesLoader]", "항해 선 및 화살표 제거됨")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항해 선 제거 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 항해 목적지 마커를 추가합니다
     */
    fun addNavigationMarker(map: MapLibreMap, location: LatLng, name: String) {
        map.getStyle { style ->
            try {
                // 기존 항해 마커 제거
                removeNavigationMarker(map)
                
                // GeoJSON Point 생성
                val navigationMarkerGeoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "Point",
                                "coordinates": [${location.longitude}, ${location.latitude}]
                            },
                            "properties": {
                                "name": "$name",
                                "type": "navigation_marker"
                            }
                        }
                    ]
                }
                """.trimIndent()
                
                // GeoJsonSource 추가
                val navigationMarkerSource = GeoJsonSource("navigation_marker_source", navigationMarkerGeoJson)
                style.addSource(navigationMarkerSource)
                
                // CircleLayer 추가 (파란색 원)
                val navigationMarkerLayer = CircleLayer("navigation_marker_layer", "navigation_marker_source")
                    .withProperties(
                        PropertyFactory.circleColor(Color.BLUE),
                        PropertyFactory.circleRadius(8.0f),
                        PropertyFactory.circleOpacity(0.8f),
                        PropertyFactory.circleStrokeColor(Color.WHITE),
                        PropertyFactory.circleStrokeWidth(2.0f)
                    )
                style.addLayer(navigationMarkerLayer)
                
                Log.d("[PMTilesLoader]", "항해 마커 추가됨: $name at $location")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항해 마커 추가 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 항해 마커를 제거합니다
     */
    fun removeNavigationMarker(map: MapLibreMap) {
        map.getStyle { style ->
            try {
                // 기존 항해 마커 레이어 제거
                if (style.getLayer("navigation_marker_layer") != null) {
                    style.removeLayer("navigation_marker_layer")
                }
                
                // 기존 항해 마커 소스 제거
                if (style.getSource("navigation_marker_source") != null) {
                    style.removeSource("navigation_marker_source")
                }
                
                Log.d("[PMTilesLoader]", "항해 마커 제거됨")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항해 마커 제거 실패: ${e.message}")
            }
        }
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
    
    /**
     * 항적 점 마커를 추가합니다 (점이 1개일 때 사용)
     */
    fun addTrackPointMarker(map: MapLibreMap, sourceId: String, point: LatLng, color: androidx.compose.ui.graphics.Color) {
        map.getStyle { style ->
            try {
                // 마커용 별도 소스 ID (선과 구분)
                val markerSourceId = "${sourceId}_marker"
                val layerId = "${sourceId}_marker_layer"
                
                // GeoJSON Point 생성
                val feature = org.json.JSONObject().apply {
                    put("type", "Feature")
                    put("geometry", org.json.JSONObject().apply {
                        put("type", "Point")
                        put("coordinates", org.json.JSONArray(listOf(point.longitude, point.latitude)))
                    })
                }
                val featureCollection = org.json.JSONObject().apply {
                    put("type", "FeatureCollection")
                    put("features", org.json.JSONArray(listOf(feature)))
                }
                
                // 기존 레이어 제거 (소스는 유지하고 데이터만 업데이트)
                try {
                    if (style.getLayer(layerId) != null) {
                        style.removeLayer(layerId)
                    }
                } catch (e: Exception) {
                    Log.w("[PMTilesLoader]", "마커 레이어 제거 실패: $layerId, ${e.message}")
                }
                
                // 기존 소스가 있으면 데이터만 업데이트, 없으면 새로 추가
                val existingSource = style.getSource(markerSourceId)
                if (existingSource != null) {
                    try {
                        val geoJsonSource = existingSource as? GeoJsonSource
                        geoJsonSource?.setGeoJson(featureCollection.toString())
                    } catch (e: Exception) {
                        Log.w("[PMTilesLoader]", "마커 소스 업데이트 실패, 재생성 시도: ${e.message}")
                        try {
                            style.removeSource(markerSourceId)
                            val newSource = GeoJsonSource(markerSourceId, featureCollection.toString())
                            style.addSource(newSource)
                        } catch (e2: Exception) {
                            Log.e("[PMTilesLoader]", "마커 소스 재생성 실패: ${e2.message}")
                            return@getStyle
                        }
                    }
                } else {
                    try {
                        val source = GeoJsonSource(markerSourceId, featureCollection.toString())
                        style.addSource(source)
                    } catch (e: Exception) {
                        Log.e("[PMTilesLoader]", "마커 소스 추가 실패: $markerSourceId, ${e.message}")
                        return@getStyle
                    }
                }
                
                // CircleLayer로 점 마커 추가 (선 색과 동일한 색, 선 굵기보다 아주 조금만 크게)
                val markerLayer = CircleLayer(layerId, markerSourceId)
                    .withProperties(
                        PropertyFactory.circleColor(android.graphics.Color.rgb(
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        )),
                        PropertyFactory.circleRadius(0.8f), // 선 굵기(1.5f)보다 아주 조금만 크게
                        PropertyFactory.circleOpacity(1.0f),
                        PropertyFactory.circleStrokeColor(android.graphics.Color.rgb(
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        )),
                        PropertyFactory.circleStrokeWidth(0.5f) // 테두리도 얇게
                    )
                
                try {
                    style.addLayer(markerLayer)
                } catch (e: Exception) {
                    Log.e("[PMTilesLoader]", "마커 레이어 추가 실패: ${e.message}")
                    return@getStyle
                }
                
                Log.d("[PMTilesLoader]", "항적 점 마커 추가됨: $markerSourceId")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항적 점 마커 추가 실패: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 항적 선을 추가합니다 (각 점마다 마커도 함께 표시)
     */
    fun addTrackLine(map: MapLibreMap, sourceId: String, points: List<LatLng>, color: androidx.compose.ui.graphics.Color, isHighlighted: Boolean = false) {
        if (points.isEmpty()) return
        
        map.getStyle { style ->
            try {
                // 선용 GeoJSON (점이 2개 이상일 때만)
                val lineFeatureCollection = if (points.size >= 2) {
                val coordinates = points.map { listOf(it.longitude, it.latitude) }
                val lineString = org.json.JSONObject().apply {
                    put("type", "LineString")
                    put("coordinates", org.json.JSONArray(coordinates))
                }
                val feature = org.json.JSONObject().apply {
                    put("type", "Feature")
                    put("geometry", lineString)
                }
                    org.json.JSONObject().apply {
                    put("type", "FeatureCollection")
                    put("features", org.json.JSONArray(listOf(feature)))
                }
                } else null
                
                // 점 마커용 GeoJSON (모든 점마다)
                val pointFeatures = points.map { point ->
                    org.json.JSONObject().apply {
                        put("type", "Feature")
                        put("geometry", org.json.JSONObject().apply {
                            put("type", "Point")
                            put("coordinates", org.json.JSONArray(listOf(point.longitude, point.latitude)))
                        })
                    }
                }
                val pointFeatureCollection = org.json.JSONObject().apply {
                    put("type", "FeatureCollection")
                    put("features", org.json.JSONArray(pointFeatures))
                }
                
                // 점 마커용 소스 ID
                val pointSourceId = "${sourceId}_points"
                val pointLayerId = "${sourceId}_points_layer"
                
                // 기존 소스와 레이어를 안전하게 제거
                try {
                    // 선 소스 제거
                    val existingLineSource = style.getSource(sourceId)
                    if (existingLineSource != null) {
                        val bgLayerId = "${sourceId}_bg_layer"
                        val layerId = "${sourceId}_layer"
                        try {
                            if (style.getLayer(bgLayerId) != null) {
                                style.removeLayer(bgLayerId)
                            }
                        } catch (e: Exception) {
                            Log.w("[PMTilesLoader]", "배경 레이어 제거 실패: $bgLayerId, ${e.message}")
                        }
                        try {
                            if (style.getLayer(layerId) != null) {
                                style.removeLayer(layerId)
                            }
                        } catch (e: Exception) {
                            Log.w("[PMTilesLoader]", "레이어 제거 실패: $layerId, ${e.message}")
                        }
                    style.removeSource(sourceId)
                }
                    
                    // 점 마커 소스 제거
                    val existingPointSource = style.getSource(pointSourceId)
                    if (existingPointSource != null) {
                        try {
                            if (style.getLayer(pointLayerId) != null) {
                                style.removeLayer(pointLayerId)
                            }
                        } catch (e: Exception) {
                            Log.w("[PMTilesLoader]", "점 마커 레이어 제거 실패: $pointLayerId, ${e.message}")
                        }
                        style.removeSource(pointSourceId)
                    }
                } catch (e: Exception) {
                    Log.w("[PMTilesLoader]", "기존 소스 제거 실패: $sourceId, ${e.message}")
                }
                
                // 선 추가 (점이 2개 이상일 때만)
                if (lineFeatureCollection != null) {
                    val lineSource = GeoJsonSource(sourceId, lineFeatureCollection.toString())
                    try {
                        style.addSource(lineSource)
                    } catch (e: Exception) {
                        if (e.message?.contains("already exists") == true) {
                            try {
                                val existingSource = style.getSource(sourceId) as? GeoJsonSource
                                existingSource?.setGeoJson(lineFeatureCollection.toString())
                            } catch (updateException: Exception) {
                                Log.e("[PMTilesLoader]", "선 소스 데이터 업데이트 실패: $sourceId, ${updateException.message}")
                                return@getStyle
                            }
                        } else {
                            Log.e("[PMTilesLoader]", "선 소스 추가 실패: $sourceId, ${e.message}")
                            return@getStyle
                        }
                    }
                    
                    // 하이라이트 여부에 따라 효과 조정 (굵기는 유지, 하이라이트 느낌만)
                    val lineWidth = 1.5f // 굵기는 항상 동일
                
                    // 하이라이트된 경우 흰색 테두리 효과 (글로우 효과)
                if (isHighlighted) {
                        // 배경 레이어 (흰색, 약간 더 두껍게 - 글로우 효과)
                    val backgroundLayer = LineLayer("${sourceId}_bg_layer", sourceId)
                        .withProperties(
                            PropertyFactory.lineColor(android.graphics.Color.WHITE),
                                PropertyFactory.lineWidth(lineWidth + 2.0f), // 테두리 효과
                                PropertyFactory.lineOpacity(0.7f), // 반투명 흰색 테두리
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    style.addLayer(backgroundLayer)
                }
                
                    // 메인 선 레이어 (하이라이트 시 색상을 더 밝게)
                    val baseColor = android.graphics.Color.rgb(
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                    )
                    
                    // 하이라이트 시 색상을 더 밝게 (RGB 값을 증가)
                    val lineColor = if (isHighlighted) {
                        android.graphics.Color.rgb(
                            ((color.red * 255).toInt() + 50).coerceAtMost(255),
                            ((color.green * 255).toInt() + 50).coerceAtMost(255),
                            ((color.blue * 255).toInt() + 50).coerceAtMost(255)
                        )
                    } else {
                        baseColor
                    }
                    
                    val trackLineLayer = LineLayer("${sourceId}_layer", sourceId)
                        .withProperties(
                            PropertyFactory.lineColor(lineColor),
                            PropertyFactory.lineWidth(lineWidth), // 굵기는 항상 동일
                            PropertyFactory.lineOpacity(if (isHighlighted) 1.0f else 0.8f), // 하이라이트 시 더 선명하게
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                style.addLayer(trackLineLayer)
                }
                
                // 점 마커 추가 (모든 점마다)
                val pointSource = GeoJsonSource(pointSourceId, pointFeatureCollection.toString())
                try {
                    style.addSource(pointSource)
                } catch (e: Exception) {
                    if (e.message?.contains("already exists") == true) {
                        try {
                            val existingSource = style.getSource(pointSourceId) as? GeoJsonSource
                            existingSource?.setGeoJson(pointFeatureCollection.toString())
                        } catch (updateException: Exception) {
                            Log.e("[PMTilesLoader]", "점 마커 소스 데이터 업데이트 실패: $pointSourceId, ${updateException.message}")
                            return@getStyle
                        }
                    } else {
                        Log.e("[PMTilesLoader]", "점 마커 소스 추가 실패: $pointSourceId, ${e.message}")
                        return@getStyle
                    }
                }
                
                // 점 마커 레이어 추가 (하이라이트 여부에 따라 색상 조정)
                val pointBaseColor = android.graphics.Color.rgb(
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
                
                // 하이라이트 시 점 마커도 더 밝게
                val pointColor = if (isHighlighted) {
                    android.graphics.Color.rgb(
                        ((color.red * 255).toInt() + 50).coerceAtMost(255),
                        ((color.green * 255).toInt() + 50).coerceAtMost(255),
                        ((color.blue * 255).toInt() + 50).coerceAtMost(255)
                    )
                } else {
                    pointBaseColor
                }
                
                // 하이라이트 시 점 마커에 흰색 테두리 효과
                if (isHighlighted) {
                    val pointBackgroundLayer = CircleLayer("${pointLayerId}_bg", pointSourceId)
                        .withProperties(
                            PropertyFactory.circleColor(android.graphics.Color.WHITE),
                            PropertyFactory.circleRadius(0.8f + 1.0f), // 테두리 효과
                            PropertyFactory.circleOpacity(0.6f),
                            PropertyFactory.circleStrokeWidth(0f)
                        )
                    style.addLayer(pointBackgroundLayer)
                }
                
                val pointMarkerLayer = CircleLayer(pointLayerId, pointSourceId)
                    .withProperties(
                        PropertyFactory.circleColor(pointColor),
                        PropertyFactory.circleRadius(0.8f), // 선 굵기(1.5f)보다 아주 조금만 크게
                        PropertyFactory.circleOpacity(if (isHighlighted) 1.0f else 1.0f),
                        PropertyFactory.circleStrokeColor(android.graphics.Color.rgb(
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        )),
                        PropertyFactory.circleStrokeWidth(0.5f) // 테두리도 얇게
                    )
                style.addLayer(pointMarkerLayer)
                
                Log.d("[PMTilesLoader]", "항적 선 및 점 마커 추가됨: $sourceId (${points.size}개 점, 하이라이트: $isHighlighted)")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항적 선 추가 실패: ${e.message}")
            }
        }
    }
    
    /**
     * 모든 항적 선을 제거합니다
     */
    fun removeAllTracks(map: MapLibreMap) {
        map.getStyle { style ->
            try {
                // 모든 항적 관련 레이어와 소스 제거
                val layersToRemove = mutableListOf<String>()
                val sourcesToRemove = mutableListOf<String>()
                
                style.layers.forEach { layer ->
                    val layerId = layer.id
                    if (layerId.contains("track_") || layerId == "current_track_layer" || 
                        layerId.startsWith("current_track_") || layerId.contains("_bg_layer") ||
                        layerId.contains("_marker_layer")) {
                        layersToRemove.add(layerId)
                    }
                }
                
                style.sources.forEach { source ->
                    val sourceId = source.id
                    if (sourceId.contains("track_") || sourceId == "current_track" || 
                        sourceId.startsWith("current_track_") || sourceId.contains("_marker") ||
                        sourceId.contains("_points")) {
                        sourcesToRemove.add(sourceId)
                    }
                }
                
                layersToRemove.forEach { layerId ->
                    try {
                        if (style.getLayer(layerId) != null) {
                            style.removeLayer(layerId)
                        }
                    } catch (e: Exception) {
                        Log.e("[PMTilesLoader]", "레이어 제거 실패: $layerId, ${e.message}")
                    }
                }
                
                sourcesToRemove.forEach { sourceId ->
                    try {
                        if (style.getSource(sourceId) != null) {
                            style.removeSource(sourceId)
                        }
                    } catch (e: Exception) {
                        Log.e("[PMTilesLoader]", "소스 제거 실패: $sourceId, ${e.message}")
                    }
                }
                
                Log.d("[PMTilesLoader]", "모든 항적 제거됨")
                
            } catch (e: Exception) {
                Log.e("[PMTilesLoader]", "항적 제거 실패: ${e.message}")
            }
        }
    }
}

