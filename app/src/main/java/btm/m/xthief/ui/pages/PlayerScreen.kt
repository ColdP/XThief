// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import btm.m.xthief.ui.TopBarMaterial
import btm.m.xthief.ui.components.GlassFloatingToolbar
import btm.m.xthief.ui.components.GlassToolbarIconButton
import btm.m.xthief.ui.components.LocalPageBackdrop

/** What the player is showing. */
data class PlayerSource(
    val uri: String,
    val title: String,
    val subtitle: String,
    /** MediaStore file name when opened from the library, so delete can act on it. */
    val mediaFileName: String? = null,
)

/**
 * Width and height of the floating bottom navigation bar, reused verbatim so the player's bars line
 * up with the app's navigation instead of being sized by eye.
 */
private const val NAV_WIDTH_FRACTION = (0.53f * 0.82f * 1.10f) * 2f
private val NAV_HEIGHT = 64.dp

/** Transport capsule: 87% of the previous height, per review. */
private val CARD_HEIGHT = 56.dp
private val CARD_SCRUB_HEIGHT = 80.dp

/** Icon and label metrics taken from the navigation bar's own tab content. */
private val NAV_ICON_SIZE = 28.dp
private val NAV_LABEL_SIZE = 10.5.sp

/**
 * The built-in player.
 *
 * The surface is a [TextureView] rather than a SurfaceView: a SurfaceView has its own compositor
 * layer, so the page backdrop could not sample the video drawn behind the glass bars.
 */
@Composable
fun PlayerScreen(
    source: PlayerSource,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenWith: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val scroll = MiuixScrollBehavior()
    val surface = MiuixTheme.colorScheme.surface

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var drawSurface by remember { mutableStateOf<Surface?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var scrubMs by remember { mutableLongStateOf(0L) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var controlsVisible by remember { mutableStateOf(true) }

    DisposableEffect(source.uri) {
        val media = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            )
            setDataSource(context, Uri.parse(source.uri))
            setOnPreparedListener { prepared ->
                durationMs = prepared.duration.toLong().coerceAtLeast(0L)
                prepared.start()
                isPlaying = true
            }
            setOnCompletionListener { isPlaying = false }
            prepareAsync()
        }
        player = media
        onDispose {
            runCatching { media.release() }
            player = null
            drawSurface = null
        }
    }

    LaunchedEffect(player, drawSurface) {
        val media = player ?: return@LaunchedEffect
        val target = drawSurface ?: return@LaunchedEffect
        runCatching { media.setSurface(target) }
    }

    // Playback clock; paused while scrubbing so the thumb follows the finger.
    LaunchedEffect(player, isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing) {
            player?.let { positionMs = it.currentPosition.toLong() }
            delay(200)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Publish this page's backdrop so the shared glass components (GlassFloatingToolbar,
        // GlassToolbarIconButton) sample the video instead of falling back to a flat tint.
        CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        // Recorded once here; the glass bars below sample this backdrop.
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                texture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                drawSurface = Surface(texture)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                texture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) = Unit

                            override fun onSurfaceTextureDestroyed(
                                texture: SurfaceTexture,
                            ): Boolean {
                                drawSurface = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }

        // Gestures: tap toggles the chrome, double tap plays/pauses, long press runs at 2x, a
        // horizontal drag scrubs and a pinch zooms (panning only once zoomed in).
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = { togglePlayback(player) { isPlaying = it } },
                        onLongPress = { applySpeed(player, 2f) },
                        onPress = {
                            tryAwaitRelease()
                            player?.let {
                                if (it.playbackParams.speed != 1f) applySpeed(player, 1f)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scrubMs = positionMs
                            isScrubbing = true
                        },
                        onDragEnd = {
                            player?.seekTo(scrubMs.toInt())
                            positionMs = scrubMs
                            isScrubbing = false
                        },
                        onDragCancel = { isScrubbing = false },
                        onHorizontalDrag = { _, dragAmount ->
                            val span = size.width.coerceAtLeast(1).toFloat()
                            scrubMs = (scrubMs + (dragAmount / span * durationMs).toLong())
                                .coerceIn(0L, durationMs)
                        },
                    )
                },
        )

        if (controlsVisible) {
            // Reuses the shared top-bar material. progress = 1 keeps it collapsed by default, so it
            // shows the progressive blur and liquid-glass buttons straight away.
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                TopBarMaterial(
                    backdrop = backdrop,
                    surface = surface,
                    progress = 1f,
                    tintedWhenExpanded = false,
                )
                TopAppBar(
                    title = source.title,
                    subtitle = source.subtitle,
                    color = Color.Transparent,
                    scrollBehavior = scroll,
                    navigationIcon = {
                        GlassToolbarIconButton(
                            icon = MiuixIcons.Regular.ChevronBackward,
                            description = "返回",
                            backdrop = backdrop,
                            collapsedFraction = 1f,
                            containerPadding = PaddingValues(start = 8.dp),
                            onClick = onBack,
                        )
                    },
                )
            }

            PlayerBottomBars(
                isPlaying = isPlaying,
                isMuted = isMuted,
                favorite = favorite,
                positionMs = if (isScrubbing) scrubMs else positionMs,
                durationMs = durationMs,
                scrubbing = isScrubbing,
                onTogglePlay = { togglePlayback(player) { isPlaying = it } },
                onToggleMute = {
                    val next = !isMuted
                    isMuted = next
                    val volume = if (next) 0f else 1f
                    runCatching { player?.setVolume(volume, volume) }
                },
                onToggleFavorite = onToggleFavorite,
                onOpenWith = onOpenWith,
                onInfo = onInfo,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        }
    }
}

