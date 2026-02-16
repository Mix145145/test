package com.sbm.aoi.ui.inspection

import android.Manifest
import android.content.pm.PackageManager
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.aoi.camera.CameraManager
import com.sbm.aoi.ui.components.DetectionOverlay
import com.sbm.aoi.ui.theme.Primary
import com.sbm.aoi.ui.theme.Secondary

@Composable
fun InspectionScreen(
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeModel by viewModel.activeModel.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val detections by viewModel.frameAnalyzer.detections.collectAsState()
    val fps by viewModel.frameAnalyzer.fps.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraManager = remember { CameraManager(context) }
    val isFlashOn by cameraManager.isFlashOn.collectAsState()
    val zoomRatio by cameraManager.zoomRatio.collectAsState()
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        onDispose { cameraManager.release() }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Требуется доступ к камере",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Разрешите доступ к камере для работы инспекции",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Камера
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                // Pinch-to-zoom
                val scaleDetector = ScaleGestureDetector(ctx,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            cameraManager.onPinchZoom(detector.scaleFactor)
                            return true
                        }
                    }
                )

                previewView.setOnTouchListener { view, event ->
                    scaleDetector.onTouchEvent(event)
                    // Tap-to-focus
                    if (event.action == MotionEvent.ACTION_UP && !scaleDetector.isInProgress) {
                        cameraManager.tapToFocus(
                            event.x, event.y,
                            view.width.toFloat(), view.height.toFloat(),
                        )
                    }
                    true
                }

                cameraManager.startCamera(
                    lifecycleOwner,
                    previewView,
                    viewModel.frameAnalyzer,
                )

                previewViewRef = previewView
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Detection overlay
        activeModel?.let { model ->
            DetectionOverlay(
                detections = detections,
                imageWidth = model.inputSize,
                imageHeight = model.inputSize,
                color = Color(model.colorArgb),
                opacity = model.overlayOpacity,
                showLabels = model.showLabels,
                showConfidence = model.showConfidence,
                highlightMethod = model.highlightMethod,
            )
        }

        // Верхняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Название модели
            Text(
                text = activeModel?.name ?: "Нет модели",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                modifier = Modifier.weight(1f),
            )

            // Переключение модели
            IconButton(onClick = { viewModel.switchModel() }) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Переключить модель",
                    tint = Color.White,
                )
            }

            // FPS индикатор
            Text(
                text = "%.0f FPS".format(fps),
                style = MaterialTheme.typography.bodySmall,
                color = Secondary,
            )
        }

        // Правая панель
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Вспышка
            IconButton(
                onClick = { cameraManager.toggleFlash() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Icon(
                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Вспышка",
                    tint = if (isFlashOn) Color.Yellow else Color.White,
                )
            }

            // Пауза
            IconButton(
                onClick = { viewModel.togglePause() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Продолжить" else "Пауза",
                    tint = Color.White,
                )
            }

            // Зум индикатор
            if (zoomRatio > 1.01f) {
                Text(
                    text = "%.1fx".format(zoomRatio),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        // Нижняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(vertical = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Автофокус
            IconButton(onClick = { cameraManager.autoFocus() }) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = "Автофокус",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Сделать фото
            IconButton(
                onClick = {
                    // Захват фото будет реализован через CameraX ImageCapture
                },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Primary),
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Сделать фото",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Переключение камеры
            IconButton(onClick = {
                previewViewRef?.let { previewView ->
                    cameraManager.switchCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        analyzer = viewModel.frameAnalyzer,
                    )
                }
            }) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Переключить камеру",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Индикатор паузы
        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = "ПАУЗА",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }

        // Статус "нет модели"
        if (activeModel == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp),
            ) {
                Text(
                    text = "Добавьте и активируйте модель\nв разделе \"Модели\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
            }
        }
    }
}
