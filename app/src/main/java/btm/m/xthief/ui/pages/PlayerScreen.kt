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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
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

/** Transport capsule: 90% of the previous collapsed height. */
private val CARD_HEIGHT = 50.dp
private val CARD_SCRUB_HEIGHT = 80.dp

/** Rounded rectangle while scrubbing, capsule while collapsed. */
private val SCRUB_CARD_RADIUS = 22.5.dp

/** Icon and label metrics taken from the navigation bar's own tab content. */
private val NAV_ICON_SIZE = 28.dp
private val NAV_LABEL_SIZE = 10.5.sp

/** Transport buttons are 80% of that icon size. */
private val TRANSPORT_ICON_SIZE = NAV_ICON_SIZE * 0.8f

/**
 * Light frosted scrim for the player's glass bars. The shared default is the theme surface at 80%
 * alpha, which reads as a flat block over video; 42% white keeps the backdrop visible while the
 * black progress bar stays legible on top of it.
 */
private val PLAYER_GLASS_TINT = Color.White.copy(alpha = .42f)

/** Progress bar: black at 80% over a black 15% track. */
private val PROGRESS_FOREGROUND = Color.Black.copy(alpha = .80f)
private val PROGRESS_TRACK = Color.Black.copy(alpha = .15f)

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
            // Recorded once here; the glass bars above sample this backdrop.
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

            // Gestures on the video itself: tap toggles the chrome, double tap plays/pauses, long
            // press runs at 2x, a pinch zooms. Scrubbing lives on the progress bar.
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
                    },
            )

            if (controlsVisible) {
                // Shared top-bar material. progress = 1 keeps it collapsed by default; the small
                // top app bar is the collapsed layout, so it never expands over the video.
                Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                    TopBarMaterial(
                        backdrop = backdrop,
                        surface = surface,
                        progress = 1f,
                        tintedWhenExpanded = false,
                    )
                    SmallTopAppBar(
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
                    onScrubStart = {
                        scrubMs = positionMs
                        isScrubbing = true
                    },
                    onScrub = { fraction ->
                        scrubMs = (fraction * durationMs).toLong().coerceIn(0L, durationMs)
                    },
                    onScrubEnd = {
                        player?.seekTo(scrubMs.toInt())
                        positionMs = scrubMs
                        isScrubbing = false
                    },
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
 * their material is identical; only their height, shape and content differ.
 *
 * While scrubbing the transport capsule grows, drops the play/mute buttons so the bar can run edge
 * to edge, turns into a rounded rectangle, and puts both timestamps underneath the bar.
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
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
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
            surfaceTint = PLAYER_GLASS_TINT,
            cornerRadius = if (scrubbing) SCRUB_CARD_RADIUS else 50.dp,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The buttons give way to the bar while scrubbing.
                    AnimatedVisibility(visible = !scrubbing) {
                        IconButton(onClick = onTogglePlay) {
                            Icon(
                                if (isPlaying) {
                                    MiuixIcons.Regular.Pause
                                } else {
                                    MiuixIcons.Regular.Play
                                },
                                if (isPlaying) "暂停" else "播放",
                                Modifier.size(TRANSPORT_ICON_SIZE),
                                tint = Color.Black,
                            )
                        }
                    }
                    ScrubBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onScrubStart = onScrubStart,
                        onScrub = onScrub,
                        onScrubEnd = onScrubEnd,
                        modifier = Modifier.weight(1f),
                    )
                    AnimatedVisibility(visible = !scrubbing) {
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                if (isMuted) {
                                    MiuixIcons.Regular.VolumeOff
                                } else {
                                    MiuixIcons.Regular.VolumeUp
                                },
                                if (isMuted) "取消静音" else "静音",
                                Modifier.size(TRANSPORT_ICON_SIZE),
                                tint = Color.Black,
                            )
                        }
                    }
                }
                // Both timestamps sit under the bar, and only while scrubbing.
                AnimatedVisibility(
                    visible = scrubbing,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(120)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatTime(positionMs),
                            style = MiuixTheme.textStyles.footnote2,
                            color = Color.Black,
                            textAlign = TextAlign.Start,
                        )
                        Text(
                            text = formatTime(durationMs),
                            style = MiuixTheme.textStyles.footnote2,
                            color = Color.Black,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }

        // Same width, height, icon size and label metrics as the floating navigation bar.
        GlassFloatingToolbar(
            modifier = Modifier.fillMaxWidth(NAV_WIDTH_FRACTION).height(NAV_HEIGHT),
            surfaceTint = PLAYER_GLASS_TINT,
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

/** Progress bar that can be dragged (or tapped) to seek. */
@Composable
private fun ScrubBar(
    positionMs: Long,
    durationMs: Long,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(32.dp)
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        onScrubStart()
                        onScrub((start.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                    },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() },
                    onHorizontalDrag = { change, _ ->
                        onScrub(
                            (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                        )
                    },
                )
            }
            .pointerInput(durationMs) {
                if (durationMs <= 0L) return@pointerInput
                detectTapGestures { offset ->
                    onScrubStart()
                    onScrub((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                    onScrubEnd()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(
            progress = if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            } else {
                0f
            },
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                PROGRESS_FOREGROUND,
                PROGRESS_TRACK,
            ),
            height = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        )
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
        Icon(icon, label, Modifier.size(NAV_ICON_SIZE), tint = Color.Black)
        Text(
            text = label,
            fontSize = NAV_LABEL_SIZE,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
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
