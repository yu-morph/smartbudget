@file:Suppress("LongMethod", "MagicNumber")

package com.yu_morph.smartbudget.ui.receipt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import kotlin.math.PI
import kotlin.math.sin

/** 응답을 기다리는 동안 진행 막대가 멈춰 있지 않도록 채워 두는 최대 비율. */
private const val PENDING_PROGRESS_LIMIT = 0.9f
private const val PENDING_PROGRESS_DURATION_MILLIS = 3_000
private const val SCAN_DURATION_MILLIS = 1_400
private const val FLOAT_DURATION_MILLIS = 2_400

/**
 * 영수증 OCR 요청을 처리하는 동안 표시하는 전체 화면 로딩 다이얼로그.
 *
 * 처리 시간을 알 수 없으므로 진행 막대는 [PENDING_PROGRESS_LIMIT]까지 천천히 채우고, 응답이 오면 다이얼로그를 닫는다.
 * 뒤로가기를 누르면 [onCancel]을 호출한다.
 */
@Composable
fun ReceiptAnalyzingDialog(onCancel: () -> Unit) {
    Dialog(
        onDismissRequest = onCancel,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                ScanningReceiptIllustration(modifier = Modifier.size(240.dp))
                Spacer(Modifier.height(36.dp))
                Text(
                    text = stringResource(R.string.receipt_analyzing_title),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.receipt_analyzing_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                PendingProgressBar(modifier = Modifier.width(220.dp))
                Spacer(Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
private fun PendingProgressBar(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = PENDING_PROGRESS_LIMIT,
            animationSpec = tween(PENDING_PROGRESS_DURATION_MILLIS, easing = FastOutSlowInEasing),
        )
    }
    val trackColor =
        if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFEDEDED) else Color(0xFF2C2C2C)
    Box(
        modifier =
            modifier
                .height(12.dp)
                .clip(CircleShape)
                .background(trackColor)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.value, 0f..1f) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(SmartBudgetGreen),
        )
    }
}

/** 원형 배경 위의 영수증을 스캔선이 위아래로 훑는 일러스트. */
@Composable
private fun ScanningReceiptIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "영수증 스캔")
    val scanFraction by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            tween(SCAN_DURATION_MILLIS, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "스캔선 위치",
    )
    val floatPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(FLOAT_DURATION_MILLIS, easing = LinearEasing)),
        label = "장식 점 움직임",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val circleRadius = size.minDimension * 0.42f
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(SmartBudgetGreen.copy(alpha = 0.18f), SmartBudgetGreen.copy(alpha = 0.08f)),
                        center = center,
                        radius = circleRadius,
                    ),
                radius = circleRadius,
                center = center,
            )
            drawDecorationDots(center, circleRadius, floatPhase)
        }
        Image(
            painter = painterResource(R.drawable.receipt_scan_3d),
            contentDescription = null,
            modifier = Modifier.size(width = 164.dp, height = 205.dp),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val receiptSize = Size(size.width * 0.5f, size.height * 0.66f)
            val receiptTopLeft =
                Offset(
                    x = (size.width - receiptSize.width) / 2,
                    y = (size.height - receiptSize.height) / 2,
                )
            drawScanLine(receiptTopLeft, receiptSize, scanFraction)
        }
    }
}

/** 원 바깥쪽에 떠 있는 장식 점을 그린다. */
private fun DrawScope.drawDecorationDots(center: Offset, circleRadius: Float, phase: Float) {
    val dots =
        listOf(
            Triple(Offset(-0.95f, -0.85f), 0.11f, 0f),
            Triple(Offset(1.08f, 0.47f), 0.075f, 2f),
            Triple(Offset(-0.95f, 0.78f), 0.05f, 4f),
        )
    dots.forEach { (position, radiusRatio, phaseOffset) ->
        val drift = sin(phase + phaseOffset) * circleRadius * 0.04f
        drawCircle(
            color = SmartBudgetGreen.copy(alpha = 0.45f),
            radius = circleRadius * radiusRatio,
            center = Offset(center.x + position.x * circleRadius, center.y + position.y * circleRadius + drift),
        )
    }
}

/** 빛이 퍼지는 효과가 있는 스캔선을 그린다. */
private fun DrawScope.drawScanLine(topLeft: Offset, receiptSize: Size, fraction: Float) {
    val y = topLeft.y + receiptSize.height * fraction
    val overhang = receiptSize.width * 0.28f
    val lineLeft = topLeft.x - overhang
    val lineWidth = receiptSize.width + overhang * 2
    val glowHeight = 36.dp.toPx()
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, SmartBudgetGreen.copy(alpha = 0.28f), Color.Transparent),
                startY = y - glowHeight / 2,
                endY = y + glowHeight / 2,
            ),
        topLeft = Offset(topLeft.x - overhang / 2, y - glowHeight / 2),
        size = Size(receiptSize.width + overhang, glowHeight),
    )
    val lineHeight = 4.dp.toPx()
    drawRoundRect(
        brush =
            Brush.horizontalGradient(
                colors =
                    listOf(
                        SmartBudgetGreen.copy(alpha = 0.3f),
                        SmartBudgetGreen,
                        SmartBudgetGreen,
                        SmartBudgetGreen.copy(alpha = 0.3f),
                    ),
                startX = lineLeft,
                endX = lineLeft + lineWidth,
            ),
        topLeft = Offset(lineLeft, y - lineHeight / 2),
        size = Size(lineWidth, lineHeight),
        cornerRadius = CornerRadius(lineHeight / 2),
    )
}