private fun togglePlayback(player: MediaPlayer?, onChanged: (Boolean) -> Unit) {
    val media = player ?: return
    runCatching {
        if (media.isPlaying) {
            media.pause()
            onChanged(false)
        } else {
            media.start()
            onChanged(true)
        }
    }
}

private fun applySpeed(player: MediaPlayer?, value: Float) {
    val media = player ?: return
    runCatching { media.playbackParams = media.playbackParams.setSpeed(value) }
}

/**
 * The transport capsule and the actions toolbar. Both are the project's [GlassFloatingToolbar], so
 * their material and capsule shape are identical; only their height and content differ.
 */
@Composable
private fun PlayerBottomBars(
    isPlaying: Boolean,
    isMuted: Boolean,
    favorite: Boolean,
    positionMs: Long,
    durationMs: Long,
    scrubbing: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenWith: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val cardHeight by animateDpAsState(
            targetValue = if (scrubbing) CARD_SCRUB_HEIGHT else CARD_HEIGHT,
            animationSpec = tween(220),
            label = "playerCardHeight",
        )
        GlassFloatingToolbar(
            modifier = Modifier.fillMaxWidth(NAV_WIDTH_FRACTION).height(cardHeight),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                        if (isPlaying) "暂停" else "播放",
                        Modifier.size(NAV_ICON_SIZE),
                    )
                }
                // Elapsed time, right-aligned against the start of the bar; scrubbing only.
                AnimatedVisibility(
                    visible = scrubbing,
                    enter = fadeIn(tween(200)) + expandHorizontally(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkHorizontally(tween(180)),
                ) {
                    Text(
                        text = formatTime(positionMs),
                        style = MiuixTheme.textStyles.footnote2,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Box(Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = if (durationMs > 0L) {
                            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        colors = ProgressIndicatorDefaults.progressIndicatorColors(
                            Color.White,
                            Color.White.copy(alpha = .45f),
                        ),
                        height = 6.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AnimatedVisibility(
                    visible = scrubbing,
                    enter = fadeIn(tween(200)) + expandHorizontally(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkHorizontally(tween(180)),
                ) {
                    Text(
                        text = formatTime(durationMs),
                        style = MiuixTheme.textStyles.footnote2,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                IconButton(onClick = onToggleMute) {
                    Icon(
                        if (isMuted) {
                            MiuixIcons.Regular.VolumeOff
                        } else {
                            MiuixIcons.Regular.VolumeUp
                        },
                        if (isMuted) "取消静音" else "静音",
                        Modifier.size(NAV_ICON_SIZE),
                    )
                }
            }
        }

        // Same width, height, icon size and label metrics as the floating navigation bar.
        GlassFloatingToolbar(
            modifier = Modifier.fillMaxWidth(NAV_WIDTH_FRACTION).height(NAV_HEIGHT),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerToolbarItem(
                    icon = if (favorite) {
                        MiuixIcons.Regular.FavoritesFill
                    } else {
                        MiuixIcons.Regular.Favorites
                    },
                    label = "收藏",
                    onClick = onToggleFavorite,
                )
                PlayerToolbarItem(MiuixIcons.Regular.Forward, "打开为", onOpenWith)
                PlayerToolbarItem(MiuixIcons.Regular.Info, "信息", onInfo)
                PlayerToolbarItem(MiuixIcons.Regular.Delete, "删除", onDelete)
            }
        }
    }
}

/** Icon above label, at the floating navigation bar's metrics. */
@Composable
private fun PlayerToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Icon(icon, label, Modifier.size(NAV_ICON_SIZE))
        Text(
            text = label,
            fontSize = NAV_LABEL_SIZE,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** mm:ss, or h:mm:ss for long clips. */
private fun formatTime(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
