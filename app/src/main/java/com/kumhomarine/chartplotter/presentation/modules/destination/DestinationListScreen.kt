package com.kumhomarine.chartplotter.presentation.modules.destination

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kumhomarine.chartplotter.data.export.PointExporter
import com.kumhomarine.chartplotter.data.models.SavedPoint
import com.kumhomarine.chartplotter.domain.repositories.PointRepository
import java.io.File

/**
 * 목적지(저장 포인트) 리스트 화면
 * - Launcher 목적지 클릭 시 표시
 * - 뒤로가기/홈 버튼 시 activity.finish()로 Launcher 복귀
 * - 내보내기 버튼으로 GPX 내보내기 및 공유
 */
@Composable
fun DestinationListScreen(
    pointRepository: PointRepository,
    activity: androidx.activity.ComponentActivity,
    onDismiss: () -> Unit = { activity.finish() }
) {
    val points = remember { mutableStateOf<List<SavedPoint>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            points.value = pointRepository.getAllSavedPoints()
        }
    }

    BackHandler(enabled = true) {
        activity.finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 바: 제목, 뒤로가기, 홈, 내보내기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { activity.finish() }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "홈",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "목적지",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        val list = points.value
                        if (list.isEmpty()) return@Button
                        val exporter = PointExporter()
                        val outputDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
                        val file = exporter.exportPoints(list, outputDir)
                        file?.let { f ->
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    f
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/gpx+xml"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "목적지 내보내기"))
                            } catch (e: Exception) {
                                android.util.Log.e("[DestinationListScreen]", "내보내기 공유 실패: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("내보내기", color = Color.White, fontSize = 14.sp)
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f))

            // 목적지 리스트
            if (points.value.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "저장된 목적지가 없습니다",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(points.value) { point ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = point.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
