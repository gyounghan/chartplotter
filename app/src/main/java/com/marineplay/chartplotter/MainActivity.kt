package com.marineplay.chartplotter

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.marineplay.chartplotter.ui.theme.ChartPlotterTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver

data class SavedPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val color: Color,
    val timestamp: Long
)

class MainActivity : ComponentActivity() {
    
    private var locationManager: LocationManager? = null
    private var mapLibreMap: MapLibreMap? = null
    private var showDialog by mutableStateOf(false)
    private var isMapInitialized by mutableStateOf(false)
    private var centerCoordinates by mutableStateOf("")
    private var pointName by mutableStateOf("")
    private var selectedColor by mutableStateOf(Color.Red)
    private var currentLatLng: LatLng? = null
    private lateinit var sharedPreferences: SharedPreferences
    
    // 포인트 관리 다이얼로그 관련
    private var showPointManageDialog by mutableStateOf(false)
    private var selectedPoint: SavedPoint? = null
    private var showEditDialog by mutableStateOf(false)
    private var editPointName by mutableStateOf("")
    private var editSelectedColor by mutableStateOf(Color.Red)

    // 위치 권한 요청 런처
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 정확한 위치 권한이 허용됨
                locationManager?.startLocationUpdates()
                locationManager?.startAutoTracking()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // 대략적인 위치 권한이 허용됨
                locationManager?.startLocationUpdates()
                locationManager?.startAutoTracking()
            }
            else -> {
                // 위치 권한이 거부됨
                android.util.Log.w("[MainActivity]", "위치 권한이 거부되었습니다.")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("chart_plotter_points", Context.MODE_PRIVATE)
        
        // 저장된 포인트들 로드
        val savedPoints = loadPointsFromLocal()
        android.util.Log.d("[MainActivity]", "저장된 포인트 ${savedPoints.size}개 로드 완료")

        // MapLibre 초기화
        MapLibre.getInstance(this)

        setContent {
            ChartPlotterTheme {
                // 포인트 등록 다이얼로그 표시
                if (showDialog) {
                    PointRegistrationDialog(
                        centerCoordinates = centerCoordinates,
                        pointName = pointName,
                        onPointNameChange = { pointName = it },
                        selectedColor = selectedColor,
                        onColorChange = { selectedColor = it },
                        onRegister = { registerPoint() },
                        onDismiss = { showDialog = false }
                    )
                }
                
                // 포인트 관리 다이얼로그 표시
                if (showPointManageDialog && selectedPoint != null) {
                    PointManageDialog(
                        point = selectedPoint!!,
                        onDelete = { deletePoint(selectedPoint!!) },
                        onEdit = { 
                            showPointManageDialog = false
                            showEditDialog = true
                        },
                        onDismiss = { showPointManageDialog = false }
                    )
                }
                
                // 포인트 편집 다이얼로그 표시
                if (showEditDialog && selectedPoint != null) {
                    PointEditDialog(
                        point = selectedPoint!!,
                        pointName = editPointName,
                        onPointNameChange = { editPointName = it },
                        selectedColor = editSelectedColor,
                        onColorChange = { editSelectedColor = it },
                        onSave = { updatePoint(selectedPoint!!, editPointName, editSelectedColor) },
                        onDismiss = { showEditDialog = false }
                    )
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                locationManager?.startAutoTracking()
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ship),
                                contentDescription = "내 위치로 이동",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    ChartPlotterMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onMapReady = { map ->
                            if (!isMapInitialized) {
                                mapLibreMap = map
                                locationManager = LocationManager(this@MainActivity, map)

                                // PMTiles 로드 후 선박 아이콘과 포인트 마커 추가를 위해 스타일 로드 완료를 기다림
                                map.getStyle { style ->
                                    locationManager?.addShipToMap(style)
                                    locationManager?.addPointsToMap(style)
                                    
                                    // 저장된 포인트들을 지도에 표시
                                    val savedPoints = loadPointsFromLocal()
                                    locationManager?.updatePointsOnMap(savedPoints)
                                }

                                // 지도 터치/드래그 감지하여 자동 추적 중지
                                map.addOnCameraMoveListener {
                                    locationManager?.stopAutoTracking()
                                }
                                
                                // 지도 클릭 이벤트 처리 (포인트 마커 클릭 감지)
                                map.addOnMapClickListener { latLng ->
                                    val savedPoints = loadPointsFromLocal()
                                    val pointClicked = locationManager?.handlePointClick(latLng, savedPoints) ?: false
                                    if (pointClicked) {
                                        // 포인트가 클릭되었으면 true 반환하여 기본 지도 클릭 이벤트 방지
                                        true
                                    } else {
                                        // 포인트가 클릭되지 않았으면 기본 지도 클릭 이벤트 허용
                                        false
                                    }
                                }
                                
                                // 포인트 클릭 리스너 설정
                                locationManager?.setOnPointClickListener { point ->
                                    selectedPoint = point
                                    editPointName = point.name
                                    editSelectedColor = point.color
                                    showPointManageDialog = true
                                }

                                // 위치 권한 확인 및 요청
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    locationManager?.startLocationUpdates()
                                    // 처음 시작 시 자동 추적 활성화
                                    locationManager?.startAutoTracking()
                                } else {
                                    locationPermissionRequest.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                                
                                isMapInitialized = true
                            }
                        },
                        isDialogShown = showDialog // ⬅ 전달
                    )
                }
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_3 -> {
                // 현재 화면 중앙 좌표 가져오기
                mapLibreMap?.cameraPosition?.target?.let { latLng ->
                    currentLatLng = latLng
                    centerCoordinates = "위도: ${String.format("%.6f", latLng.latitude)}\n경도: ${String.format("%.6f", latLng.longitude)}"
                    pointName = "" // 포인트명 초기화
                    selectedColor = Color.Red // 색상 초기화
                } ?: run {
                    centerCoordinates = "좌표를 가져올 수 없습니다."
                    currentLatLng = null
                }
                showDialog = true
                return true
            }
            KeyEvent.KEYCODE_BUTTON_4 -> {
                // 줌 아웃
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom - 1.0).coerceAtLeast(0.0)
                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                    map.animateCamera(cameraUpdate, 300)
                    android.util.Log.d("[MainActivity]", "줌 아웃: $currentZoom -> $newZoom")
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_5 -> {
                // 줌 인
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom + 1.0).coerceAtMost(20.0)
                    val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                    map.animateCamera(cameraUpdate, 300)
                    android.util.Log.d("[MainActivity]", "줌 인: $currentZoom -> $newZoom")
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun registerPoint() {
        currentLatLng?.let { latLng ->
            val point = SavedPoint(
                name = pointName,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                color = selectedColor,
                timestamp = System.currentTimeMillis()
            )
            
            savePointToLocal(point)
            
            // 새로 등록된 포인트를 지도에 즉시 표시
            val allPoints = loadPointsFromLocal()
            locationManager?.updatePointsOnMap(allPoints)
            
            android.util.Log.d("[MainActivity]", "포인트 등록 완료: $pointName, 좌표: $latLng, 색상: $selectedColor")
            showDialog = false
        }
    }
    
    private fun savePointToLocal(point: SavedPoint) {
        try {
            val existingPoints = loadPointsFromLocal().toMutableList()
            existingPoints.add(point)
            
            val jsonArray = JSONArray()
            existingPoints.forEach { p ->
                val jsonObject = JSONObject().apply {
                    put("name", p.name)
                    put("latitude", p.latitude)
                    put("longitude", p.longitude)
                    put("color", AndroidColor.argb(
                        (p.color.alpha * 255).toInt(),
                        (p.color.red * 255).toInt(),
                        (p.color.green * 255).toInt(),
                        (p.color.blue * 255).toInt()
                    ))
                    put("timestamp", p.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPreferences.edit()
                .putString("saved_points", jsonArray.toString())
                .apply()
                
            android.util.Log.d("[MainActivity]", "포인트 저장 완료: ${existingPoints.size}개")
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 저장 실패: ${e.message}")
        }
    }
    
    private fun loadPointsFromLocal(): List<SavedPoint> {
        return try {
            val jsonString = sharedPreferences.getString("saved_points", null)
            if (jsonString != null) {
                val jsonArray = JSONArray(jsonString)
                val points = mutableListOf<SavedPoint>()
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val point = SavedPoint(
                        name = jsonObject.getString("name"),
                        latitude = jsonObject.getDouble("latitude"),
                        longitude = jsonObject.getDouble("longitude"),
                        color = Color(jsonObject.getInt("color")),
                        timestamp = jsonObject.getLong("timestamp")
                    )
                    points.add(point)
                }
                points
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 로드 실패: ${e.message}")
            emptyList()
        }
    }
    
    /** 포인트 삭제 */
    private fun deletePoint(point: SavedPoint) {
        try {
            val existingPoints = loadPointsFromLocal().toMutableList()
            existingPoints.removeAll { it.timestamp == point.timestamp }
            
            val jsonArray = JSONArray()
            existingPoints.forEach { p ->
                val jsonObject = JSONObject().apply {
                    put("name", p.name)
                    put("latitude", p.latitude)
                    put("longitude", p.longitude)
                    put("color", AndroidColor.argb(
                        (p.color.alpha * 255).toInt(),
                        (p.color.red * 255).toInt(),
                        (p.color.green * 255).toInt(),
                        (p.color.blue * 255).toInt()
                    ))
                    put("timestamp", p.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPreferences.edit()
                .putString("saved_points", jsonArray.toString())
                .apply()
            
            // 지도에서 포인트 제거
            locationManager?.updatePointsOnMap(existingPoints)
            
            android.util.Log.d("[MainActivity]", "포인트 삭제 완료: ${point.name}")
            showPointManageDialog = false
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 삭제 실패: ${e.message}")
        }
    }
    
    /** 포인트 업데이트 */
    private fun updatePoint(originalPoint: SavedPoint, newName: String, newColor: Color) {
        try {
            val existingPoints = loadPointsFromLocal().toMutableList()
            val pointIndex = existingPoints.indexOfFirst { it.timestamp == originalPoint.timestamp }
            
            if (pointIndex != -1) {
                val updatedPoint = originalPoint.copy(
                    name = newName,
                    color = newColor
                )
                existingPoints[pointIndex] = updatedPoint
                
                val jsonArray = JSONArray()
                existingPoints.forEach { p ->
                    val jsonObject = JSONObject().apply {
                        put("name", p.name)
                        put("latitude", p.latitude)
                        put("longitude", p.longitude)
                        put("color", AndroidColor.argb(
                            (p.color.alpha * 255).toInt(),
                            (p.color.red * 255).toInt(),
                            (p.color.green * 255).toInt(),
                            (p.color.blue * 255).toInt()
                        ))
                        put("timestamp", p.timestamp)
                    }
                    jsonArray.put(jsonObject)
                }
                
                sharedPreferences.edit()
                    .putString("saved_points", jsonArray.toString())
                    .apply()
                
                // 지도에서 포인트 업데이트
                locationManager?.updatePointsOnMap(existingPoints)
                
                android.util.Log.d("[MainActivity]", "포인트 업데이트 완료: $newName")
            }
            
            showEditDialog = false
        } catch (e: Exception) {
            android.util.Log.e("[MainActivity]", "포인트 업데이트 실패: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.stopLocationUpdates()
    }
}

@Composable
fun ChartPlotterMap(
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {},
    showCenterMarker: Boolean = true,
    isDialogShown: Boolean = false, // ⬅ 추가
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapView는 한 번만 생성
    val mapView = remember { MapView(context) }

    // MapView 생명주기 연결
    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null) // 중요: 최초 1회
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // 다이얼로그가 떠 있을 땐 렌더 일시 정지 → 닫히면 재개
    LaunchedEffect(isDialogShown) {
        if (isDialogShown) mapView.onPause() else mapView.onResume()
    }

    // Map이 준비되었을 때 1회만 초기 설정
    val mapConfigured = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = modifier
        ) { mapViewInstance ->
            mapViewInstance.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(map: MapLibreMap) {
                    if (!mapConfigured.value) {
                        val centerPoint = LatLng(35.0, 128.0)
                        map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(centerPoint)
                            .zoom(8.0)
                            .build()

                        PMTilesLoader.loadPMTilesFromAssets(context, map)

                        mapConfigured.value = true       // ⬅ 재초기화 방지
                        onMapReady(map)
                    }
                }
            })
        }

        if (showCenterMarker) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "+")
            }
        }
    }
}




