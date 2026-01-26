package com.marineplay.chartplotter.data.datasources

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * AIS 데이터 소스 인터페이스
 */
interface AISDataSource {
    val isConnected: StateFlow<Boolean>
    val rawMessages: StateFlow<List<String>>
    
    fun setOnMessageReceived(callback: (String) -> Unit)
    fun connect(baudRate: Int): Boolean
    fun disconnect()
    fun cleanup()
}

/**
 * AIS 데이터 소스 구현체
 */
class AISDataSourceImpl(
    private val context: Context
) : AISDataSource {
    private val dataReceiver = com.marineplay.chartplotter.data.datasources.AISDataReceiver(context)
    
    override val isConnected: StateFlow<Boolean> = dataReceiver.isConnected
    override val rawMessages: StateFlow<List<String>> = dataReceiver.rawMessages
    
    override fun setOnMessageReceived(callback: (String) -> Unit) {
        dataReceiver.setOnMessageReceived(callback)
    }
    
    override fun connect(baudRate: Int): Boolean {
        return dataReceiver.connect(baudRate)
    }
    
    override fun disconnect() {
        dataReceiver.disconnect()
    }
    
    override fun cleanup() {
        dataReceiver.cleanup()
    }
}

