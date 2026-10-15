// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import btm.m.xthief.data.LIBRARY_VIEW_GRID
import btm.m.xthief.data.LIBRARY_VIEW_LIST
import btm.m.xthief.data.MediaEntry
import btm.m.xthief.data.ThumbnailCache
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.components.EmptyState
import btm.m.xthief.ui.components.GlassFloatingToolbar
import btm.m.xthief.ui.components.GlassFloatingToolbarRow
import btm.m.xthief.ui.components.GlassToolbarIconButton
import btm.m.xthief.ui.components.SensitiveBadge
import btm.m.xthief.ui.components.TOOLBAR_MAX_BLUR
import btm.m.xthief.ui.components.ToolbarAnimationTiming
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.fileSizeLabel
import btm.m.xthief.ui.timeLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.state.ToggleableState

@Composable
fun LibraryPage(
    entries: List<MediaEntry>,
    directory: String,
    onRefresh: () -> Unit,
    onOpen: (MediaEntry) -> Unit,
    onShare: (MediaEntry) -> Unit,
    onDelete: (MediaEntry) -> Unit,
    onDeleteAll: (List<MediaEntry>) -> Unit = {},
    /**
     * Host-driven visibility of the floating toolbar. It is delayed until the bottom navigation
     * has finished leaving the screen, so the two never overlap.
     */
    toolbarVisible: Boolean = false,
    /** Lets the host hide the bottom navigation while the selection toolbar is up. */
    onSelectionModeChange: (Boolean) -> Unit = {},
    /** LIBRARY_VIEW_LIST or LIBRARY_VIEW_GRID. */
    viewMode: String = LIBRARY_VIEW_LIST,
    onToggleViewMode: () -> Unit = {},
    /** Blurs the cover of entries whose source post was flagged sensitive. */
    blurSensitiveCovers: Boolean = true,
    /** Supplies the cover frames; null leaves the cards without artwork. */
    thumbnails: ThumbnailCache? = null,
) {
    // Multi-select state, mirroring HyperChanger's list pages.
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MediaEntry?>(null) }
    var actionTarget by remember { mutableStateOf<MediaEntry?>(null) }

    fun closeSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    // Refresh automatically on entry so changes made elsewhere show up without a manual tap.
    LaunchedEffect(Unit) { onRefresh() }

    // Report the selection state so the host can animate the bottom navigation away.
    LaunchedEffect(selectionMode) { onSelectionModeChange(selectionMode) }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChange(false) }
    }

    // Drop selections whose entries disappeared (e.g. after delete + refresh).
    LaunchedEffect(entries) {
        if (selectedIds.isNotEmpty()) {
            val alive = entries.mapTo(hashSetOf()) { it.uri }
            selectedIds = selectedIds intersect alive
        }
    }

    BackHandler(enabled = selectionMode) { closeSelection() }

    AppPage(
        title = "媒体库",
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
        floatingToolbar = {
            AnimatedVisibility(
                visible = toolbarVisible,
                enter = fadeIn(tween(ToolbarAnimationTiming.ENTER_MS)) +
                    scaleIn(tween(ToolbarAnimationTiming.ENTER_MS), initialScale = .86f),
                exit = fadeOut(tween(ToolbarAnimationTiming.EXIT_MS)) +
                    scaleOut(tween(ToolbarAnimationTiming.EXIT_MS), targetScale = .9f),
            ) {
                // Blur-in alongside the fade: the capsule resolves out of a blur as it appears.
                val blurProgress by animateFloatAsState(
                    targetValue = if (toolbarVisible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = ToolbarAnimationTiming.ENTER_MS,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "toolbarBlur",
                )
                val blurPx = with(LocalDensity.current) {
                    (TOOLBAR_MAX_BLUR * (1f - blurProgress.coerceIn(0f, 1f))).toPx()
                }
                Box(
                    Modifier.graphicsLayer {
                        alpha = blurProgress.coerceIn(0f, 1f)
                        renderEffect =
                            if (blurPx > .1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                BlurEffect(blurPx, blurPx, TileMode.Clamp)
                            } else {
                                null
                            }
                    },
                ) {
                    GlassFloatingToolbar {
                        GlassFloatingToolbarRow {
                            IconButton(onClick = {
                                selectedIds = if (selectedIds.size == entries.size) {
                                    emptySet()
                                } else {
                                    entries.mapTo(linkedSetOf()) { it.uri }
                                }
                            }) {
                                Icon(
                                    MiuixIcons.Regular.SelectAll,
                                    "全选",
                                    Modifier.size(26.4.dp),
                                )
                            }
                            IconButton(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = { confirmBatchDelete = true },
                            ) {
                                Icon(
                                    MiuixIcons.Regular.Delete,
                                    "删除",
                                    Modifier.size(26.4.dp),
                                    tint = Color(0xFFD32F2F),
                                )
                            }
                            IconButton(onClick = { closeSelection() }) {
                                Icon(
                                    MiuixIcons.Regular.Close,
                                    "关闭",
                                    Modifier.size(26.4.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = !selectionMode,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = .9f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = .9f),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val grid = viewMode == LIBRARY_VIEW_GRID
                    // GlassToolbarIconButton is the same capsule the About page's back button uses:
                    // its liquid-glass backdrop fades in as the top bar collapses.
                    GlassToolbarIconButton(
                        // The icon announces what tapping switches to.
                        icon = if (grid) {
                            MiuixIcons.Regular.ListView
                        } else {
                            MiuixIcons.Regular.GridView
                        },
                        description = if (grid) "切换为列表" else "切换为瀑布流",
                        onClick = onToggleViewMode,
                    )
                    GlassToolbarIconButton(
                        icon = MiuixIcons.Regular.Refresh,
                        description = "刷新",
                        onClick = onRefresh,
                    )
                }
            }
        },
        content = {
            if (viewMode != LIBRARY_VIEW_GRID) {
                if (entries.isEmpty()) {
                    item {
                        EmptyLibraryState(directory, MiuixIcons.Regular.Photos)
                    }
                } else {
                    items(entries, key = { it.uri }) { entry ->
                        MediaCard(
                            entry = entry,
                            selectionMode = selectionMode,
                            selected = entry.uri in selectedIds,
                            thumbnails = thumbnails,
                            blurCover = blurSensitiveCovers && entry.sensitive,
                            onClick = {
                                if (selectionMode) toggleSelection(entry.uri) else actionTarget = entry
                            },
                            onLongPress = {
                                if (selectionMode) {
                                    toggleSelection(entry.uri)
                                } else {
                                    selectionMode = true
                                    selectedIds = selectedIds + entry.uri
                                }
                            },
                            onToggle = { toggleSelection(entry.uri) },
                        )
                    }
                }
            }
        },
        staggeredContent = if (viewMode == LIBRARY_VIEW_GRID) {
            {
                if (entries.isEmpty()) {
                    item {
                        EmptyLibraryState(directory, MiuixIcons.Regular.Photos)
                    }
                } else {
                    staggeredItems(entries, key = { it.uri }) { entry ->
                        MediaCoverCard(
                            entry = entry,
                            selectionMode = selectionMode,
                            selected = entry.uri in selectedIds,
                            thumbnails = thumbnails,
                            blurCover = blurSensitiveCovers && entry.sensitive,
                            onClick = {
                                if (selectionMode) toggleSelection(entry.uri) else actionTarget = entry
                            },
                            onLongPress = {
                                if (selectionMode) {
                                    toggleSelection(entry.uri)
                                } else {
                                    selectionMode = true
                                    selectedIds = selectedIds + entry.uri
                                }
                            },
                            onToggle = { toggleSelection(entry.uri) },
                        )
                    }
                }
            }
        } else {
            null
        },
    )

    actionTarget?.let { entry ->
        MediaActionDialog(
            title = entry.displayName,
            subtitle = buildString {
                append(fileSizeLabel(entry.sizeBytes))
                if (entry.width > 0 && entry.height > 0) {
                    append(" · ${minOf(entry.width, entry.height)}p")
                }
                if (entry.durationMs > 0) append(" · ${durationLabel(entry.durationMs)}")
            },
            onPlay = { actionTarget = null; onOpen(entry) },
            onShare = { actionTarget = null; onShare(entry) },
            onDelete = { actionTarget = null; pendingDelete = entry },
            onDismiss = { actionTarget = null },
        )
    }

    pendingDelete?.let { entry ->
        XThiefDialog(
            title = "删除视频",
            message = "「${entry.displayName}」将从设备存储中永久删除。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                onDelete(entry)
                pendingDelete = null
                onRefresh()
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (confirmBatchDelete) {
        val targets = entries.filter { it.uri in selectedIds }
        XThiefDialog(
            title = "删除 ${targets.size} 个视频",
            message = "所选视频将从设备存储中永久删除，此操作不可撤销。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                confirmBatchDelete = false
                onDeleteAll(targets)
                closeSelection()
                // Re-scan so the list reflects what is actually left on disk.
                onRefresh()
            },
            onDismiss = { confirmBatchDelete = false },
        )
    }
}

/** One library entry, using MIUIX's card padding and an animated selection checkbox. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCard(
    entry: MediaEntry,
    selectionMode: Boolean,
    selected: Boolean,
    thumbnails: ThumbnailCache?,
    blurCover: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.5.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListCover(entry = entry, thumbnails = thumbnails, blurCover = blurCover)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entry.sensitive) {
                        SensitiveBadge(Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    text = buildString {
                        append(fileSizeLabel(entry.sizeBytes))
                        if (entry.width > 0 && entry.height > 0) {
                            append(" · ${minOf(entry.width, entry.height)}p")
                        }
                        if (entry.durationMs > 0) append(" · ${durationLabel(entry.durationMs)}")
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = buildString {
                        append(timeLabel(entry.addedAt))
                        if (entry.directory.isNotBlank()) append(" · ${entry.directory.trimEnd('/')}")
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            AnimatedVisibility(
                visible = selectionMode,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { it / 2 },
            ) {
                Checkbox(
                    state = if (selected) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggle,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(directory: String, icon: ImageVector) {
    EmptyState(
        title = "媒体库是空的",
        description = "下载完成的视频会自动出现在这里。也可以点右上角刷新，重新扫描 $directory。",
        icon = {
            androidx.compose.foundation.Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        },
    )
}

/**
 * Two-column staggered card, modelled on the supplied design reference: the cover fills the width
 * at its own aspect ratio (so cards interleave naturally), and the file name plus
 * size · resolution · length sit on a gradient sampled from the artwork itself.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCoverCard(
    entry: MediaEntry,
    selectionMode: Boolean,
    selected: Boolean,
    thumbnails: ThumbnailCache?,
    blurCover: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
) {
    val cover by produceState<ImageBitmap?>(initialValue = null, entry.uri, thumbnails) {
        value = thumbnails?.cover(entry)?.asImageBitmap()
    }
    val scrim = remember(cover) {
        val source = cover
        val sampled = if (source != null && thumbnails != null) {
            thumbnails.scrimColorOf(source.asAndroidBitmap())
        } else {
            0xFF1C1B1F.toInt()
        }
        Color(sampled)
    }

    // The card's height follows the artwork's aspect ratio, so the frosted strip is sized from the
    // measured height: a landscape clip gets a short strip, a portrait one a taller strip.
    var cardHeightPx by remember(entry.uri) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bandHeight = with(density) {
        if (cardHeightPx > 0) {
            (cardHeightPx * BAND_HEIGHT_FRACTION).toDp().coerceIn(MIN_BAND_HEIGHT, MAX_BAND_HEIGHT)
        } else {
            FALLBACK_BAND_HEIGHT
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            // A fixed ratio per orientation keeps the staggered column predictable: landscape clips
            // are rendered 3:2 and portrait ones 3:4, whatever the source video's real shape.
            .aspectRatio(aspectRatioOf(entry))
            .clip(RoundedCornerShape(22.5.dp))
            .onSizeChanged { cardHeightPx = it.height }
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        if (cover != null) {
            // Sensitive covers use a render effect rather than Modifier.blur: blur() feathers its
            // edges, so a tall portrait card ends up only partly obscured.
            val sensitiveBlurPx = with(density) {
                if (blurCover) SENSITIVE_BLUR.toPx() else 0f
            }
            Image(
                bitmap = cover!!,
                contentDescription = entry.displayName,
                // The card is a fixed ratio, so the artwork is cropped to fill it.
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        renderEffect = if (
                            sensitiveBlurPx > .1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ) {
                            BlurEffect(sensitiveBlurPx, sensitiveBlurPx, TileMode.Clamp)
                        } else {
                            null
                        }
                    },
            )

            // Staggered frosting instead of one hard-edged block: each layer covers a wider slice of
            // the artwork with a lighter blur, so the image dissolves gradually into the tint.
            // Unbounded edge treatment lets the blur bleed outwards; the card's clip trims it, which
            // is what removes the visible seam.
            FROST_LAYERS.forEach { layer ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(bandHeight * layer.heightFactor)
                        .blur(layer.radius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                ) {
                    Image(
                        bitmap = cover!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        // Cropping from the bottom keeps the strip showing the same pixels as the
                        // full cover behind it, so the layers line up.
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (blurCover) {
                                    Modifier.graphicsLayer {
                                        renderEffect =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                val px = SENSITIVE_BLUR.toPx()
                                                BlurEffect(px, px, TileMode.Clamp)
                                            } else {
                                                null
                                            }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
                    .background(MiuixTheme.colorScheme.surfaceContainer),
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                // Same height as the frosting layers, so the tint and the blur end together.
                .height(bandHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.40f to scrim.copy(alpha = .45f),
                        0.70f to scrim.copy(alpha = .88f),
                        1f to scrim,
                    ),
                ),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                Text(
                    text = entry.displayName,
                    color = Color.White,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Bold,
                    // One line only: a landscape clip is short, and a second line would eat into it.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(fileSizeLabel(entry.sizeBytes))
                        if (entry.width > 0 && entry.height > 0) {
                            append(" · ${entry.width}×${entry.height}")
                        }
                        if (entry.durationMs > 0) append(" · ${durationLabel(entry.durationMs)}")
                    },
                    color = Color.White.copy(alpha = .95f),
                    style = MiuixTheme.textStyles.footnote2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (entry.sensitive) {
            SensitiveBadge(Modifier.align(Alignment.TopStart).padding(8.dp))
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = .8f),
            exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = .9f),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Checkbox(
                state = if (selected) ToggleableState.On else ToggleableState.Off,
                onClick = onToggle,
            )
        }
    }
}

/** Small square cover used by the list layout. */
@Composable
private fun ListCover(
    entry: MediaEntry,
    thumbnails: ThumbnailCache?,
    blurCover: Boolean,
) {
    val cover by produceState<ImageBitmap?>(initialValue = null, entry.uri, thumbnails) {
        value = thumbnails?.cover(entry)?.asImageBitmap()
    }
    Box(
        Modifier
            .size(width = 84.dp, height = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            val sensitiveBlurPx = with(LocalDensity.current) {
                if (blurCover) SENSITIVE_BLUR.toPx() else 0f
            }
            Image(
                bitmap = cover!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        renderEffect = if (
                            sensitiveBlurPx > .1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ) {
                            BlurEffect(sensitiveBlurPx, sensitiveBlurPx, TileMode.Clamp)
                        } else {
                            null
                        }
                    },
            )
        }
    }
}

private fun durationLabel(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Share of the card height taken by the frosted caption strip. */
private const val BAND_HEIGHT_FRACTION = 0.38f

/** Clamps so the strip stays legible on a short landscape card and never swallows a tall one. */
private val MIN_BAND_HEIGHT = 62.dp
private val MAX_BAND_HEIGHT = 96.dp

/** Used for the first frame, before the card has reported its measured height. */
private val FALLBACK_BAND_HEIGHT = 76.dp

/**
 * Card aspect ratios (width / height). Landscape clips are boxed to 3:2 and portrait ones to 3:4,
 * so the two-column layout keeps a consistent rhythm instead of following every video's own shape.
 */
private const val LANDSCAPE_ASPECT = 3f / 2f
private const val PORTRAIT_ASPECT = 3f / 4f

/** Portrait when the video is taller than it is wide. */
private fun aspectRatioOf(entry: MediaEntry): Float =
    if (entry.height > entry.width && entry.width > 0) PORTRAIT_ASPECT else LANDSCAPE_ASPECT

/** Blur applied to the cover of a sensitive entry, so the whole card is evenly obscured. */
private val SENSITIVE_BLUR = 26.dp

/**
 * Frosting layers, ordered widest/lightest to narrowest/strongest, so artwork blurs progressively
 * towards the caption instead of switching at a hard seam.
 */
private val FROST_LAYERS = listOf(
    FrostLayer(heightFactor = 1f, radius = 8.dp),
    FrostLayer(heightFactor = 0.66f, radius = 17.dp),
    FrostLayer(heightFactor = 0.34f, radius = 26.dp),
)

private data class FrostLayer(val heightFactor: Float, val radius: Dp)
