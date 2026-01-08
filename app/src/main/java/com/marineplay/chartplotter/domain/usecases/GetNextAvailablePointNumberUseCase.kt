package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.helpers.PointHelper

/**
 * 다음 사용 가능한 포인트 번호 가져오기 UseCase
 */
class GetNextAvailablePointNumberUseCase(
    private val pointHelper: PointHelper
) {
    /**
     * 다음 사용 가능한 포인트 번호를 반환합니다.
     * "Point123" 형태의 이름에서 숫자 부분을 추출하여 사용되지 않은 최소 번호를 찾습니다.
     * @return 다음 사용 가능한 포인트 번호
     */
    fun execute(): Int {
        val existingPoints = pointHelper.loadPointsFromLocal()
        val usedNumbers = existingPoints.mapNotNull { point ->
            // "Point123" 형태에서 숫자 부분만 추출
            val matchResult = Regex("Point(\\d+)").find(point.name)
            matchResult?.groupValues?.get(1)?.toIntOrNull()
        }.toSet()

        // 1부터 시작해서 사용되지 않은 첫 번째 번호 찾기
        var nextNumber = 1
        while (usedNumbers.contains(nextNumber)) {
            nextNumber++
        }
        return nextNumber
    }
}

