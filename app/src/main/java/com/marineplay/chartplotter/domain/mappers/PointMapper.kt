package com.marineplay.chartplotter.domain.mappers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import com.marineplay.chartplotter.SavedPoint
import com.marineplay.chartplotter.helpers.PointHelper

/**
 * PointHelper.SavedPoint와 SavedPoint 간의 타입 변환을 담당하는 Mapper
 */
object PointMapper {
    /**
     * PointHelper.SavedPoint를 SavedPoint로 변환
     */
    fun toUiPoint(pointHelperPoint: PointHelper.SavedPoint): SavedPoint {
        return SavedPoint(
            name = pointHelperPoint.name,
            latitude = pointHelperPoint.latitude,
            longitude = pointHelperPoint.longitude,
            color = Color(pointHelperPoint.color.toArgb()),
            iconType = pointHelperPoint.iconType,
            timestamp = pointHelperPoint.timestamp
        )
    }
    
    /**
     * PointHelper.SavedPoint 리스트를 SavedPoint 리스트로 변환
     */
    fun toUiPoints(points: List<PointHelper.SavedPoint>): List<SavedPoint> {
        return points.map { toUiPoint(it) }
    }
    
    /**
     * SavedPoint를 PointHelper.SavedPoint로 변환
     */
    fun toHelperPoint(uiPoint: SavedPoint): PointHelper.SavedPoint {
        return PointHelper.SavedPoint(
            name = uiPoint.name,
            latitude = uiPoint.latitude,
            longitude = uiPoint.longitude,
            color = AndroidColor.valueOf(uiPoint.color.toArgb()),
            iconType = uiPoint.iconType,
            timestamp = uiPoint.timestamp
        )
    }
    
    /**
     * SavedPoint 리스트를 PointHelper.SavedPoint 리스트로 변환
     */
    fun toHelperPoints(points: List<SavedPoint>): List<PointHelper.SavedPoint> {
        return points.map { toHelperPoint(it) }
    }
}

