package com.yu_morph.smartbudget.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.ui.theme.OutfitFontFamily
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen

@Composable
fun BrandSignature(modifier: Modifier = Modifier, logoSize: Dp = 64.dp, textSize: Int = 28) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(logoSize)
                    .background(SmartBudgetGreen, RoundedCornerShape(percent = 22)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_logo_description),
                modifier = Modifier.size(logoSize),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.brand_smart),
                color = SmartBudgetGreen,
                fontFamily = OutfitFontFamily,
                fontSize = textSize.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.brand_budget),
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = OutfitFontFamily,
                fontSize = textSize.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
