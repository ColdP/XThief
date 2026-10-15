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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
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
import btm.m.xthief.ui.components.GlassFloatingToolbar
import btm.m.xthief.ui.components.GlassToolbarIconButton

/** What the player is showing. */
data class PlayerSource(
    val uri: String,
    val title: String,
    val subtitle: String,
    /** MediaStore file name when opened from the library, so delete can act on it. */
    val mediaFileName: String? = null,
)

private val TOP_BAR_HEIGHT = 56.dp
private val TOP_SCRIM_HEIGHT = 136.dp
private val TOP_BLUR = 26.dp
private val CARD_SHAPE = RoundedCornerShape(22.5.dp)

/**
 * The built-in player.
 *
 * The surface is a [TextureView] rather than a SurfaceView on purpose: a SurfaceView lives in its
 * own compositor layer, so the page backdrop used by the glass bars could not sample the video
 * drawn underneath them.
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

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var surface by remember { mutableStateOf<Surface?>(null) }
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
            surface = null
        }
    }

    // Attach the drawing surface once both the player and the TextureView are ready.
    LaunchedEffect(player, surface) {
        val media = player ?: return@LaunchedEffect
        val target = surface ?: return@LaunchedEffect
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
        // The video is recorded exactly once here; the glass bars below sample this backdrop.
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
                                surface = Surface(texture)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                texture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) = Unit

                            override fun onSurfaceTextureDestroyed(
                                texture: SurfaceTexture,
                            ): Boolean {
                                surface = null
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
        // horizontal drag scrubs, and a pinch zooms (panning only once zoomed in).
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
                            // Restore normal speed whether or not the long press fired.
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
            PlayerTopBar(
                backdrop = backdrop,
                title = source.title,
                subtitle = source.subtitle,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            PlayerBottomBars(
                backdrop = backdrop,
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

/** Collapsed MIUIX title bar: a gradient blur scrim with liquid-glass actions floating on it. */
@Composable
private fun PlayerTopBar(
    backdrop: LayerBackdrop,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(TOP_SCRIM_HEIGHT)) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = { blur(TOP_BLUR.toPx()) },
                    highlight = null,
                    shadow = null,
                    innerShadow = null,
                    onDrawSurface = {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = .58f),
                                0.55f to Color.Black.copy(alpha = .22f),
                                1f to Color.Transparent,
                            ),
                        )
                    },
                ),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(TOP_BAR_HEIGHT)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassToolbarIconButton(
                icon = MiuixIcons.Regular.Back,
                description = "返回",
                onClick = onBack,
            )
            Column(Modifier.padding(start = 6.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = .78f),
                        style = MiuixTheme.textStyles.footnote2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The floating transport capsule plus the actions toolbar beneath it. */
@Composable
private fun PlayerBottomBars(
    backdrop: LayerBackdrop,
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The time labels extend the capsule while scrubbing, hence the animated height.
        val cardHeight by animateDpAsState(
            targetValue = if (scrubbing) 92.dp else 64.dp,
            animationSpec = tween(220),
            label = "playerCardHeight",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(CARD_SHAPE)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CARD_SHAPE },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = .55f) },
                    shadow = {
                        Shadow.Default.copy(radius = 14.dp, color = Color.Black, alpha = .40f)
                    },
                    innerShadow = null,
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = .34f)) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) MiuixIcons.Regular.Pause else MiuixIcons.Regular.Play,
                        if (isPlaying) "暂停" else "播放",
                        Modifier.size(28.dp),
                        tint = Color.White,
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
                        color = Color.White,
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
                            // Foreground white on a translucent track, per the player spec.
                            Color.White,
                            Color.White.copy(alpha = .30f),
                        ),
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
                        color = Color.White,
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
                        Modifier.size(28.dp),
                        tint = Color.White,
                    )
                }
            }
        }

        // Same capsule material and height as the home floating navigation bar.
        GlassFloatingToolbar {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
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

/** Icon above label, matching the bottom navigation's rhythm. */
@Composable
private fun PlayerToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(icon, label, Modifier.size(24.dp), tint = Color.White)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            style = MiuixTheme.textStyles.footnote2,
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
