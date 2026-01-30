package com.marineplay.chartplotter.data.datasources

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * USB 시리얼 통신을 통한 AIS 데이터 수신 클래스
 * Data 레이어에 위치하여 외부 데이터 소스와 통신
 */
class AISDataReceiver(private val context: Context) {
    
    private var usbPort: UsbSerialPort? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _rawMessages = MutableStateFlow<List<String>>(emptyList())
    val rawMessages: StateFlow<List<String>> = _rawMessages.asStateFlow()
    
    private var onMessageReceived: ((String) -> Unit)? = null
    
    // 완전한 NMEA 메시지 정규식: !AIVDM 또는 !AIVDO로 시작하고 체크섬으로 끝나는 메시지
    private val nmeaMessagePattern = Pattern.compile("^!AIVD[MO],.*\\*[0-9A-Fa-f]{2}\\r?\\n?$")
    
    /**
     * 메시지 수신 콜백 설정
     */
    fun setOnMessageReceived(callback: (String) -> Unit) {
        onMessageReceived = callback
    }
    
    /**
     * USB 디바이스 연결 시도
     * @param baudRate 보드레이트 (기본값: 38400)
     */
    fun connect(baudRate: Int = 38400): Boolean {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            
            if (availableDrivers.isEmpty()) {
                return false
            }
            
            // 첫 번째 사용 가능한 드라이버 사용
            val driver = availableDrivers[0]
            val connection = usbManager.openDevice(driver.device)
            
            if (connection == null) {
                return false
            }
            
            usbPort = driver.ports[0]
            usbPort?.open(connection)
            usbPort?.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            
            _isConnected.value = true
            
            // 데이터 수신 시작
            startReceiving()
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _isConnected.value = false
            return false
        }
    }
    
    /**
     * USB 연결 해제
     */
    fun disconnect() {
        receiveJob?.cancel()
        try {
            usbPort?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        usbPort = null
        _isConnected.value = false
    }
    
    /**
     * 데이터 수신 시작
     */
    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val buffer = ByteArray(1024)
            var messageBuffer = StringBuilder()
            
            while (isActive && _isConnected.value) {
                try {
                    val bytesRead = usbPort?.read(buffer, 1000) ?: 0
                    
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                        messageBuffer.append(data)
                        
                        // NMEA 메시지 파싱 (줄바꿈으로 구분)
                        val lines = messageBuffer.toString().split("\n", "\r\n")
                        
                        // 마지막 줄은 완전하지 않을 수 있으므로 버퍼에 유지
                        messageBuffer.clear()
                        if (lines.isNotEmpty() && !data.endsWith("\n") && !data.endsWith("\r\n")) {
                            messageBuffer.append(lines.last())
                            lines.dropLast(1)
                        }
                        
                        // 완전한 메시지 처리 (정규식으로 검증)
                        lines.filter { line ->
                            val trimmed = line.trim()
                            trimmed.isNotEmpty() && nmeaMessagePattern.matcher(trimmed).matches()
                        }
                            .forEach { message ->
                                val trimmed = message.trim()
                                onMessageReceived?.invoke(trimmed)
                                _rawMessages.value = _rawMessages.value + trimmed
                            }
                    }
                } catch (e: IOException) {
                    if (isActive) {
                        delay(100)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isActive) {
                        delay(100)
                    }
                }
            }
        }
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

