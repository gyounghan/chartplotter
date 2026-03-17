package com.kumhomarine.chartplotter.alerts

import android.content.Context
import android.content.Intent

class AISAlertBroadcaster(
    private val context: Context
) {
    fun broadcast(payload: AISAlertPayload) {
        // ChartPlotter 인앱 배너 수신용
        context.sendBroadcast(createIntent(payload).setPackage("com.kumhomarine.chartplotter"))
        // Dashboard 인앱 배너 수신용
        context.sendBroadcast(createIntent(payload).setPackage("com.kumhomarine.dashboard"))
    }

    private fun createIntent(payload: AISAlertPayload): Intent {
        return Intent(AISAlertContract.ACTION_AIS_ALERT).apply {
            putExtra(AISAlertContract.EXTRA_TYPE, payload.type)
            putExtra(AISAlertContract.EXTRA_MMSI, payload.mmsi)
            putExtra(AISAlertContract.EXTRA_VESSEL_NAME, payload.vesselName)
            putExtra(AISAlertContract.EXTRA_CPA, payload.cpa)
            putExtra(AISAlertContract.EXTRA_TCPA, payload.tcpa)
            putExtra(AISAlertContract.EXTRA_TIMESTAMP, payload.timestamp)
            putExtra(AISAlertContract.EXTRA_MESSAGE, payload.message)
        }
    }
}

