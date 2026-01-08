package com.marineplay.chartplotter.domain.usecases

import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor
import com.marineplay.chartplotter.helpers.PointHelper
import org.maplibre.android.geometry.LatLng

/**
 * 포인트 등록 UseCase
 */
class RegisterPointUseCase(
    private val pointHelper: PointHelper
) {
    /**
     * 포인트를 등록합니다.
     * @param latLng 포인트 위치
     * @param name 포인트 이름 (비어있으면 자동 생성)
     * @param color 포인트 색상 (Compose Color)
     * @param iconType 포인트 아이콘 타입
     * @return 등록된 포인트 목록
     */
    fun execute(
        latLng: LatLng,
        name: String,
        color: ComposeColor,
        iconType: String = "circle"
    ): List<PointHelper.SavedPoint> {
        val finalName = if (name.isBlank()) {
            // 자동 포인트명 생성
            val nextNumber = GetNextAvailablePointNumberUseCase(pointHelper).execute()
            "Point$nextNumber"
        } else {
            name
        }
        
        // Compose Color를 Android Color로 변환
        val androidColor = AndroidColor.valueOf(color.toArgb())
        
        return pointHelper.addPoint(
            name = finalName,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            color = androidColor,
            iconType = iconType
        )
    }
}

