@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caverock.androidsvg.SVG
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.auth.PrimaryAuthButton
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import kotlin.math.min

private data class BankOption(
    val key: String,
    @param:StringRes val nameResource: Int,
    val logoAssetName: String,
    val logoWidth: Int,
    val logoHeight: Int,
    val logoScale: Float = 1f,
)

private val BankOptions =
    listOf(
        BankOption("kb", R.string.bank_kb, "KB국민은행 심볼.svg", 30, 24),
        BankOption("shinhan", R.string.bank_shinhan, "신한은행 심볼.svg", 28, 28),
        BankOption("hana", R.string.bank_hana, "하나은행 심볼.svg", 27, 28),
        BankOption("woori", R.string.bank_woori, "우리은행 심볼.svg", 28, 28),
        BankOption("nh", R.string.bank_nh, "NH농협은행 심볼.svg", 23, 29),
        BankOption("ibk", R.string.bank_ibk, "IBK기업은행 심볼.svg", 28, 28),
        BankOption("sc", R.string.bank_sc, "SC제일은행 심볼.svg", 19, 30),
        BankOption("citi", R.string.bank_citi, "한국씨티은행 심볼.svg", 30, 20),
        BankOption("im", R.string.bank_im, "iM뱅크 심볼.svg", 32, 16),
        BankOption("busan", R.string.bank_busan, "BNK부산은행 심볼.svg", 32, 10),
        BankOption("gyeongnam", R.string.bank_gyeongnam, "BNK경남은행 심볼.svg", 32, 10),
        BankOption("gwangju", R.string.bank_gwangju, "광주은행 심볼.svg", 28, 28),
        BankOption("jeonbuk", R.string.bank_jeonbuk, "전북은행 심볼.svg", 28, 28),
        BankOption("jeju", R.string.bank_jeju, "제주은행 심볼.svg", 28, 28),
        BankOption("kbank", R.string.bank_kbank, "케이뱅크 심볼.svg", 32, 12, logoScale = 3f),
        BankOption("kakao", R.string.bank_kakao, "카카오뱅크 심볼.svg", 28, 28),
        BankOption("toss", R.string.bank_toss, "토스뱅크 심볼.svg", 28, 26),
        BankOption("suhyup", R.string.bank_suhyup, "수협은행 심볼.svg", 30, 22),
        BankOption("kdb", R.string.bank_kdb, "한국산업은행 심볼.svg", 30, 27),
    )

