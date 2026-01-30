package com.marineplay.chartplotter.domain.usecases

import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor
import com.marineplay.chartplotter.data.models.SavedPoint
import com.marineplay.chartplotter.domain.repositories.PointRepository

/**
 * 포인트 업데이트 UseCase
 */
class UpdatePointUseCase(
    private val pointRepository: PointRepository
) {
    /**
     * 포인트를 업데이트합니다.
     * @param originalPoint 원본 포인트
     * @param newName 새로운 이름
     * @param newColor 새로운 색상 (Compose Color)
     * @return 업데이트 후 포인트 목록
     */
    fun execute(
        originalPoint: SavedPoint,
        newName: String,
        newColor: ComposeColor
    ): List<SavedPoint> {
        // Compose Color를 Int로 변환
        val colorInt = newColor.toArgb()
        return pointRepository.updateSavedPoint(originalPoint, newName, colorInt)
    }
}

