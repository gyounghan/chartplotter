package com.kumhomarine.chartplotter.domain.usecases

import com.kumhomarine.chartplotter.data.models.SavedPoint
import com.kumhomarine.chartplotter.domain.repositories.PointRepository

/**
 * 포인트 삭제 UseCase
 */
class DeletePointUseCase(
    private val pointRepository: PointRepository
) {
    /**
     * 포인트를 삭제합니다.
     * @param point 삭제할 포인트
     * @return 삭제 후 포인트 목록
     */
    fun execute(point: SavedPoint): List<SavedPoint> {
        return pointRepository.deleteSavedPoint(point)
    }
}

