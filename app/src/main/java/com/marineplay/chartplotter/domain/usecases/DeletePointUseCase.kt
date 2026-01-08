package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.helpers.PointHelper

/**
 * 포인트 삭제 UseCase
 */
class DeletePointUseCase(
    private val pointHelper: PointHelper
) {
    /**
     * 포인트를 삭제합니다.
     * @param point 삭제할 포인트
     * @return 삭제 후 포인트 목록
     */
    fun execute(point: PointHelper.SavedPoint): List<PointHelper.SavedPoint> {
        val existingPoints = pointHelper.loadPointsFromLocal().toMutableList()
        existingPoints.removeAll { it.timestamp == point.timestamp }
        pointHelper.savePointsToLocal(existingPoints)
        return existingPoints
    }
}

