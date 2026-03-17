package com.kumhomarine.chartplotter.alerts

object AISAlertContract {
    const val ACTION_AIS_ALERT = "com.kumhomarine.ais.ALERT"

    const val EXTRA_TYPE = "extra_type"
    const val EXTRA_MMSI = "extra_mmsi"
    const val EXTRA_VESSEL_NAME = "extra_vessel_name"
    const val EXTRA_CPA = "extra_cpa"
    const val EXTRA_TCPA = "extra_tcpa"
    const val EXTRA_TIMESTAMP = "extra_timestamp"
    const val EXTRA_MESSAGE = "extra_message"

    const val TYPE_CPA_WARNING = "CPA_WARNING"
    const val TYPE_GUARD_INTRUSION = "GUARD_INTRUSION"
    const val TYPE_FAVORITE = "FAVORITE"
}

data class AISAlertPayload(
    val type: String,
    val mmsi: String,
    val vesselName: String,
    val cpa: Double,
    val tcpa: Int,
    val timestamp: Long,
    val message: String
)

