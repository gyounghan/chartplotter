package com.marineplay.chartplotter.domain.usecases

import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor
import com.marineplay.chartplotter.helpers.PointHelper

/**
 * 포인트 업데이트 UseCase
 */
class UpdatePointUseCase(
    private val pointHelper: PointHelper
) {
    /**
     * 포인트를 업데이트합니다.
     * @param originalPoint 원본 포인트
     * @param newName 새로운 이름
     * @param newColor 새로운 색상 (Compose Color)
     * @return 업데이트 후 포인트 목록
     */
    fun execute(
        originalPoint: PointHelper.SavedPoint,
        newName: String,
        newColor: ComposeColor
    ): List<PointHelper.SavedPoint> {
        val existingPoints = pointHelper.loadPointsFromLocal().toMutableList()
        val pointIndex = existingPoints.indexOfFirst { it.timestamp == originalPoint.timestamp }
        
        if (pointIndex != -1) {
            // Compose Color를 Android Color로 변환
            val androidColor = AndroidColor.valueOf(newColor.toArgb())
            val updatedPoint = originalPoint.copy(
                name = newName,
                color = androidColor
            )
            existingPoints[pointIndex] = updatedPoint
            pointHelper.savePointsToLocal(existingPoints)
        }
        
        return existingPoints
    }
}

