@file:Suppress("LongMethod", "MagicNumber", "TooManyFunctions")

package com.yu_morph.smartbudget.ui.receipt

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val OverlayDimColor = Color.Black.copy(alpha = 0.55f)
private const val GUIDE_WIDTH_RATIO = 0.78f
private const val GUIDE_ASPECT_RATIO = 1.45f
private const val GUIDE_MAX_HEIGHT_RATIO = 0.6f
private const val JPEG_QUALITY = 90
private const val MAX_UPLOAD_EDGE_PX = 2_560

/**
 * 영수증을 촬영하는 전체 화면 다이얼로그.
 *
 * 카메라 권한 요청 → 가이드라인이 표시된 카메라 화면 → 촬영 사진 확인 순서로 진행한다.
 * 사용자가 "사용하기"를 누르면 JPEG 바이트를 [onConfirm]으로 전달한다.
 * 촬영한 이미지는 메모리에만 두며 파일로 저장하거나 로그로 남기지 않는다.
 */
@Composable
fun ReceiptCaptureDialog(onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            CameraPermissionGate(onDismiss = onDismiss) {
                ReceiptCaptureContent(onDismiss = onDismiss, onConfirm = onConfirm)
            }
        }
    }
}

/** 카메라 권한을 확인하고, 허용된 경우에만 [content]를 표시한다. */
@Composable
private fun CameraPermissionGate(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var isDenied by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            isDenied = !granted
        }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }
    when {
        hasPermission -> content()

        isDenied ->
            PermissionDeniedContent(
                onRetry = { launcher.launch(Manifest.permission.CAMERA) },
                onDismiss = onDismiss,
            )

        else -> Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun PermissionDeniedContent(onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CaptureTopBar(title = stringResource(R.string.receipt_capture_title), onClose = onDismiss)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.receipt_permission_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.receipt_permission_message),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        CaptureActionButton(
            text = stringResource(R.string.receipt_permission_retry),
            color = SmartBudgetGreen,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRetry,
        )
    }
}

/** 촬영 단계와 확인 단계를 전환한다. */
@Composable
private fun ReceiptCaptureContent(onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
    var captured by remember { mutableStateOf<Bitmap?>(null) }
    var isEncoding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bitmap = captured
    // 확인 단계에서 뒤로가기를 누르면 다이얼로그를 닫지 않고 다시 촬영 단계로 돌아간다.
    BackHandler(enabled = bitmap != null && !isEncoding) { captured = null }
    if (bitmap == null) {
        CameraPreviewStep(onCaptured = { captured = it }, onDismiss = onDismiss)
    } else {
        ReviewStep(
            bitmap = bitmap,
            isEncoding = isEncoding,
            onRetake = { captured = null },
            onUse = {
                isEncoding = true
                scope.launch {
                    val bytes = withContext(Dispatchers.Default) { bitmap.toJpegBytes() }
                    onConfirm(bytes)
                }
            },
        )
    }
}

@Composable
private fun CameraPreviewStep(onCaptured: (Bitmap) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller =
        remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }
        }
    var isCapturing by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        ReceiptGuideOverlay(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            CaptureTopBar(
                title = stringResource(R.string.receipt_capture_title),
                onClose = onDismiss,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text =
                    stringResource(
                        if (hasError) R.string.receipt_capture_failed else R.string.receipt_capture_guide,
                    ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            ShutterButton(
                enabled = !isCapturing,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    isCapturing = true
                    hasError = false
                    controller.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.use { it.toUprightBitmap() }
                                isCapturing = false
                                onCaptured(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                                hasError = true
                            }
                        },
                    )
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CaptureTopBar(title: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.receipt_capture_close),
                tint = Color.White,
            )
        }
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.receipt_capture_shutter)
    Box(
        modifier =
            modifier
                .size(76.dp)
                .border(4.dp, Color.White, CircleShape)
                .padding(8.dp)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f))
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!enabled) {
            CircularProgressIndicator(color = SmartBudgetGreen, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        } else {
            Icon(Icons.Outlined.CameraAlt, contentDescription = description, tint = Color.Black)
        }
    }
}