@Composable
fun PointRegistrationDialog(
    centerCoordinates: String,
    pointName: String,
    onPointNameChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    onRegister: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Red to "빨간색",
        Color.Blue to "파란색", 
        Color.Green to "초록색",
        Color.Yellow to "노란색",
        Color.Magenta to "자홍색",
        Color.Cyan to "청록색"
    )
    
    var showColorMenu by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 등록") },
        text = { 
            Column {
                // 좌표 표시
                Text("현재 화면 중앙 좌표:", fontSize = 14.sp)
                Text(
                    text = centerCoordinates,
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 12.sp
                )
                
                // 포인트명 입력
                TextField(
                    value = pointName,
                    onValueChange = onPointNameChange,
                    label = { Text("포인트명") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 색상 선택
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("색상:", modifier = Modifier.padding(end = 8.dp))
                    Box(
                        modifier = Modifier
                            .background(selectedColor, CircleShape)
                            .clickable { showColorMenu = true }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .background(selectedColor, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = colors.find { it.first == selectedColor }?.second ?: "빨간색",
                        fontSize = 12.sp
                    )
                }
                
                // 색상 드롭다운 메뉴
                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { showColorMenu = false }
                ) {
                    colors.forEach { (color, name) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(color, CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                onColorChange(color)
                                showColorMenu = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRegister,
                enabled = pointName.isNotBlank()
            ) {
                Text("등록")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
fun PointManageDialog(
    point: SavedPoint,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 관리") },
        text = { 
            Column {
                Text("포인트명: ${point.name}", fontSize = 16.sp)
                Text("위도: ${String.format("%.6f", point.latitude)}", fontSize = 14.sp)
                Text("경도: ${String.format("%.6f", point.longitude)}", fontSize = 14.sp)
                Text("등록일: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(point.timestamp))}", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Blue
                )
            ) {
                Text("변경")
            }
        },
        dismissButton = {
            Row {
                Button(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Red
                    )
                ) {
                    Text("삭제")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

@Composable
fun PointEditDialog(
    point: SavedPoint,
    pointName: String,
    onPointNameChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Red to "빨간색",
        Color.Blue to "파란색", 
        Color.Green to "초록색",
        Color.Yellow to "노란색",
        Color.Magenta to "자홍색",
        Color.Cyan to "청록색"
    )
    
    var showColorMenu by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 편집") },
        text = { 
            Column {
                // 좌표 표시
                Text("좌표:", fontSize = 14.sp)
                Text(
                    text = "위도: ${String.format("%.6f", point.latitude)}\n경도: ${String.format("%.6f", point.longitude)}",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 12.sp
                )
                
                // 포인트명 입력
                TextField(
                    value = pointName,
                    onValueChange = onPointNameChange,
                    label = { Text("포인트명") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 색상 선택
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("색상:", modifier = Modifier.padding(end = 8.dp))
                    Box(
                        modifier = Modifier
                            .background(selectedColor, CircleShape)
                            .clickable { showColorMenu = true }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .background(selectedColor, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = colors.find { it.first == selectedColor }?.second ?: "빨간색",
                        fontSize = 12.sp
                    )
                }
                
                // 색상 드롭다운 메뉴
                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { showColorMenu = false }
                ) {
                    colors.forEach { (color, name) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(color, CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                onColorChange(color)
                                showColorMenu = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = pointName.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChartPlotterMapPreview() {
    ChartPlotterTheme {
        Text("Chart Plotter Map Preview")
    }
}