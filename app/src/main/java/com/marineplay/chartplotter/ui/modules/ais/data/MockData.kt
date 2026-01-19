package com.marineplay.chartplotter.ui.modules.ais.data

import com.marineplay.chartplotter.ui.modules.ais.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AIS 더미 데이터 관리
 */
object MockData {
    private val _vessels = MutableStateFlow<List<AISVessel>>(generateMockVessels())
    val vessels: StateFlow<List<AISVessel>> = _vessels.asStateFlow()

    private val _events = MutableStateFlow<List<RiskEvent>>(generateMockEvents())
    val events: StateFlow<List<RiskEvent>> = _events.asStateFlow()

    private val _settings = MutableStateFlow<AISSettings>(AISSettings())
    val settings: StateFlow<AISSettings> = _settings.asStateFlow()

    fun toggleWatchlist(vesselId: String) {
        _vessels.value = _vessels.value.map { vessel ->
            if (vessel.id == vesselId) {
                vessel.copy(isWatchlisted = !vessel.isWatchlisted)
            } else {
                vessel
            }
        }
    }

    fun updateSettings(newSettings: AISSettings) {
        _settings.value = newSettings
    }

    private fun generateMockVessels(): List<AISVessel> {
        val now = System.currentTimeMillis()
        return listOf(
            AISVessel(
                id = "v1",
                name = "SUNRISE QUEEN",
                mmsi = "440123456",
                type = VesselType.CARGO,
                distance = 2.3,
                bearing = 45,
                speed = 12.5,
                course = 135,
                cpa = 0.3,
                tcpa = 8,
                riskLevel = RiskLevel.CRITICAL,
                isWatchlisted = false,
                lastUpdate = now
            ),
            AISVessel(
                id = "v2",
                name = "PACIFIC STAR",
                mmsi = "440234567",
                type = VesselType.TANKER,
                distance = 4.8,
                bearing = 315,
                speed = 10.2,
                course = 225,
                cpa = 0.8,
                tcpa = 18,
                riskLevel = RiskLevel.WARNING,
                isWatchlisted = true,
                lastUpdate = now
            ),
            AISVessel(
                id = "v3",
                name = "진해호",
                mmsi = "440345678",
                type = VesselType.FISHING,
                distance = 1.5,
                bearing = 90,
                speed = 3.5,
                course = 180,
                cpa = 0.2,
                tcpa = 6,
                riskLevel = RiskLevel.CRITICAL,
                isWatchlisted = true,
                lastUpdate = now
            ),
            AISVessel(
                id = "v4",
                name = "OCEAN PRINCESS",
                mmsi = "440456789",
                type = VesselType.PASSENGER,
                distance = 8.2,
                bearing = 270,
                speed = 18.0,
                course = 90,
                cpa = 3.5,
                tcpa = 45,
                riskLevel = RiskLevel.SAFE,
                isWatchlisted = false,
                lastUpdate = now
            ),
            AISVessel(
                id = "v5",
                name = "부산1호",
                mmsi = "440567890",
                type = VesselType.FISHING,
                distance = 3.1,
                bearing = 180,
                speed = 4.2,
                course = 270,
                cpa = 1.2,
                tcpa = 22,
                riskLevel = RiskLevel.WARNING,
                isWatchlisted = false,
                lastUpdate = now
            ),
            AISVessel(
                id = "v6",
                name = "NAVIGATOR EXPRESS",
                mmsi = "440678901",
                type = VesselType.CARGO,
                distance = 12.5,
                bearing = 30,
                speed = 14.8,
                course = 315,
                cpa = 5.2,
                tcpa = 68,
                riskLevel = RiskLevel.SAFE,
                isWatchlisted = false,
                lastUpdate = now
            ),
            AISVessel(
                id = "v7",
                name = "SEA BREEZE",
                mmsi = "440789012",
                type = VesselType.PLEASURE,
                distance = 6.3,
                bearing = 210,
                speed = 8.5,
                course = 45,
                cpa = 2.8,
                tcpa = 35,
                riskLevel = RiskLevel.SAFE,
                isWatchlisted = false,
                lastUpdate = now
            ),
            AISVessel(
                id = "v8",
                name = "GOLDEN WAVE",
                mmsi = "440890123",
                type = VesselType.TANKER,
                distance = 5.4,
                bearing = 120,
                speed = 11.3,
                course = 200,
                cpa = 1.5,
                tcpa = 25,
                riskLevel = RiskLevel.WARNING,
                isWatchlisted = false,
                lastUpdate = now
            ),
            AISVessel(
                id = "v9",
                name = "해운대호",
                mmsi = "440901234",
                type = VesselType.FISHING,
                distance = 2.8,
                bearing = 340,
                speed = 2.8,
                course = 160,
                cpa = 0.9,
                tcpa = 28,
                riskLevel = RiskLevel.WARNING,
                isWatchlisted = true,
                lastUpdate = now
            ),
            AISVessel(
                id = "v10",
                name = "ATLANTIC VOYAGER",
                mmsi = "441012345",
                type = VesselType.CARGO,
                distance = 15.2,
                bearing = 60,
                speed = 16.2,
                course = 280,
                cpa = 8.5,
                tcpa = 92,
                riskLevel = RiskLevel.SAFE,
                isWatchlisted = false,
                lastUpdate = now
            )
        )
    }

    private fun generateMockEvents(): List<RiskEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            RiskEvent(
                id = "e1",
                timestamp = now - 15 * 60 * 1000L,
                vesselId = "v1",
                vesselName = "SUNRISE QUEEN",
                cpa = 0.3,
                tcpa = 8,
                riskLevel = RiskLevel.CRITICAL,
                description = "우선수에서 고속 접근 중, 충돌 위험"
            ),
            RiskEvent(
                id = "e2",
                timestamp = now - 45 * 60 * 1000L,
                vesselId = "v3",
                vesselName = "진해호",
                cpa = 0.2,
                tcpa = 6,
                riskLevel = RiskLevel.CRITICAL,
                description = "우현에서 저속 접근, 조업 중으로 추정"
            ),
            RiskEvent(
                id = "e3",
                timestamp = now - 2 * 60 * 60 * 1000L,
                vesselId = "v2",
                vesselName = "PACIFIC STAR",
                cpa = 0.8,
                tcpa = 18,
                riskLevel = RiskLevel.WARNING,
                description = "좌선수에서 접근 중, 주의 필요"
            ),
            RiskEvent(
                id = "e4",
                timestamp = now - 4 * 60 * 60 * 1000L,
                vesselId = "v5",
                vesselName = "부산1호",
                cpa = 1.2,
                tcpa = 22,
                riskLevel = RiskLevel.WARNING,
                description = "정선미에서 접근, 침로 변경 권고"
            ),
            RiskEvent(
                id = "e5",
                timestamp = now - 6 * 60 * 60 * 1000L,
                vesselId = "v8",
                vesselName = "GOLDEN WAVE",
                cpa = 1.5,
                tcpa = 25,
                riskLevel = RiskLevel.WARNING,
                description = "우현에서 접근 중, 모니터링 필요"
            ),
            RiskEvent(
                id = "e6",
                timestamp = now - 12 * 60 * 60 * 1000L,
                vesselId = "v9",
                vesselName = "해운대호",
                cpa = 0.9,
                tcpa = 28,
                riskLevel = RiskLevel.WARNING,
                description = "좌현에서 접근 중, 어로 작업 중"
            )
        )
    }
}