/** 카메라 영상 위에 영수증 위치를 안내하는 가이드라인을 오버레이로 그린다. */
@Composable
private fun ReceiptGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val frameWidth = size.width * GUIDE_WIDTH_RATIO
        val frameHeight = (frameWidth * GUIDE_ASPECT_RATIO).coerceAtMost(size.height * GUIDE_MAX_HEIGHT_RATIO)
        val topLeft = Offset((size.width - frameWidth) / 2, (size.height - frameHeight) / 2 - 24.dp.toPx())
        val frameSize = Size(frameWidth, frameHeight)
        val radius = CornerRadius(20.dp.toPx())
        // 가이드 영역 바깥을 어둡게 처리하고, 가이드 영역은 투명하게 비운다.
        drawRect(OverlayDimColor)
        drawRoundRect(Color.Transparent, topLeft, frameSize, radius, blendMode = BlendMode.Clear)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = topLeft,
            size = frameSize,
            cornerRadius = radius,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
        )
        drawCornerBrackets(topLeft, frameSize)
    }
}

/** 가이드 영역의 네 모서리에 강조 표시를 그린다. */
private fun DrawScope.drawCornerBrackets(topLeft: Offset, frameSize: Size) {
    val length = 28.dp.toPx()
    val inset = 2.dp.toPx()
    val strokeWidth = 4.dp.toPx()
    val left = topLeft.x + inset
    val top = topLeft.y + inset
    val right = topLeft.x + frameSize.width - inset
    val bottom = topLeft.y + frameSize.height - inset
    val corners =
        listOf(
            Triple(Offset(left, top), 1f, 1f),
            Triple(Offset(right, top), -1f, 1f),
            Triple(Offset(left, bottom), 1f, -1f),
            Triple(Offset(right, bottom), -1f, -1f),
        )
    corners.forEach { (corner, dx, dy) ->
        drawLine(SmartBudgetGreen, corner, corner + Offset(length * dx, 0f), strokeWidth, StrokeCap.Round)
        drawLine(SmartBudgetGreen, corner, corner + Offset(0f, length * dy), strokeWidth, StrokeCap.Round)
    }
}

/** 촬영한 사진을 보여 주고 다시 찍을지 사용할지 선택하게 한다. */
@Composable
private fun ReviewStep(bitmap: Bitmap, isEncoding: Boolean, onRetake: () -> Unit, onUse: () -> Unit) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.DocumentScanner,
            contentDescription = null,
            tint = SmartBudgetGreen,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.receipt_review_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.receipt_review_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))
        Image(
            bitmap = imageBitmap,
            contentDescription = stringResource(R.string.receipt_review_image),
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CaptureActionButton(
                text = stringResource(R.string.receipt_review_retake),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = !isEncoding,
                modifier = Modifier.weight(1f),
                onClick = onRetake,
            )
            CaptureActionButton(
                text = stringResource(R.string.receipt_review_use),
                color = SmartBudgetGreen,
                enabled = !isEncoding,
                isLoading = isEncoding,
                modifier = Modifier.weight(1f),
                onClick = onUse,
            )
        }
    }
}

/** 다이얼로그 "저장하기" 버튼과 같은 모양의 둥근 버튼. */
@Composable
private fun CaptureActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentColor: Color = Color.White,
) {
    Surface(
        modifier =
            modifier
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled || isLoading) color else color.copy(alpha = 0.5f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(text, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 카메라 센서 방향을 반영해 똑바로 선 비트맵으로 변환한다. */
private fun ImageProxy.toUprightBitmap(): Bitmap {
    val source = toBitmap()
    val degrees = imageInfo.rotationDegrees
    if (degrees == 0) return source
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

/**
 * OCR 업로드용 JPEG으로 변환한다.
 *
 * 서버의 파일당 10,000,000바이트 제한을 넘지 않고 업로드 시간을 줄이도록 긴 변을 [MAX_UPLOAD_EDGE_PX] 이하로 줄인다.
 */
private fun Bitmap.toJpegBytes(): ByteArray {
    val longEdge = maxOf(width, height)
    val scaled =
        if (longEdge > MAX_UPLOAD_EDGE_PX) {
            val ratio = MAX_UPLOAD_EDGE_PX.toFloat() / longEdge
            Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
        } else {
            this
        }
    return ByteArrayOutputStream().use { stream ->
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        stream.toByteArray()
    }
}