@Composable
internal fun BankConnectionScreen(isLoading: Boolean, modifier: Modifier = Modifier, onConnect: () -> Unit) {
    val selectedKeys = remember { mutableStateListOf<String>().apply { addAll(BankOptions.map { it.key }) } }
    if (isLoading) {
        BankSyncLoading(modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BankSelectionHeader(
            selectedAll = selectedKeys.size == BankOptions.size,
            onToggleAll = {
                if (selectedKeys.size == BankOptions.size) {
                    selectedKeys.clear()
                } else {
                    selectedKeys.clear()
                    selectedKeys.addAll(BankOptions.map { it.key })
                }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(BankOptions, key = { it.key }) { bank ->
                BankItem(
                    bank = bank,
                    selected = bank.key in selectedKeys,
                    onClick = {
                        if (bank.key in selectedKeys) selectedKeys.remove(bank.key) else selectedKeys.add(bank.key)
                    },
                )
            }
        }
        PrimaryAuthButton(
            text = stringResource(R.string.settings_bank_connect),
            enabled = selectedKeys.isNotEmpty(),
            onClick = onConnect,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BankSelectionHeader(selectedAll: Boolean, onToggleAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = stringResource(R.string.settings_bank_connection_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.settings_bank_connection_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_bank_select_all),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            SelectionCheck(selected = selectedAll, onClick = onToggleAll)
        }
    }
}

@Composable
private fun BankItem(bank: BankOption, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier.fillMaxWidth().height(84.dp).clip(shape).clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SmartBudgetGreen else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                SvgAssetImage(
                    assetName = bank.logoAssetName,
                    contentDescription = stringResource(R.string.settings_bank_logo, stringResource(bank.nameResource)),
                    renderWidth = bank.logoWidth,
                    renderHeight = bank.logoHeight,
                    modifier =
                        Modifier
                            .size(bank.logoWidth.dp, bank.logoHeight.dp)
                            .graphicsLayer(scaleX = bank.logoScale, scaleY = bank.logoScale),
                )
            }
            Text(
                text = stringResource(bank.nameResource),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SvgAssetImage(
    assetName: String,
    contentDescription: String,
    renderWidth: Int,
    renderHeight: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val image = remember(assetName, renderWidth, renderHeight) {
        context.assets.open(assetName).use { input ->
            val bitmapWidth = renderWidth * SVG_RENDER_SCALE
            val bitmapHeight = renderHeight * SVG_RENDER_SCALE
            val picture = SVG.getFromInputStream(input).renderToPicture()
            Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                val scale =
                    min(
                        bitmapWidth.toFloat() / picture.width,
                        bitmapHeight.toFloat() / picture.height,
                    )
                val horizontalOffset = (bitmapWidth - picture.width * scale) / 2f
                val verticalOffset = (bitmapHeight - picture.height * scale) / 2f
                canvas.translate(horizontalOffset, verticalOffset)
                canvas.scale(scale, scale)
                canvas.drawPicture(picture)
            }.asImageBitmap()
        }
    }
    Image(
        bitmap = image,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private const val SVG_RENDER_SCALE = 8
private const val BANK_SYNC_CONTENT_OFFSET_DP = 42

@Composable
private fun SelectionCheck(selected: Boolean, onClick: (() -> Unit)? = null) {
    val indicatorColor = if (selected) SmartBudgetGreen else MaterialTheme.colorScheme.outline
    val interactionModifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clip(CircleShape).clickable(onClick = onClick)
        }
    Box(
        modifier =
            Modifier
                .size(22.dp)
                .background(if (selected) SmartBudgetGreen else Color.Transparent, CircleShape)
                .border(1.dp, indicatorColor, CircleShape)
                .then(interactionModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = if (selected) Color.White else indicatorColor,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
internal fun BankSyncLoading(modifier: Modifier, @StringRes message: Int = R.string.settings_bank_fetching) {
    val dataFlowTransition = rememberInfiniteTransition(label = "은행 데이터 흐름")
    val dataFlowProgress by
        dataFlowTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
            label = "은행 데이터 이동",
        )
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp)
                .offset(y = (-BANK_SYNC_CONTENT_OFFSET_DP).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            BankDataFlow(progress = dataFlowProgress, modifier = Modifier.fillMaxSize())
            Image(
                painter = painterResource(R.drawable.bank_sync_robot),
                contentDescription = null,
                modifier = Modifier.size(220.dp),
            )
        }
        LinearProgressIndicator(
            modifier =
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth(0.55f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
            color = SmartBudgetGreen,
            trackColor = SmartBudgetGreen.copy(alpha = 0.16f),
        )
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(top = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun BankDataFlow(progress: Float, modifier: Modifier = Modifier) {
    ComposeCanvas(modifier = modifier) {
        repeat(6) { index ->
            val phase = (progress + index / 6f) % 1f
            val comesFromLeft = index % 2 == 0
            val startX = if (comesFromLeft) -size.width * 0.06f else size.width * 1.06f
            val endX = size.width * 0.52f
            val startY = size.height * (0.18f + (index % 3) * 0.16f)
            val endY = size.height * 0.58f
            val curvedY = startY + (endY - startY) * phase - size.height * 0.12f * phase * (1f - phase)
            val position =
                Offset(
                    x = startX + (endX - startX) * phase,
                    y = curvedY,
                )
            val alpha = (1f - kotlin.math.abs(phase - 0.5f) * 1.5f).coerceIn(0.12f, 0.85f)
            drawCircle(
                color = SmartBudgetGreen.copy(alpha = alpha * 0.18f),
                radius = 11.dp.toPx(),
                center = position,
            )
            drawCircle(
                color = SmartBudgetGreen.copy(alpha = alpha),
                radius = if (index % 3 == 0) 4.5.dp.toPx() else 3.dp.toPx(),
                center = position,
            )
        }
    }
}
