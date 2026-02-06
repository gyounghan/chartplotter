package com.marineplay.chartplotter.domain.usecases

import com.marineplay.chartplotter.data.models.Route
import com.marineplay.chartplotter.data.models.RoutePoint
import com.marineplay.chartplotter.SavedPoint
import androidx.compose.ui.graphics.Color
import org.maplibre.android.geometry.LatLng
import kotlin.math.*

/**
 * 경로를 항해로 연결하는 UseCase
 * 1. 경로 밖에 있으면:
 *    - 현재 위치에서 첫 번째 점으로 가는 선이 경로 선분과 교차하면 교차점 이후부터 연결
 *    - 교차하지 않으면 첫 번째 점부터 연결
 * 2. 경로 안에 있으면 (점 사이 선분 위) 다음 가야 할 점부터 연결
 */
class ConnectRouteToNavigationUseCase(
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) {
    
    /**
     * 경로를 항해 경로로 변환
     * @param route 경로
     * @param currentLocation 현재 위치
     * @return 항해 경로 (waypoints, destination)
     */
    fun execute(
        route: Route,
        currentLocation: LatLng
    ): Pair<List<SavedPoint>, SavedPoint> {
        android.util.Log.d("[ConnectRouteToNavigation]", "execute 호출됨: route=${route.name}, 포인트 개수=${route.points.size}, currentLocation=(${currentLocation.latitude}, ${currentLocation.longitude})")
        if (route.points.isEmpty()) {
            throw IllegalArgumentException("경로에 포인트가 없습니다")
        }
        
        // 경로 포인트를 SavedPoint로 변환 (첫 번째 점부터 시작)
        val routeSavedPoints = route.points.mapIndexed { index, routePoint ->
            SavedPoint(
                name = routePoint.name.ifEmpty { "Waypoint ${index + 1}" },
                latitude = routePoint.latitude,
                longitude = routePoint.longitude,
                color = Color.Blue,
                iconType = "circle",
                timestamp = System.currentTimeMillis()
            )
        }
        
        // 디버깅: 경로 포인트 확인
        android.util.Log.d("[ConnectRouteToNavigation]", "경로 포인트 개수: ${routeSavedPoints.size}")
        if (routeSavedPoints.isNotEmpty()) {
            android.util.Log.d("[ConnectRouteToNavigation]", "첫 번째 점: ${routeSavedPoints.first().name} (${routeSavedPoints.first().latitude}, ${routeSavedPoints.first().longitude})")
        }
        
        // 현재 위치가 경로 선분 위에 있는지 확인
        val (closestSegmentIndex, distanceToSegment) = findClosestSegmentOnRoute(currentLocation, route.points)
        
        // 경로 안인지 판단 (가장 가까운 선분까지의 거리가 50m 이내면 경로 안)
        val isOnRoute = distanceToSegment <= 50.0
        
        return if (isOnRoute) {
            // 경로 안: 해당 선분의 끝점(다음 점)부터 연결
            val startIndex = closestSegmentIndex + 1 // 선분의 끝점 인덱스
            val remainingPoints = routeSavedPoints.drop(startIndex)
            if (remainingPoints.isEmpty()) {
                // 이미 경로 끝에 도달한 경우
                Pair(emptyList(), routeSavedPoints.last())
            } else {
                val waypoints = remainingPoints.dropLast(1)
                val destination = remainingPoints.last()
                Pair(waypoints, destination)
            }
        } else {
            // 경로 밖: 현재 위치에서 첫 번째 점으로 가는 선이 경로 선분과 교차하는지 확인
            val firstPoint = route.points.first()
            val firstPointLatLng = LatLng(firstPoint.latitude, firstPoint.longitude)
            android.util.Log.d("[ConnectRouteToNavigation]", "경로 밖 - 현재 위치에서 첫 번째 점(${firstPoint.name})으로 가는 선이 경로와 교차하는지 확인")
            
            val intersectionSegmentIndex = findIntersectionSegment(
                currentLocation,
                firstPointLatLng,
                route.points
            )
            
            if (intersectionSegmentIndex != null) {
                // 교차하는 선분이 있으면 교차점을 계산하고 그 점부터 경로를 이어붙임
                val segmentStart = LatLng(route.points[intersectionSegmentIndex].latitude, route.points[intersectionSegmentIndex].longitude)
                val segmentEnd = LatLng(route.points[intersectionSegmentIndex + 1].latitude, route.points[intersectionSegmentIndex + 1].longitude)
                
                // 교차점 좌표 계산
                val intersectionPoint = calculateIntersectionPoint(
                    currentLocation,
                    firstPointLatLng,
                    segmentStart,
                    segmentEnd
                )
                
                android.util.Log.d("[ConnectRouteToNavigation]", "교차 감지됨! 선분 인덱스: $intersectionSegmentIndex, 교차점: (${intersectionPoint.latitude}, ${intersectionPoint.longitude})")
                
                // 교차점 이후의 모든 경로 점들을 waypoint로 포함 (교차점이 있는 선분의 끝점부터)
                val startIndex = intersectionSegmentIndex + 1 // 교차점 이후의 점부터 시작
                val remainingPoints = routeSavedPoints.drop(startIndex)
                
                if (remainingPoints.isEmpty()) {
                    // 이미 경로 끝에 도달한 경우
                    android.util.Log.d("[ConnectRouteToNavigation]", "경로 끝에 도달")
                    // 교차점을 waypoint로 포함
                    val intersectionWaypoint = SavedPoint(
                        name = "교차점",
                        latitude = intersectionPoint.latitude,
                        longitude = intersectionPoint.longitude,
                        color = Color.Blue,
                        iconType = "circle",
                        timestamp = System.currentTimeMillis()
                    )
                    Pair(listOf(intersectionWaypoint), routeSavedPoints.last())
                } else {
                    // 교차점을 첫 번째 waypoint로 추가하고, 교차점 이후의 모든 점들을 포함
                    val intersectionWaypoint = SavedPoint(
                        name = "교차점",
                        latitude = intersectionPoint.latitude,
                        longitude = intersectionPoint.longitude,
                        color = Color.Blue,
                        iconType = "circle",
                        timestamp = System.currentTimeMillis()
                    )
                    val waypoints = listOf(intersectionWaypoint) + remainingPoints.dropLast(1)
                    val destination = remainingPoints.last()
                    android.util.Log.d("[ConnectRouteToNavigation]", "교차 후 - waypoints 개수: ${waypoints.size} (교차점 포함), 첫 번째 waypoint: ${waypoints.first().name}, destination: ${destination.name}")
                    Pair(waypoints, destination)
                }
            } else {
                android.util.Log.d("[ConnectRouteToNavigation]", "교차하지 않음 - 첫 번째 점부터 시작")
                // 교차하지 않으면 첫 번째 점부터 연결
                // waypoints에 첫 번째 점부터 포함 (마지막 점만 제외)
                // routeSavedPoints는 [첫 번째, 두 번째, ..., 마지막] 순서
                val waypoints = if (routeSavedPoints.size > 1) {
                    // 첫 번째 점부터 마지막-1까지 포함
                    val result = routeSavedPoints.take(routeSavedPoints.size - 1)
                    android.util.Log.d("[ConnectRouteToNavigation]", "경로 밖 - waypoints 개수: ${result.size}, 첫 번째 waypoint: ${result.firstOrNull()?.name}")
                    result
                } else {
                    emptyList()
                }
                val destination = routeSavedPoints.last()
                android.util.Log.d("[ConnectRouteToNavigation]", "경로 밖 - destination: ${destination.name}")
                Pair(waypoints, destination)
            }
        }
    }
    
    /**
     * 현재 위치가 경로 선분 위에 있는지 확인 (선분과의 최단 거리 계산)
     */
    private fun findClosestSegmentOnRoute(
        currentLocation: LatLng,
        routePoints: List<RoutePoint>
    ): Pair<Int, Double> {
        var minDistance = Double.MAX_VALUE
        var closestSegmentIndex = 0
        
        // 각 선분에 대해 최단 거리 계산
        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]
            val distance = distanceToSegment(
                currentLocation,
                LatLng(p1.latitude, p1.longitude),
                LatLng(p2.latitude, p2.longitude)
            )
            if (distance < minDistance) {
                minDistance = distance
                closestSegmentIndex = i
            }
        }
        
        return Pair(closestSegmentIndex, minDistance)
    }
    
    /**
     * 점에서 선분까지의 최단 거리 계산
     */
    private fun distanceToSegment(point: LatLng, segmentStart: LatLng, segmentEnd: LatLng): Double {
        val A = point.longitude - segmentStart.longitude
        val B = point.latitude - segmentStart.latitude
        val C = segmentEnd.longitude - segmentStart.longitude
        val D = segmentEnd.latitude - segmentStart.latitude
        
        val dot = A * C + B * D
        val lenSq = C * C + D * D
        val param = if (lenSq != 0.0) dot / lenSq else -1.0
        
        val xx: Double
        val yy: Double
        
        when {
            param < 0 -> {
                xx = segmentStart.longitude
                yy = segmentStart.latitude
            }
            param > 1 -> {
                xx = segmentEnd.longitude
                yy = segmentEnd.latitude
            }
            else -> {
                xx = segmentStart.longitude + param * C
                yy = segmentStart.latitude + param * D
            }
        }
        
        val dx = point.longitude - xx
        val dy = point.latitude - yy
        return sqrt(dx * dx + dy * dy) * 111000.0 // 대략적인 미터 변환
    }
    
    /**
     * 현재 위치에서 첫 번째 점으로 가는 선이 경로 선분과 교차하는지 확인
     * @param currentLocation 현재 위치
     * @param firstPoint 첫 번째 경로 점
     * @param routePoints 경로 포인트 리스트
     * @return 교차하는 선분의 인덱스 (교차하지 않으면 null)
     */
    private fun findIntersectionSegment(
        currentLocation: LatLng,
        firstPoint: LatLng,
        routePoints: List<RoutePoint>
    ): Int? {
        // 현재 위치에서 첫 번째 점까지의 선분
        val lineStart = currentLocation
        val lineEnd = firstPoint
        
        android.util.Log.d("[ConnectRouteToNavigation]", "교차 확인 시작: 현재 위치(${currentLocation.latitude}, ${currentLocation.longitude}) -> 첫 번째 점(${firstPoint.latitude}, ${firstPoint.longitude})")
        
        // 경로의 각 선분과 교차하는지 확인 (첫 번째 선분은 제외 - 첫 번째 점이 시작점이므로)
        // 첫 번째 선분(0번)은 현재 위치 -> 첫 번째 점으로 가는 선과 같은 선분이므로 제외
        for (i in 1 until routePoints.size - 1) {
            val segmentStart = LatLng(routePoints[i].latitude, routePoints[i].longitude)
            val segmentEnd = LatLng(routePoints[i + 1].latitude, routePoints[i + 1].longitude)
            
            android.util.Log.d("[ConnectRouteToNavigation]", "선분 $i 확인: (${segmentStart.latitude}, ${segmentStart.longitude}) -> (${segmentEnd.latitude}, ${segmentEnd.longitude})")
            
            if (doLinesIntersect(lineStart, lineEnd, segmentStart, segmentEnd)) {
                android.util.Log.d("[ConnectRouteToNavigation]", "교차 발견! 선분 인덱스: $i")
                return i // 교차하는 선분의 인덱스 반환
            }
        }
        
        android.util.Log.d("[ConnectRouteToNavigation]", "교차하는 선분 없음")
        return null // 교차하지 않음
    }
    
    /**
     * 두 선분이 교차하는지 확인
     * @param line1Start 첫 번째 선분의 시작점
     * @param line1End 첫 번째 선분의 끝점
     * @param line2Start 두 번째 선분의 시작점
     * @param line2End 두 번째 선분의 끝점
     * @return 교차하면 true
     */
    private fun doLinesIntersect(
        line1Start: LatLng,
        line1End: LatLng,
        line2Start: LatLng,
        line2End: LatLng
    ): Boolean {
        // 선분 교차 판정 알고리즘 (CCW - Counter Clockwise)
        val o1 = orientation(line1Start, line1End, line2Start)
        val o2 = orientation(line1Start, line1End, line2End)
        val o3 = orientation(line2Start, line2End, line1Start)
        val o4 = orientation(line2Start, line2End, line1End)
        
        // 일반적인 교차 케이스
        if (o1 != o2 && o3 != o4) {
            return true
        }
        
        // 특수 케이스: 한 선분의 끝점이 다른 선분 위에 있는 경우
        if (o1 == 0 && onSegment(line1Start, line2Start, line1End)) return true
        if (o2 == 0 && onSegment(line1Start, line2End, line1End)) return true
        if (o3 == 0 && onSegment(line2Start, line1Start, line2End)) return true
        if (o4 == 0 && onSegment(line2Start, line1End, line2End)) return true
        
        return false
    }
    
    /**
     * 세 점의 방향성 계산 (CCW)
     * @return 0: 일직선, 1: 시계방향, 2: 반시계방향
     */
    private fun orientation(p1: LatLng, p2: LatLng, p3: LatLng): Int {
        val val1 = (p2.latitude - p1.latitude) * (p3.longitude - p2.longitude)
        val val2 = (p2.longitude - p1.longitude) * (p3.latitude - p2.latitude)
        val result = val1 - val2
        
        return when {
            abs(result) < 1e-9 -> 0 // 일직선
            result > 0 -> 1 // 시계방향
            else -> 2 // 반시계방향
        }
    }
    
    /**
     * 점 q가 선분 pr 위에 있는지 확인
     */
    private fun onSegment(p: LatLng, q: LatLng, r: LatLng): Boolean {
        return (q.longitude <= max(p.longitude, r.longitude) && 
                q.longitude >= min(p.longitude, r.longitude) &&
                q.latitude <= max(p.latitude, r.latitude) && 
                q.latitude >= min(p.latitude, r.latitude))
    }
    
    /**
     * 두 선분의 교차점 좌표 계산
     * @param line1Start 첫 번째 선분의 시작점
     * @param line1End 첫 번째 선분의 끝점
     * @param line2Start 두 번째 선분의 시작점
     * @param line2End 두 번째 선분의 끝점
     * @return 교차점 좌표
     */
    private fun calculateIntersectionPoint(
        line1Start: LatLng,
        line1End: LatLng,
        line2Start: LatLng,
        line2End: LatLng
    ): LatLng {
        // 두 선분의 방정식: line1 = line1Start + t * (line1End - line1Start)
        //                   line2 = line2Start + s * (line2End - line2Start)
        
        val x1 = line1Start.longitude
        val y1 = line1Start.latitude
        val x2 = line1End.longitude
        val y2 = line1End.latitude
        val x3 = line2Start.longitude
        val y3 = line2Start.latitude
        val x4 = line2End.longitude
        val y4 = line2End.latitude
        
        val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        
        if (abs(denom) < 1e-9) {
            // 평행한 경우, 선분2의 시작점 반환
            return line2Start
        }
        
        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
        
        // 교차점 좌표 계산
        val intersectionLon = x1 + t * (x2 - x1)
        val intersectionLat = y1 + t * (y2 - y1)
        
        return LatLng(intersectionLat, intersectionLon)
    }
}
