// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import btm.m.xthief.BuildConfig
import btm.m.xthief.R
import btm.m.xthief.ui.DetailPage
import btm.m.xthief.ui.components.ActionRow
import btm.m.xthief.ui.components.HyperCeilerBrandView
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Year

private data class OpenProject(
    val name: String,
    val version: String,
    val description: String,
    val url: String,
    val license: String = "Apache License 2.0",
)

private val OPEN_PROJECTS = listOf(
    OpenProject(
        "MIUIX",
        "0.9.3",
        "HyperOS 风格界面、偏好设置、图标与模糊效果",
        "https://github.com/compose-miuix-ui/miuix",
    ),
    OpenProject(
        "Backdrop / AndroidLiquidGlass",
        "2.0.0",
        "液态玻璃渲染与底部导航交互",
        "https://github.com/Kyant0/AndroidLiquidGlass",
    ),
    OpenProject(
        "Compose Multiplatform",
        "1.11.x",
        "声明式界面、布局与动画",
        "https://github.com/JetBrains/compose-multiplatform",
    ),
    OpenProject(
        "AndroidX",
        "多个组件",
        "Activity、Lifecycle、Core 等 Android 基础库",
        "https://github.com/androidx/androidx",
    ),
)

@Composable
private fun aboutCardColors() = CardDefaults.defaultColors(
    MiuixTheme.colorScheme.surfaceContainer.copy(
        alpha = if (MiuixTheme.colorScheme.surface.luminance() < .5f) .60f else .65f,
    ),
)

/** Body of the About page: brand header, profile card, links and the project list. */
fun LazyListScope.aboutContent(
    onOpenUrl: (String) -> Unit,
    onOpenPage: (DetailPage) -> Unit,
) {
    item { AboutBrandHeader() }
    item {
        Card(Modifier.fillMaxWidth(), cornerRadius = 22.5.dp, colors = aboutCardColors()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrl("https://btm-m.site") }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.btm_m_avatar),
                    contentDescription = "btm_m",
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("btm_m", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
                    Text(
                        "https://btm-m.site",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                ChevronForwardIcon()
            }
        }
    }
    item {
        Card(Modifier.fillMaxWidth(), cornerRadius = 22.5.dp, colors = aboutCardColors()) {
            ActionRow(title = "LICENSE", subtitle = "Apache License 2.0", onClick = { onOpenPage(DetailPage.LICENSE) })
            ActionRow(
                title = "GitHub 仓库",
                subtitle = "https://github.com/ColdP/XThief",
                onClick = { onOpenUrl("https://github.com/ColdP/XThief") },
            )
            ActionRow(title = "开源代码声明", subtitle = "本项目使用的开源项目", onClick = { onOpenPage(DetailPage.OPEN_SOURCE) })
            ActionRow(title = "本项目基于 MIUIX 构建", subtitle = "compose-miuix-ui.github.io/miuix", onClick = { onOpenUrl("https://compose-miuix-ui.github.io/miuix/") })
            ActionRow(title = "Backdrop", subtitle = "液态玻璃与实时模糊材质", onClick = { onOpenUrl("https://github.com/Kyant0/Backdrop") })
        }
    }
    item {
        Card(Modifier.fillMaxWidth(), cornerRadius = 22.5.dp, colors = aboutCardColors()) {
            ActionRow(title = "用户协议", onClick = { onOpenPage(DetailPage.LICENSE) })
            ActionRow(title = "免责声明", onClick = { onOpenPage(DetailPage.LICENSE) })
            ActionRow(title = "隐私政策", onClick = { onOpenPage(DetailPage.LICENSE) })
        }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.5.dp,
            insideMargin = PaddingValues(16.dp),
            colors = aboutCardColors(),
        ) {
            Text(
                "XThief 通过 X（Twitter）的公开接口解析视频直链并保存到本地。请仅下载你拥有保存权限的内容，并遵守 X 的服务条款与当地法律。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.5.dp,
            insideMargin = PaddingValues(16.dp),
            colors = aboutCardColors(),
        ) {
            Text("Apache License 2.0", style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
            Text(
                "Copyright 2026 btm_m",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                "XThief 以 Apache License 2.0 授权发布。你可以在遵守该许可证的前提下使用、修改和再分发本项目；分发时需保留版权声明、许可证副本，并标注修改内容。本软件按「原样」提供，不附带任何明示或暗示的担保。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
    val projects = OPEN_PROJECTS
    items(projects.size) { index ->
        val project = projects[index]
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.5.dp,
            insideMargin = PaddingValues(16.dp),
            colors = aboutCardColors(),
            onClick = { onOpenUrl(project.url) },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
                    Text(
                        "${project.version} · ${project.license}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    Text(
                        project.description,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                ChevronForwardIcon()
            }
        }
    }
    item {
        Text(
            "© ${Year.now().value} btm_m",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = .56f),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/**
 * Brand header built on HyperChanger's [HyperCeilerBrandView]: the logo and wordmark are MIUI
 * blend views, so they receive the same advanced-material treatment (MIUI view blur plus
 * background blend colours) that HyperChanger's about page uses.
 */
@Composable
private fun AboutBrandHeader() {
    val density = LocalDensity.current
    val dark = MiuixTheme.colorScheme.surface.luminance() < .5f
    val versionColor = MiuixTheme.colorScheme.onSurface.copy(alpha = .78f).toArgb()
    BoxWithConstraints(
        // 86% of the original header height, with headroom for the upward optical offset.
        Modifier
            .fillMaxWidth()
            .height(310.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        val width = maxWidth - 18.dp
        val logoSize = width * (.94f / 3.91f) * 2.78f * .8f * .95f * .74f * 1.25f * .87f
        val textWidth = width * (2.97f / 3.91f) * .82f * .85f * .92f
        AndroidView(
            factory = { context -> HyperCeilerBrandView(context) },
            update = { view ->
                view.configure(
                    with(density) { logoSize.roundToPx() },
                    with(density) { textWidth.roundToPx() },
                    "版本 ${BuildConfig.VERSION_NAME}",
                    versionColor,
                    dark,
                    { },
                    { },
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ChevronForwardIcon() {
    Image(
        imageVector = MiuixIcons.Regular.ChevronForward,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantSummary),
    )
}
