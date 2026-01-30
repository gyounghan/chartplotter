package com.marineplay.chartplotter.domain.mappers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import com.marineplay.chartplotter.SavedPoint as UiSavedPoint
import com.marineplay.chartplotter.data.models.SavedPoint as DataSavedPoint

/**
 * DataSavedPoint와 UiSavedPoint 간의 타입 변환을 담당하는 Mapper
 */
object PointMapper {
    /**
     * DataSavedPoint를 UiSavedPoint로 변환
     */
    fun toUiPoint(dataPoint: DataSavedPoint): UiSavedPoint {
        return UiSavedPoint(
            name = dataPoint.name,
            latitude = dataPoint.latitude,
            longitude = dataPoint.longitude,
            color = Color(dataPoint.color), // dataPoint.color는 이미 Int 타입
            iconType = dataPoint.iconType,
            timestamp = dataPoint.timestamp
        )
    }
    
    /**
     * DataSavedPoint 리스트를 UiSavedPoint 리스트로 변환
     */
    fun toUiPoints(points: List<DataSavedPoint>): List<UiSavedPoint> {
        return points.map { toUiPoint(it) }
    }
    
    /**
     * UiSavedPoint를 DataSavedPoint로 변환
     */
    fun toDataPoint(uiPoint: UiSavedPoint): DataSavedPoint {
        return DataSavedPoint(
            name = uiPoint.name,
            latitude = uiPoint.latitude,
            longitude = uiPoint.longitude,
            color = uiPoint.color.toArgb(), // Int 타입으로 직접 사용
            iconType = uiPoint.iconType,
            timestamp = uiPoint.timestamp
        )
    }
    
    /**
     * UiSavedPoint 리스트를 DataSavedPoint 리스트로 변환
     */
    fun toDataPoints(points: List<UiSavedPoint>): List<DataSavedPoint> {
        return points.map { toDataPoint(it) }
    }
}

