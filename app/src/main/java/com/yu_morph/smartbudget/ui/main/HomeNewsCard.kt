@file:Suppress("MagicNumber")

package com.yu_morph.smartbudget.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yu_morph.smartbudget.R
import com.yu_morph.smartbudget.data.remote.news.NewsArticle
import com.yu_morph.smartbudget.ui.theme.SmartBudgetGreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
internal fun NewsCard(
    state: HomeNewsUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BreakingNewsTicker(
                articles = state.breakingNews,
                isLoading = state.isLoading,
                onArticleClick = onArticleClick,
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
            EconomyNewsSection(
                state = state,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
                onArticleClick = onArticleClick,
            )
        }
    }
}

@Composable
private fun BreakingNewsTicker(
    articles: List<NewsArticle>,
    isLoading: Boolean,
    onArticleClick: (NewsArticle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Campaign,
            contentDescription = null,
            modifier = Modifier.padding(start = 18.dp).size(16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(R.string.home_breaking_news),
            modifier = Modifier.padding(start = 4.dp, end = 7.dp),
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        when {
            articles.isNotEmpty() -> ArticleMarquee(articles, onArticleClick, Modifier.weight(1f))

            isLoading -> CircularProgressIndicator(
                modifier = Modifier.padding(start = 6.dp).size(17.dp),
                strokeWidth = 2.dp,
                color = SmartBudgetGreen,
            )

            else -> Text(
                text = stringResource(R.string.home_news_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun ArticleMarquee(
    articles: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density
    var cycleWidth by remember(articles) { mutableIntStateOf(0) }
    LaunchedEffect(articles, cycleWidth) {
        if (cycleWidth <= 0) return@LaunchedEffect
        while (true) {
            scrollState.scrollTo(0)
            delay(1_000L)
            val durationMillis = ((cycleWidth / density) / 28f * 1_000f).roundToInt().coerceAtLeast(8_000)
            scrollState.animateScrollTo(
                value = cycleWidth,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
            )
        }
    }
    Row(
        modifier = modifier.clipToBounds().horizontalScroll(scrollState, enabled = false),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArticleTickerCycle(
            articles = articles,
            onArticleClick = onArticleClick,
            modifier = Modifier.onSizeChanged { cycleWidth = it.width },
        )
        ArticleTickerCycle(articles = articles, onArticleClick = onArticleClick)
    }
}

@Composable
private fun ArticleTickerCycle(
    articles: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        articles.forEach { article ->
            Text(
                text = article.title,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onArticleClick(article) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
            )
            Text("·", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun EconomyNewsSection(
    state: HomeNewsUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        val visibleArticles = state.economyPageArticles
        if (visibleArticles.isEmpty()) {
            Text(
                text =
                    stringResource(
                        if (state.isLoading) R.string.home_news_loading else R.string.home_news_unavailable,
                    ),
                modifier = Modifier.padding(vertical = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        } else {
            visibleArticles.forEachIndexed { index, article ->
                EconomyArticleRow(article = article, onClick = { onArticleClick(article) })
                if (index != visibleArticles.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }
            }
        }
        if (state.economyPageCount > 1) {
            NewsPagination(
                currentPage = state.economyPage,
                pageCount = state.economyPageCount,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
            )
        }
    }
}

@Composable
private fun NewsPagination(currentPage: Int, pageCount: Int, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        IconButton(onClick = onPreviousPage, enabled = currentPage > 0, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.home_news_previous_page),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.home_news_page, currentPage + 1, pageCount),
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onNextPage, enabled = currentPage + 1 < pageCount, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.home_news_next_page),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EconomyArticleRow(article: NewsArticle, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (article.thumbnailUrl != null) {
            AsyncImage(
                model = article.thumbnailUrl,
                contentDescription = stringResource(R.string.home_news_thumbnail, article.title),
                modifier =
                    Modifier
                        .size(width = 50.dp, height = 40.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(width = 50.dp, height = 40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = article.title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            article.publishedAt?.let { publishedAt ->
                Text(
                    text = formatNewsPublishedAt(publishedAt, locale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun formatNewsPublishedAt(publishedAt: Instant, locale: Locale): String {
    val pattern = if (locale.language == Locale.KOREAN.language) "M월 d일 a h:mm" else "MMM d, h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale)
        .format(publishedAt.atZone(ZoneId.of("Asia/Seoul")))
}
