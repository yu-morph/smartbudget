package com.yu_morph.smartbudget.ui.document

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yu_morph.smartbudget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock

    data class Paragraph(val text: String) : MarkdownBlock

    data class Bullet(val text: String) : MarkdownBlock

    data object Divider : MarkdownBlock
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownViewerScreen(document: MarkdownDocument, onBack: () -> Unit) {
    val context = LocalContext.current
    var content by remember(document) { mutableStateOf<String?>(null) }
    var loadFailed by remember(document) { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(document) {
        runCatching {
            withContext(Dispatchers.IO) {
                context.assets.open(document.assetName).bufferedReader().use { it.readText() }
            }
        }.onSuccess { content = it }
            .onFailure { loadFailed = true }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(document.titleResource),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        when {
            loadFailed -> Text(
                text = stringResource(R.string.markdown_document_load_error),
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )

            content != null -> MarkdownContent(
                markdown = content.orEmpty(),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun MarkdownContent(markdown: String, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        parseMarkdown(markdown).forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownHeading(block)

                is MarkdownBlock.Paragraph -> Text(
                    text = cleanInlineMarkdown(block.text),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )

                is MarkdownBlock.Bullet -> Row(modifier = Modifier.fillMaxWidth()) {
                    Text("•", modifier = Modifier.padding(end = 10.dp), fontWeight = FontWeight.Bold)
                    Text(cleanInlineMarkdown(block.text), fontSize = 15.sp, lineHeight = 23.sp)
                }

                MarkdownBlock.Divider -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun MarkdownHeading(block: MarkdownBlock.Heading) {
    val fontSize = when (block.level) {
        1 -> 26.sp
        2 -> 21.sp
        else -> 17.sp
    }
    Text(
        text = cleanInlineMarkdown(block.text),
        modifier = Modifier.padding(top = if (block.level == 1) 0.dp else 12.dp),
        fontSize = fontSize,
        lineHeight = fontSize * 1.3,
        fontWeight = FontWeight.Bold,
        fontStyle = if (block.level > 3) FontStyle.Italic else FontStyle.Normal,
    )
}

private fun parseMarkdown(markdown: String): List<MarkdownBlock> = buildList {
    val paragraph = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            add(MarkdownBlock.Paragraph(paragraph.joinToString(" ")))
            paragraph.clear()
        }
    }

    markdown.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.isEmpty() -> flushParagraph()

            line == "---" || line == "***" -> {
                flushParagraph()
                add(MarkdownBlock.Divider)
            }

            line.startsWith("#") -> {
                flushParagraph()
                val level = line.takeWhile { it == '#' }.length.coerceAtMost(6)
                add(MarkdownBlock.Heading(level, line.drop(level).trim()))
            }

            line.startsWith("- ") || line.startsWith("* ") -> {
                flushParagraph()
                add(MarkdownBlock.Bullet(line.drop(2).trim()))
            }

            else -> paragraph += line
        }
    }
    flushParagraph()
}

private fun cleanInlineMarkdown(text: String): String = text
    .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1 ($2)")
    .replace("**", "")
    .replace("__", "")
    .replace("`", "")
