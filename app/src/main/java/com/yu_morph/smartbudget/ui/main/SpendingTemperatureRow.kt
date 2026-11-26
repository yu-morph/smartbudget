@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.util.Locale

private const val GAUGE_MINIMUM_TEMPERATURE = 28.0
private const val GAUGE_NORMAL_TEMPERATURE = 36.5
private const val GAUGE_MAXIMUM_TEMPERATURE = 42.0

private data class TemperatureAppearance(
    @param:StringRes val label: Int,
    @param:DrawableRes val face: Int,
    val accentColor: Color,
)

@Composable
internal fun SpendingTemperatureRow(
    temperature: Double,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    val appearance = spendingTemperatureLevel(temperature).appearance()
    val label = stringResource(appearance.label)
    val formattedTemperature = String.format(Locale.US, "%.1f°C", temperature)
    val description = stringResource(R.string.home_spending_temperature_description, formattedTemperature, label)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = horizontalPadding, vertical = 20.dp)
                .semantics { contentDescription = description },
    ) {
        HomeSpendingCardHeading(stringResource(R.string.home_spending_temperature))
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp).offset(x = -6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeviceThermostat,
                    contentDescription = null,
                    tint = appearance.accentColor,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = formattedTemperature,
                    color = appearance.accentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Image(
                painter = painterResource(appearance.face),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }
        TemperatureGradientBar(
            position = spendingTemperaturePosition(temperature),
        )
    }
}

@Composable
internal fun HomeSpendingCardHeading(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun TemperatureGradientBar(position: Float) {
    val markerBorderColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
        val markerRadius = 7.5.dp.toPx()
        val innerRadius = 5.5.dp.toPx()
        val barHeight = 9.dp.toPx()
        val barStart = 0f
        val barWidth = size.width
        val barTop = (size.height - barHeight) / 2f
        val centerY = size.height / 2f
        val gradient =
            Brush.horizontalGradient(
                colorStops =
                    arrayOf(
                        0f to Color(0xFF397FC2),
                        0.25f to Color(0xFF5C9FD5),
                        0.5f to SmartBudgetGreen,
                        0.72f to Color(0xFFF08A38),
                        1f to Color(0xFFE94C64),
                    ),
                startX = barStart,
                endX = barStart + barWidth,
            )
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(barStart, barTop),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barHeight / 2f),
        )
        val markerX =
            (barStart + barWidth * position.coerceIn(0f, 1f))
                .coerceIn(markerRadius, size.width - markerRadius)
        val markerBackgroundColor = temperatureGradientColor(position)
        drawCircle(color = markerBorderColor, radius = markerRadius, center = Offset(markerX, centerY))
        drawCircle(color = markerBackgroundColor, radius = innerRadius, center = Offset(markerX, centerY))
        drawCircle(
            color = Color.White.copy(alpha = 0.74f),
            radius = 1.8.dp.toPx(),
            center = Offset(markerX - 1.8.dp.toPx(), centerY - 1.8.dp.toPx()),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.58f),
            radius = innerRadius,
            center = Offset(markerX, centerY),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private fun temperatureGradientColor(position: Float): Color {
    val boundedPosition = position.coerceIn(0f, 1f)
    val stops =
        listOf(
            0f to Color(0xFF397FC2),
            0.25f to Color(0xFF5C9FD5),
            0.5f to SmartBudgetGreen,
            0.72f to Color(0xFFF08A38),
            1f to Color(0xFFE94C64),
        )
    val upperIndex = stops.indexOfFirst { boundedPosition <= it.first }.coerceAtLeast(1)
    val lowerStop = stops[upperIndex - 1]
    val upperStop = stops[upperIndex]
    val fraction = (boundedPosition - lowerStop.first) / (upperStop.first - lowerStop.first)
    return androidx.compose.ui.graphics.lerp(lowerStop.second, upperStop.second, fraction)
}

internal fun spendingTemperaturePosition(temperature: Double): Float = when {
    temperature <= GAUGE_NORMAL_TEMPERATURE -> {
        val lowerProgress =
            (
                (temperature - GAUGE_MINIMUM_TEMPERATURE) /
                    (GAUGE_NORMAL_TEMPERATURE - GAUGE_MINIMUM_TEMPERATURE)
                ).coerceIn(0.0, 1.0)
        (lowerProgress * 0.5).toFloat()
    }

    else -> {
        val upperProgress =
            (
                (temperature - GAUGE_NORMAL_TEMPERATURE) /
                    (GAUGE_MAXIMUM_TEMPERATURE - GAUGE_NORMAL_TEMPERATURE)
                ).coerceIn(0.0, 1.0)
        (0.5 + upperProgress * 0.5).toFloat()
    }
}

private fun SpendingTemperatureLevel.appearance(): TemperatureAppearance = when (this) {
    SpendingTemperatureLevel.BAD ->
        TemperatureAppearance(
            label = R.string.home_spending_temperature_bad,
            face = R.drawable.spending_temperature_bad,
            accentColor = Color(0xFF397FC2),
        )

    SpendingTemperatureLevel.NOT_GOOD ->
        TemperatureAppearance(
            label = R.string.home_spending_temperature_not_good,
            face = R.drawable.spending_temperature_not_good,
            accentColor = Color(0xFF5C9FD5),
        )

    SpendingTemperatureLevel.OKAY ->
        TemperatureAppearance(
            label = R.string.home_spending_temperature_okay,
            face = R.drawable.spending_temperature_okay,
            accentColor = Color(0xFFD6A90F),
        )

    SpendingTemperatureLevel.GOOD ->
        TemperatureAppearance(
            label = R.string.home_spending_temperature_good,
            face = R.drawable.spending_temperature_good,
            accentColor = Color(0xFFF08A38),
        )

    SpendingTemperatureLevel.AMAZING ->
        TemperatureAppearance(
            label = R.string.home_spending_temperature_amazing,
            face = R.drawable.spending_temperature_amazing,
            accentColor = Color(0xFFE94C64),
        )
}
