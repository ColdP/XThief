# MIUIX 0.9.3 — 可用 API 清单

> **本文档由本地 AAR 字节码逆向生成，不是官方文档。**
>
> 数据来源与可信度：
>
> 1. **class 文件方法描述符**（精确）：JVM 参数类型、个数、静态性、返回类型。
> 2. **Compose `sourceInformation` 常量池字符串**（精确）：形如
>    `C(Scaffold)N(modifier,topBar,…,contentWindowInsets:c#ui.graphics.Color,content)…:Scaffold.kt`
>    的字符串由 Compose 编译器写入 class 文件，给出**全部 Kotlin 参数名与声明顺序**。
>    本清单中出现的参数名均取自它，属于字节码事实。
> 3. Kotlin `@Metadata` protobuf（辅助）：仅用于 Defaults / 主题等非 Composable 声明。
>
> 标注约定：
>
> - `字节码未给出` = 该位置的 Kotlin 参数名在字节码里取不到（无 sourceInformation，
>   且 `@Metadata` 未完全还原），此时**类型与顺序仍然精确**；
> - 类型名做了缩写：`androidx.compose.`、`top.yukonga.miuix.kmp.`、`kotlin.`、`com.kyant.` 前缀省略；
> - `FunctionN` = `kotlin.jvm.functions.FunctionN`（lambda 参数）；
> - JVM 方法名里的 `-XXXXXXXX` 后缀是 Kotlin inline class 名字修饰（mangling），
>   文档中已剥离，Kotlin 侧直接写整洁名。
>
> 依赖坐标：
>
> ```kotlin
> implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.3")
> implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.3")
> implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.3")
> implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.3")
> // 传递依赖，写代码时可能直接用到：
> // io.github.kyant0:backdrop:2.0.0
> // io.github.kyant0:shapes:1.2.0
> ```

## 0. 快速上手（可直接编译）

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixThemeController

@Composable
fun Demo() {
    val controller = remember { MiuixThemeController() }   // 返回 ThemeController
    MiuixTheme(controller = controller) {
        Scaffold(topBar = { TopAppBar(title = "标题") }) { padding ->
            Card(modifier = Modifier.padding(padding)) {
                Text(text = "Hello MIUIX", style = MiuixTheme.textStyles.body1)
            }
        }
    }
}
```

三个最容易踩的坑：

1. **`MiuixTheme` 有两个身份。** `top.yukonga.miuix.kmp.theme.MiuixTheme` 既是
   `@Composable fun MiuixTheme(controller, textStyles, content)`（主题注入，在 `MiuixThemeKt` 里），
   又是 `object MiuixTheme`（读取当前主题值的单例）。
   写入用 `MiuixTheme(controller) { … }`，读取用 `MiuixTheme.colorScheme` / `MiuixTheme.textStyles`。
2. **主题控制器构造入口叫 `MiuixThemeController`。** 实际类名是 `ThemeController`，
   公开工厂函数是 `MiuixThemeController(...)`。
3. **图标是扩展属性。** 写作 `MiuixIcons.Regular.Settings`，
   需要 `import top.yukonga.miuix.kmp.icon.extended.Settings`。

## 1. `top.yukonga.miuix.kmp.basic`

MIUIX 的组件主体。下面每个 `#### \`XxxKt\`` 小节里的 `static fun` 就是顶层
`@Composable`。**Composable 在 JVM 上尾部会多出 `Composer` 与若干 `Int`**
（Compose 的 `$changed`/`$default` 控制位），文档已剔除，调用时不要传。

本包共 **109** 个公开顶层类：`*Kt`（Composable 载体）33 个、
`Defaults`/`Colors`/状态与数据类 76 个。

### 1.1 组件（`*Kt`）

#### `BadgeKt`

- `val badgeEndRuler: ui.layout.VerticalRuler`
- `val badgeOffset: Float`
- `val badgeTopRuler: ui.layout.HorizontalRuler`
- `val badgeWithContentHorizontalOffset: Float`
- `val badgeWithContentHorizontalPadding: Float`
- `val badgeWithContentVerticalOffset: Float`
- `static fun BadgedBox(badge: Function3, modifier: Modifier, content: Function3)`
- `static fun Badge(modifier: Modifier, containerColor: ui.graphics.Color, contentColor: ui.graphics.Color, content: Function3)`
- `static fun badgeBounds(字节码未给出: Modifier): Modifier`

#### `ButtonKt`

- `static fun Button(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, cornerRadius: ui.unit.Dp, minWidth: ui.unit.Dp, minHeight: ui.unit.Dp, colors: basic.ButtonColors, insideMargin: foundation.layout.PaddingValues, interactionSource: foundation.interaction.MutableInteractionSource, indication: foundation.Indication, content: Function3)`
- `static fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, cornerRadius: ui.unit.Dp, minWidth: ui.unit.Dp, minHeight: ui.unit.Dp, colors: basic.TextButtonColors, insideMargin: foundation.layout.PaddingValues, textStyle: TextStyle, interactionSource: foundation.interaction.MutableInteractionSource, indication: foundation.Indication)`

#### `CardKt`

- `static fun Card(modifier: Modifier, cornerRadius: ui.unit.Dp, insideMargin: foundation.layout.PaddingValues, colors: basic.CardColors, content: Function3)`
- `static fun Card(modifier: Modifier, cornerRadius: ui.unit.Dp, insideMargin: foundation.layout.PaddingValues, colors: basic.CardColors, pressFeedbackType: utils.PressFeedbackType, showIndication: Boolean, holdDownState: Boolean, onClick: () -> Unit, onLongPress: () -> Unit, content: Function3)`

#### `CheckboxKt`

- `static fun Checkbox(state: ui.state.ToggleableState, onClick: () -> Unit, modifier: Modifier, colors: basic.CheckboxColors, enabled: Boolean)`

#### `ColorPaletteKt`

- `static fun ColorPalette-(字节码未给出: Long, 字节码未给出: Function1, 字节码未给出: Modifier, 字节码未给出: Int, 字节码未给出: Int, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Float, 字节码未给出: Float)`

#### `ColorPickerKt`

- `static fun ColorPicker(color: ui.graphics.Color, onColorChanged: Function1, modifier: Modifier, showPreview: Boolean, hapticEffect: basic.SliderDefaults.SliderHapticEffect, colorSpace: basic.ColorSpace)`
- `static fun HsvColorPicker(color: ui.graphics.Color, onColorChanged: Function1, modifier: Modifier, showPreview: Boolean, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun HsvHueSlider(currentHue: Float, onHueChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun HsvSaturationSlider(currentHue: Float, currentSaturation: Float, onSaturationChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun HsvValueSlider(currentHue: Float, currentSaturation: Float, currentValue: Float, onValueChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun HsvAlphaSlider(currentHue: Float, currentSaturation: Float, currentValue: Float, currentAlpha: Float, onAlphaChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkHsvColorPicker(color: ui.graphics.Color, onColorChanged: Function1, modifier: Modifier, showPreview: Boolean, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkHsvHueSlider(currentH: Float, onHueChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkHsvSaturationSlider(currentH: Float, currentS: Float, onSaturationChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkHsvValueSlider(currentH: Float, currentS: Float, currentV: Float, onValueChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkHsvAlphaSlider(currentH: Float, currentS: Float, currentV: Float, currentAlpha: Float, onAlphaChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLabColorPicker(color: ui.graphics.Color, onColorChanged: Function1, modifier: Modifier, showPreview: Boolean, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLchColorPicker(color: ui.graphics.Color, onColorChanged: Function1, modifier: Modifier, showPreview: Boolean, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLchLightnessSlider(currentL: Float, currentC: Float, currentH: Float, onLightnessChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLchChromaSlider(currentL: Float, currentC: Float, currentH: Float, onChromaChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLchHueSlider(currentL: Float, currentC: Float, currentH: Float, onHueChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLchAlphaSlider(currentL: Float, currentC: Float, currentH: Float, currentAlpha: Float, onAlphaChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLabLightnessSlider(currentL: Float, currentA: Float, currentB: Float, onLightnessChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLabAChannelSlider(currentL: Float, currentA: Float, currentB: Float, onAChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLabBChannelSlider(currentL: Float, currentA: Float, currentB: Float, onBChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun OkLabAlphaSlider(currentL: Float, currentA: Float, currentB: Float, currentAlpha: Float, onAlphaChanged: Function1, hapticEffect: basic.SliderDefaults.SliderHapticEffect)`
- `static fun drawCheckerboard(字节码未给出: Modifier, 字节码未给出: Float, 字节码未给出: Long, 字节码未给出: Long): Modifier`

#### `ComponentKt`

- `static fun BasicComponent(modifier: Modifier, title: String, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, startAction: Function2, endActions: Function3, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, onClick: () -> Unit, onClickLabel: String, role: ui.semantics.Role, holdDownState: Boolean, enabled: Boolean, interactionSource: foundation.interaction.MutableInteractionSource)`
- `static fun BasicComponent(modifier: Modifier, startAction: Function2, endActions: Function3, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, onClick: () -> Unit, onClickLabel: String, role: ui.semantics.Role, holdDownState: Boolean, enabled: Boolean, interactionSource: foundation.interaction.MutableInteractionSource, content: Function3)`

#### `DividerKt`

- `static fun HorizontalDivider(modifier: Modifier, thickness: ui.unit.Dp, color: ui.graphics.Color)`
- `static fun VerticalDivider(modifier: Modifier, thickness: ui.unit.Dp, color: ui.graphics.Color)`

#### `DropdownKt`

- `static fun DropdownArrowEndAction(actionColor: ui.graphics.Color, modifier: Long, 字节码未给出: Modifier)`
- `static fun DropdownImpl(item: basic.DropdownItem, optionSize: Int, isSelected: Boolean, index: Int, dropdownColors: basic.DropdownColors, enabled: Boolean, dialogMode: Boolean, hasSubmenu: Boolean, isFirst: Boolean, isLast: Boolean, onSelectedIndexChange: Function1)`
- `static fun DropdownImpl(text: String, optionSize: Int, isSelected: Boolean, index: Int, dropdownColors: basic.DropdownColors, enabled: Boolean, dialogMode: Boolean, onSelectedIndexChange: Function1)`
- `static fun SpinnerItemImpl(entry: basic.DropdownItem, entryCount: Int, isSelected: Boolean, index: Int, spinnerColors: basic.DropdownColors, dialogMode: Boolean, onSelectedIndexChange: Function1)`
- `static fun SpinnerColors$annotations()`
- `static fun SpinnerEntry$annotations()`

#### `FloatingActionButtonKt`

- `static fun FloatingActionButton(onClick: () -> Unit, modifier: Modifier, shape: ui.graphics.Shape, containerColor: ui.graphics.Color, shadowElevation: ui.unit.Dp, minWidth: ui.unit.Dp, minHeight: ui.unit.Dp, content: Function2)`

#### `FloatingToolbarKt`

- `static fun FloatingToolbar(modifier: Modifier, color: ui.graphics.Color, cornerRadius: ui.unit.Dp, outSidePadding: foundation.layout.PaddingValues, shadowElevation: ui.unit.Dp, showDivider: Boolean, content: Function2)`

#### `IconButtonKt`

- `static fun IconButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, holdDownState: Boolean, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`

#### `IconKt`

- `static fun Icon(imageVector: ui.graphics.vector.ImageVector, contentDescription: String, modifier: Modifier, tint: ui.graphics.Color)`
- `static fun Icon(imageVector: ui.graphics.ImageBitmap, contentDescription: String, modifier: Modifier, tint: ui.graphics.Color)`
- `static fun Icon(imageVector: ui.graphics.painter.Painter, contentDescription: String, modifier: Modifier, tint: ui.graphics.Color)`
- `static fun Icon(imageVector: ui.graphics.painter.Painter, contentDescription: ui.graphics.ColorProducer, modifier: String, tint: ui.graphics.Color)`

#### `ListPopupKt`

- `static fun ListPopupColumn(content: Function2)`
- `static fun safeTransformOrigin(字节码未给出: Float, 字节码未给出: Float): Long`
- `static fun rememberListPopupLayoutInfo(alignment: basic.PopupPositionProvider.Align, popupPositionProvider: basic.PopupPositionProvider, parentBounds: ui.unit.IntRect, popupContentSize: ui.unit.IntSize): basic.ListPopupLayoutInfo`
- `static fun ListPopupContent(popupContentSize: ui.unit.IntSize, onPopupContentSizeChange: Function1, fractionProgress: () -> Unit, alphaProgress: () -> Unit, popupLayoutPosition: basic.PopupLayoutPosition, localTransformOrigin: ui.graphics.TransformOrigin, modifier: Modifier, content: Function2)`
- `static fun popupClipReveal(字节码未给出: Modifier, 字节码未给出: () -> Unit, 字节码未给出: basic.PopupLayoutPosition, 字节码未给出: Float, 字节码未给出: Boolean): Modifier`

#### `NavigationBarKt`

- `val localNavigationBarDisplayMode: runtime.ProvidableCompositionLocal`
- `static fun NavigationBar(modifier: Modifier, color: ui.graphics.Color, showDivider: Boolean, defaultWindowInsetsPadding: Boolean, mode: basic.NavigationBarDisplayMode, content: Function3)`
- `static fun NavigationBarItem(selected: foundation.layout.RowScope, onClick: Boolean, icon: () -> Unit, label: ui.graphics.vector.ImageVector, modifier: String, enabled: Modifier, badge: Boolean, 字节码未给出: Function2)`
- `static fun FloatingNavigationBar(modifier: Modifier, color: ui.graphics.Color, cornerRadius: ui.unit.Dp, horizontalAlignment: ui.Alignment.Horizontal, horizontalOutSidePadding: ui.unit.Dp, shadowElevation: ui.unit.Dp, showDivider: Boolean, defaultWindowInsetsPadding: Boolean, content: Function2)`
- `static fun FloatingNavigationBarItem(selected: Boolean, onClick: () -> Unit, icon: ui.graphics.vector.ImageVector, label: String, modifier: Modifier, enabled: Boolean, badge: Function2)`
- `static fun NavigationItemIcon(badge: Function2, modifier: Modifier, content: Function3)`

#### `NavigationRailKt`

- `val localNavigationRailExpandInfo: runtime.ProvidableCompositionLocal`
- `static fun NavigationRail(modifier: Modifier, state: basic.NavigationRailState, header: Function3, color: ui.graphics.Color, showDivider: Boolean, defaultWindowInsetsPadding: Boolean, minWidth: ui.unit.Dp, expandedWidth: ui.unit.Dp, expandContentDescription: String, collapseContentDescription: String, scrollState: foundation.ScrollState, content: Function3)`
- `static fun NavigationRailItem(selected: Boolean, onClick: () -> Unit, icon: ui.graphics.vector.ImageVector, label: String, modifier: Modifier, enabled: Boolean, badge: Function2)`
- `static fun rememberNavigationRailState(initialValue: basic.NavigationRailValue): basic.NavigationRailState`

#### `NumberPickerKt`

- `static fun NumberPicker(value: Int, onValueChange: Function1, modifier: Modifier, enabled: Boolean, range: ranges.IntRange, label: Function1, visibleItemCount: Int, wrapAround: Boolean, colors: basic.NumberPickerColors, textStyle: TextStyle, itemHeight: ui.unit.Dp)`

#### `ProgressIndicatorKt`

- `static fun LinearProgressIndicator-(字节码未给出: Modifier, 字节码未给出: Float, 字节码未给出: basic.ProgressIndicatorColors, 字节码未给出: Float)`
- `static fun CircularProgressIndicator(modifier: Modifier, progress: Float, colors: basic.ProgressIndicatorColors, strokeWidth: ui.unit.Dp, size: ui.unit.Dp)`
- `static fun InfiniteProgressIndicator(modifier: Modifier, color: ui.graphics.Color, size: ui.unit.Dp, strokeWidth: ui.unit.Dp, orbitingDotSize: ui.unit.Dp)`

#### `PullToRefreshKt`

- `val localPullToRefreshState: runtime.ProvidableCompositionLocal`
- `static fun PullToRefresh(isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier, pullToRefreshState: basic.PullToRefreshState, contentPadding: foundation.layout.PaddingValues, topAppBarScrollBehavior: basic.ScrollBehavior, color: ui.graphics.Color, circleSize: ui.unit.Dp, refreshTexts: List, refreshTextStyle: TextStyle, content: Function2)`
- `static fun rememberPullToRefreshState(): basic.PullToRefreshState`

#### `RadioButtonKt`

- `static fun RadioButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier, colors: basic.RadioButtonColors, enabled: Boolean)`

#### `ScaffoldKt`

- `static fun Scaffold(modifier: Modifier, topBar: Function2, bottomBar: Function2, floatingActionButton: Function2, floatingActionButtonPosition: basic.FabPosition, floatingToolbar: Function2, floatingToolbarPosition: basic.ToolbarPosition, snackbarHost: Function2, popupHost: Function2, containerColor: ui.graphics.Color, contentWindowInsets: foundation.layout.WindowInsets, content: Function3)`
- `static fun getVertical(字节码未给出: ui.Alignment): ui.Alignment.Vertical`
- `static fun getHorizontal(字节码未给出: ui.Alignment): ui.Alignment.Horizontal`
- `static fun toAlignment(): ui.Alignment`

#### `ScrollBarKt`

- `static fun getMaxScrollOffset(字节码未给出: basic.ScrollBarAdapter): Double`
- `static fun getMaxScrollOffset$annotations(字节码未给出: basic.ScrollBarAdapter)`
- `static fun rememberScrollBarAdapter(scrollState: foundation.ScrollState): basic.ScrollBarAdapter`
- `static fun rememberScrollBarAdapter(scrollState: foundation.lazy.LazyListState): basic.ScrollBarAdapter`
- `static fun rememberScrollBarAdapter(scrollState: foundation.lazy.grid.LazyGridState): basic.ScrollBarAdapter`
- `static fun VerticalScrollBar(adapter: basic.ScrollBarAdapter, modifier: Modifier, reverseLayout: Boolean, trackPadding: foundation.layout.PaddingValues, colors: basic.ScrollBarColors, thumbWidth: ui.unit.Dp, cornerRadius: ui.unit.Dp, thumbMinLength: ui.unit.Dp, endPadding: ui.unit.Dp)`
- `static fun HorizontalScrollBar(adapter: basic.ScrollBarAdapter, modifier: Modifier, reverseLayout: Boolean, trackPadding: foundation.layout.PaddingValues, colors: basic.ScrollBarColors, thumbWidth: ui.unit.Dp, cornerRadius: ui.unit.Dp, thumbMinLength: ui.unit.Dp, endPadding: ui.unit.Dp)`

#### `SearchBarKt`

- `static fun SearchBar(inputField: Function2, onExpandedChange: Function1, modifier: Modifier, insideMargin: ui.unit.DpSize, expanded: Boolean, outsideEndAction: Function2, content: Function3)`
- `static fun InputField(query: String, onQueryChange: Function1, onSearch: Function1, expanded: Boolean, onExpandedChange: Function1, modifier: Modifier, label: String, enabled: Boolean, textStyle: TextStyle, leadingIcon: Function2, trailingIcon: Function2, interactionSource: foundation.interaction.MutableInteractionSource)`

#### `SliderKt`

- `static fun Slider(value: Float, onValueChange: Function1, modifier: Modifier, enabled: Boolean, valueRange: ranges.ClosedFloatingPointRange, steps: Int, onValueChangeFinished: () -> Unit, reverseDirection: Boolean, height: ui.unit.Dp, colors: basic.SliderColors, hapticEffect: basic.SliderDefaults.SliderHapticEffect, showKeyPoints: Boolean, keyPoints: List, magnetThreshold: Float)`
- `static fun VerticalSlider(value: Float, onValueChange: Function1, modifier: Modifier, enabled: Boolean, valueRange: ranges.ClosedFloatingPointRange, steps: Int, onValueChangeFinished: () -> Unit, reverseDirection: Boolean, width: ui.unit.Dp, colors: basic.SliderColors, effect: Boolean, hapticEffect: basic.SliderDefaults.SliderHapticEffect, showKeyPoints: Boolean, keyPoints: List, magnetThreshold: Float)`
- `static fun RangeSlider-xKBSf-U(字节码未给出: ranges.ClosedFloatingPointRange, 字节码未给出: Function1, 字节码未给出: Modifier, 字节码未给出: Boolean, 字节码未给出: ranges.ClosedFloatingPointRange, 字节码未给出: Int, 字节码未给出: () -> Unit, 字节码未给出: Float, 字节码未给出: basic.SliderColors, 字节码未给出: basic.SliderDefaults.SliderHapticEffect, 字节码未给出: Boolean, 字节码未给出: List, 字节码未给出: Float)`

#### `SmallTitleKt`

- `static fun SmallTitle(text: String, modifier: Modifier, textColor: ui.graphics.Color, insideMargin: foundation.layout.PaddingValues)`

#### `SnackbarKt`

- `static fun toMillis(字节码未给出: basic.SnackbarDuration, 字节码未给出: Boolean, 字节码未给出: ui.platform.AccessibilityManager): Long`
- `static fun SnackbarHost(state: basic.SnackbarHostState, modifier: Modifier, canSwipeToDismiss: Boolean, content: Function3)`
- `static fun Snackbar(data: basic.SnackbarData, modifier: Modifier, cornerRadius: ui.unit.Dp, colors: basic.SnackbarColors, insideMargin: foundation.layout.PaddingValues)`

#### `SurfaceKt`

- `static fun Surface-F(字节码未给出: Modifier, 字节码未给出: ui.graphics.Shape, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: foundation.BorderStroke, 字节码未给出: Float, 字节码未给出: Function2)`
- `static fun Surface(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, shape: ui.graphics.Shape, color: ui.graphics.Color, contentColor: ui.graphics.Color, border: foundation.BorderStroke, shadowElevation: ui.unit.Dp, interactionSource: foundation.interaction.MutableInteractionSource, indication: foundation.Indication, content: Function2)`

#### `SwitchKt`

- `static fun Switch(checked: Boolean, onCheckedChange: Function1, modifier: Modifier, colors: basic.SwitchColors, enabled: Boolean)`

#### `TabRowKt`

- `static fun TabRow(tabs: List, selectedTabIndex: Int, onTabSelected: Function1, modifier: Modifier, colors: basic.TabRowColors, minWidth: ui.unit.Dp, maxWidth: ui.unit.Dp, height: ui.unit.Dp, cornerRadius: ui.unit.Dp, itemSpacing: ui.unit.Dp, contentAlignment: ui.Alignment, listState: foundation.lazy.LazyListState, interactionSource: foundation.interaction.MutableInteractionSource, indication: foundation.Indication)`
- `static fun TabRowWithContour(tabs: List, selectedTabIndex: Int, onTabSelected: Function1, modifier: Modifier, colors: basic.TabRowColors, minWidth: ui.unit.Dp, maxWidth: ui.unit.Dp, height: ui.unit.Dp, cornerRadius: ui.unit.Dp, itemSpacing: ui.unit.Dp, contentAlignment: ui.Alignment, listState: foundation.lazy.LazyListState, interactionSource: foundation.interaction.MutableInteractionSource, indication: foundation.Indication)`

#### `TextFieldKt`

- `static fun TextField(state: foundation.text.input.TextFieldState, modifier: Modifier, insideMargin: ui.unit.DpSize, colors: basic.TextFieldColors, cornerRadius: ui.unit.Dp, label: String, useLabelAsPlaceholder: Boolean, enabled: Boolean, readOnly: Boolean, inputTransformation: foundation.text.input.InputTransformation, textStyle: TextStyle, keyboardOptions: foundation.text.KeyboardOptions, onKeyboardAction: foundation.text.input.KeyboardActionHandler, lineLimits: foundation.text.input.TextFieldLineLimits, leadingIcon: Function2, trailingIcon: Function2, onTextLayout: Function2, interactionSource: foundation.interaction.MutableInteractionSource, cursorBrush: ui.graphics.Brush, outputTransformation: foundation.text.input.OutputTransformation, scrollState: foundation.ScrollState)`
- `static fun TextField(value: ui.text.input.TextFieldValue, onValueChange: Function1, modifier: Modifier, insideMargin: ui.unit.DpSize, colors: basic.TextFieldColors, cornerRadius: ui.unit.Dp, label: String, useLabelAsPlaceholder: Boolean, enabled: Boolean, readOnly: Boolean, textStyle: TextStyle, keyboardOptions: foundation.text.KeyboardOptions, keyboardActions: foundation.text.KeyboardActions, leadingIcon: Function2, trailingIcon: Function2, singleLine: Boolean, maxLines: Int, minLines: Int, visualTransformation: ui.text.input.VisualTransformation, onTextLayout: Function1, interactionSource: foundation.interaction.MutableInteractionSource, cursorBrush: ui.graphics.Brush)`
- `static fun TextField(value: String, onValueChange: Function1, modifier: Modifier, insideMargin: ui.unit.DpSize, colors: basic.TextFieldColors, cornerRadius: ui.unit.Dp, label: String, useLabelAsPlaceholder: Boolean, enabled: Boolean, readOnly: Boolean, textStyle: TextStyle, keyboardOptions: foundation.text.KeyboardOptions, keyboardActions: foundation.text.KeyboardActions, leadingIcon: Function2, trailingIcon: Function2, singleLine: Boolean, maxLines: Int, minLines: Int, visualTransformation: ui.text.input.VisualTransformation, onTextLayout: Function1, interactionSource: foundation.interaction.MutableInteractionSource, cursorBrush: ui.graphics.Brush)`

#### `TextKt`

- `static fun Text(text: String, modifier: Modifier, color: ui.graphics.Color, autoSize: foundation.text.TextAutoSize, fontSize: ui.unit.TextUnit, fontStyle: ui.text.font.FontStyle, fontWeight: ui.text.font.FontWeight, fontFamily: ui.text.font.FontFamily, letterSpacing: ui.unit.TextUnit, textDecoration: ui.text.style.TextDecoration, textAlign: ui.text.style.TextAlign, lineHeight: ui.unit.TextUnit, overflow: ui.text.style.TextOverflow, softWrap: Boolean, maxLines: Int, minLines: Int, onTextLayout: Function1, style: TextStyle)`
- `static fun Text(text: String, modifier: ui.graphics.ColorProducer, color: ui.graphics.Color, autoSize: foundation.text.TextAutoSize, fontSize: ui.unit.TextUnit, fontStyle: ui.text.font.FontStyle, fontWeight: ui.text.font.FontWeight, fontFamily: ui.text.font.FontFamily, letterSpacing: ui.unit.TextUnit, textDecoration: ui.text.style.TextDecoration, textAlign: ui.text.style.TextAlign, lineHeight: ui.unit.TextUnit, overflow: ui.text.style.TextOverflow, softWrap: Boolean, maxLines: Int, minLines: Int, onTextLayout: Function1, style: TextStyle)`
- `static fun Text(text: ui.text.AnnotatedString, modifier: Modifier, color: ui.graphics.Color, autoSize: foundation.text.TextAutoSize, fontSize: ui.unit.TextUnit, fontStyle: ui.text.font.FontStyle, fontWeight: ui.text.font.FontWeight, fontFamily: ui.text.font.FontFamily, letterSpacing: ui.unit.TextUnit, textDecoration: ui.text.style.TextDecoration, textAlign: ui.text.style.TextAlign, lineHeight: ui.unit.TextUnit, overflow: ui.text.style.TextOverflow, softWrap: Boolean, maxLines: Int, minLines: Int, inlineContent: Map, onTextLayout: Function1, style: TextStyle)`
- `static fun Text(text: ui.text.AnnotatedString, modifier: ui.graphics.ColorProducer, color: ui.graphics.Color, autoSize: foundation.text.TextAutoSize, fontSize: ui.unit.TextUnit, fontStyle: ui.text.font.FontStyle, fontWeight: ui.text.font.FontWeight, fontFamily: ui.text.font.FontFamily, letterSpacing: ui.unit.TextUnit, textDecoration: ui.text.style.TextDecoration, textAlign: ui.text.style.TextAlign, lineHeight: ui.unit.TextUnit, overflow: ui.text.style.TextOverflow, softWrap: Boolean, maxLines: Int, minLines: Int, inlineContent: Map, onTextLayout: Function1, style: TextStyle)`

#### `TooltipKt`

- `static fun rememberTooltipState(initialIsVisible: Boolean, isPersistent: Boolean, mutatorMutex: foundation.MutatorMutex): basic.TooltipState`
- `static fun TooltipBox(positionProvider: ui.window.PopupPositionProvider, tooltip: Function3, state: basic.TooltipState, modifier: Modifier, focusable: Boolean, enableUserInput: Boolean, content: Function2)`
- `static fun PlainTooltip(modifier: basic.TooltipScope, caretShape: Modifier, maxWidth: ui.unit.Dp, cornerRadius: ui.unit.Dp, containerColor: ui.graphics.Color, contentColor: ui.graphics.Color, insideMargin: Long, content: foundation.layout.PaddingValues, 字节码未给出: Function2)`
- `static fun RichTooltip(modifier: basic.TooltipScope, title: Modifier, action: Function2, caretShape: Function2, maxWidth: ui.unit.Dp, cornerRadius: ui.unit.Dp, colors: Float, insideMargin: basic.RichTooltipColors, text: foundation.layout.PaddingValues, 字节码未给出: Function2)`
- `static fun TooltipBox(text: String, modifier: Modifier, state: basic.TooltipState, enabled: Boolean, positioning: basic.TooltipAnchorPosition, containerColor: ui.graphics.Color, contentColor: ui.graphics.Color, content: Function2)`
- `static fun RichTooltipBox(text: String, modifier: Modifier, state: basic.TooltipState, title: String, actionText: String, onActionClick: () -> Unit, enabled: Boolean, positioning: basic.TooltipAnchorPosition, colors: basic.RichTooltipColors, content: Function2)`

#### `TopAppBarKt`

- `static fun TopAppBar(title: String, modifier: Modifier, color: ui.graphics.Color, titleColor: ui.graphics.Color, largeTitle: String, largeTitleColor: ui.graphics.Color, subtitle: String, subtitleColor: ui.graphics.Color, navigationIcon: Function2, actions: Function3, scrollBehavior: basic.ScrollBehavior, defaultWindowInsetsPadding: Boolean, titlePadding: ui.unit.Dp, navigationIconPadding: ui.unit.Dp, actionIconPadding: ui.unit.Dp, bottomContent: Function2)`
- `static fun SmallTopAppBar(title: String, modifier: Modifier, color: ui.graphics.Color, titleColor: ui.graphics.Color, subtitle: String, subtitleColor: ui.graphics.Color, navigationIcon: Function2, actions: Function3, scrollBehavior: basic.ScrollBehavior, defaultWindowInsetsPadding: Boolean, titlePadding: ui.unit.Dp, navigationIconPadding: ui.unit.Dp, actionIconPadding: ui.unit.Dp, bottomContent: Function2)`
- `static fun MiuixScrollBehavior(state: basic.TopAppBarState, canScroll: () -> Unit, snapAnimationSpec: animation.core.AnimationSpec, flingAnimationSpec: animation.core.DecayAnimationSpec): basic.ScrollBehavior`
- `static fun rememberTopAppBarState(initialHeightOffsetLimit: Float, initialHeightOffset: Float, initialContentOffset: Float): basic.TopAppBarState`

### 1.2 `Defaults` / `Colors` 单例

这些是 `object`，成员用 `XxxDefaults.成员名` 读取。

#### `BadgeDefaults`

- `val largeSize: Float`
- `val size: Float`
- `fun getContainerColor(): Long`
- `fun getContentColor(): Long`

#### `BasicComponentColors`

- `constructor(p0: Long, p1: Long)`
- `val color: Long`
- `val disabledColor: Long`
- `fun color-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun copy-(字节码未给出: Long, 字节码未给出: Long): basic.BasicComponentColors`

#### `BasicComponentDefaults`

- `val insideMargin: foundation.layout.PaddingValues`
- `fun titleColor(color: ui.graphics.Color, disabledColor: ui.graphics.Color): basic.BasicComponentColors`
- `fun summaryColor(color: ui.graphics.Color, disabledColor: ui.graphics.Color): basic.BasicComponentColors`

#### `ButtonColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long)`
- `val color: Long`
- `val contentColor: Long`
- `val disabledColor: Long`
- `val disabledContentColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.ButtonColors`

#### `ButtonDefaults`

- `val cornerRadius: Float`
- `val insideMargin: foundation.layout.PaddingValues`
- `val minHeight: Float`
- `val minWidth: Float`
- `fun buttonColors(color: ui.graphics.Color, disabledColor: ui.graphics.Color, contentColor: ui.graphics.Color, disabledContentColor: ui.graphics.Color): basic.ButtonColors`
- `fun buttonColorsPrimary(color: ui.graphics.Color, disabledColor: ui.graphics.Color, contentColor: ui.graphics.Color, disabledContentColor: ui.graphics.Color): basic.ButtonColors`
- `fun textButtonColors(color: ui.graphics.Color, disabledColor: ui.graphics.Color, textColor: ui.graphics.Color, disabledTextColor: ui.graphics.Color): basic.TextButtonColors`
- `fun textButtonColorsPrimary(color: ui.graphics.Color, disabledColor: ui.graphics.Color, textColor: ui.graphics.Color, disabledTextColor: ui.graphics.Color): basic.TextButtonColors`

#### `CardColors`

- `constructor(p0: Long, p1: Long)`
- `val color: Long`
- `val contentColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun copy-(字节码未给出: Long, 字节码未给出: Long): basic.CardColors`

#### `CardDefaults`

- `val cornerRadius: Float`
- `val insideMargin: foundation.layout.PaddingValues`
- `fun defaultColors(color: ui.graphics.Color, contentColor: ui.graphics.Color): basic.CardColors`

#### `CheckboxColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long, p4: Long, p5: Long, p6: Long, p7: Long)`
- `fun checkedForegroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun uncheckedForegroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun checkedBackgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun uncheckedBackgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.CheckboxColors`

#### `CheckboxDefaults`

- `fun checkboxColors(checkedForegroundColor: ui.graphics.Color, uncheckedForegroundColor: ui.graphics.Color, disabledCheckedForegroundColor: ui.graphics.Color, disabledUncheckedForegroundColor: ui.graphics.Color, checkedBackgroundColor: ui.graphics.Color, uncheckedBackgroundColor: ui.graphics.Color, disabledCheckedBackgroundColor: ui.graphics.Color, disabledUncheckedBackgroundColor: ui.graphics.Color): basic.CheckboxColors`

#### `DividerDefaults`

- `val thickness: Float`
- `fun getDividerColor(): Long`

#### `DropdownColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long, p4: Long, p5: Long, p6: Long)`
- `val containerColor: Long`
- `val contentColor: Long`
- `val selectedContainerColor: Long`
- `val selectedContentColor: Long`
- `val selectedIndicatorColor: Long`
- `val selectedSummaryColor: Long`
- `val summaryColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun component5(): Long`
- `fun component6(): Long`
- `fun component7(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.DropdownColors`

#### `DropdownDefaults`

- `val arrowSize: Long`
- `val checkIconSize: Float`
- `val checkIconStartPadding: Float`
- `val chevronSize: Long`
- `val dialogHorizontalPadding: Float`
- `val firstLastVerticalPadding: Float`
- `val iconEndPadding: Float`
- `val iconMinSize: Float`
- `val insideHorizontalPadding: Float`
- `val maxItemTextWidth: Float`
- `val middleVerticalPadding: Float`
- `val minHeight: Float`
- `val minWidth: Float`
- `fun dropdownColors(contentColor: ui.graphics.Color, summaryColor: ui.graphics.Color, containerColor: ui.graphics.Color, selectedContentColor: ui.graphics.Color, selectedSummaryColor: ui.graphics.Color, selectedContainerColor: ui.graphics.Color, selectedIndicatorColor: ui.graphics.Color): basic.DropdownColors`
- `fun dialogDropdownColors(contentColor: ui.graphics.Color, summaryColor: ui.graphics.Color, containerColor: ui.graphics.Color, selectedContentColor: ui.graphics.Color, selectedSummaryColor: ui.graphics.Color, selectedContainerColor: ui.graphics.Color, selectedIndicatorColor: ui.graphics.Color): basic.DropdownColors`

#### `FloatingActionButtonDefaults`

- `val minHeight: Float`
- `val minWidth: Float`
- `val shadowElevation: Float`

#### `FloatingNavigationBarDefaults`

- `val horizontalOutSidePadding: Float`
- `val horizontalPadding: Float`
- `val iconPadding: Float`
- `val iconSize: Float`
- `val itemSpacing: Float`
- `val selectedPressedAlpha: Float`
- `val shadowElevation: Float`
- `val unselectedAlpha: Float`
- `val unselectedPressedAlpha: Float`

#### `FloatingToolbarDefaults`

- `val cornerRadius: Float`
- `val outSidePadding: foundation.layout.PaddingValues`
- `fun defaultColor(): Long`

#### `IconButtonDefaults`

- `val cornerRadius: Float`
- `val minHeight: Float`
- `val minWidth: Float`

#### `ListPopupDefaults`

- `val alphaEnterAnimationSpec: animation.core.TweenSpec`
- `val alphaExitAnimationSpec: animation.core.TweenSpec`
- `val contextMenuPositionProvider: basic.PopupPositionProvider`
- `val dimEnterAnimationSpec: animation.core.TweenSpec`
- `val dimExitAnimationSpec: animation.core.TweenSpec`
- `val dropdownPositionProvider: basic.PopupPositionProvider`
- `val fractionAnimationSpec: animation.core.SpringSpec`
- `val minPopupHeight: Float`
- `val minWidth: Float`
- `val resetAnimationSpec: animation.core.SpringSpec`
- `fun dropdownPositionProvider(字节码未给出: Float, 字节码未给出: Float): basic.PopupPositionProvider`

#### `NavigationBarDefaults`

- `val bottomPadding: Float`
- `val iconSize: Float`
- `val iconTopPadding: Float`
- `val itemHeight: Float`
- `val labelFontSize: Long`
- `val selectedPressedAlpha: Float`
- `val unselectedAlpha: Float`
- `val unselectedPressedAlpha: Float`

#### `NavigationRailDefaults`

- `val collapseContentDescription: String`
- `val collapsedIndicatorVerticalPadding: Float`
- `val expandContentDescription: String`
- `val expandedItemContentHorizontalPadding: Float`
- `val expandedItemContentVerticalPadding: Float`
- `val expandedItemCornerRadius: Float`
- `val expandedItemHorizontalMargin: Float`
- `val expandedItemIconTextSpacing: Float`
- `val expandedLabelFontSize: Long`
- `val expandedWidth: Float`
- `val headerSpacing: Float`
- `val iconSize: Float`
- `val iconTextSpacing: Float`
- `val itemVerticalPadding: Float`
- `val labelFontSize: Long`
- `val minWidth: Float`
- `val verticalPadding: Float`

#### `NumberPickerColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long)`
- `fun selectedTextColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun unselectedTextColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.NumberPickerColors`

#### `NumberPickerDefaults`

- `val itemHeight: Float`
- `fun colors(selectedTextColor: ui.graphics.Color, unselectedTextColor: ui.graphics.Color, disabledSelectedTextColor: ui.graphics.Color, disabledUnselectedTextColor: ui.graphics.Color): basic.NumberPickerColors`

#### `ProgressIndicatorColors`

- `constructor(p0: Long, p1: Long, p2: Long)`
- `fun foregroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun backgroundColor-0d7_KjU$top_yukonga_miuix_kmp_miuix_ui(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.ProgressIndicatorColors`

#### `ProgressIndicatorDefaults`

- `val defaultCircularProgressIndicatorSize: Float`
- `val defaultCircularProgressIndicatorStrokeWidth: Float`
- `val defaultInfiniteProgressIndicatorOrbitingDotSize: Float`
- `val defaultInfiniteProgressIndicatorSize: Float`
- `val defaultInfiniteProgressIndicatorStrokeWidth: Float`
- `val defaultLinearProgressIndicatorHeight: Float`
- `fun progressIndicatorColors(foregroundColor: ui.graphics.Color, disabledForegroundColor: ui.graphics.Color, backgroundColor: ui.graphics.Color): basic.ProgressIndicatorColors`

#### `PullToRefreshDefaults`

- `val circleSize: Float`
- `val color: Long`
- `val refreshTexts: List`
- `val refreshTextStyle: TextStyle`

#### `RadioButtonColors`

- `constructor(p0: Long, p1: Long)`
- `fun color-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun copy-(字节码未给出: Long, 字节码未给出: Long): basic.RadioButtonColors`

#### `RadioButtonDefaults`

- `fun radioButtonColors(selectedColor: ui.graphics.Color, disabledSelectedColor: ui.graphics.Color): basic.RadioButtonColors`

#### `RichTooltipColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long)`
- `val actionContentColor: Long`
- `val containerColor: Long`
- `val contentColor: Long`
- `val titleContentColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.RichTooltipColors`

#### `ScrollBarColors`

- `constructor(p0: Long, p1: Long)`
- `val thumbColor: Long`
- `val trackColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun copy-(字节码未给出: Long, 字节码未给出: Long): basic.ScrollBarColors`

#### `ScrollBarDefaults`

- `val cornerRadius: Float`
- `val dragAnimationDurationMillis: Int`
- `val dragThumbAlpha: Float`
- `val dragThumbWidth: Float`
- `val endPadding: Float`
- `val fadeDelayMillis: Int`
- `val fadeDurationMillis: Int`
- `val thumbAlpha: Float`
- `val thumbMinLength: Float`
- `val thumbWidth: Float`
- `val touchTargetWidth: Float`
- `fun scrollBarColors-(字节码未给出: Long, 字节码未给出: Long): basic.ScrollBarColors`

#### `SearchBarDefaults`

- `val inputFieldFontSize: Long`
- `val inputFieldMinHeight: Float`
- `val insideMargin: Long`
- `val leadingIconEndPadding: Float`
- `val leadingIconStartPadding: Float`
- `val trailingIconEndPadding: Float`
- `val trailingIconStartPadding: Float`

#### `SliderColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long, p4: Long, p5: Long, p6: Long, p7: Long)`
- `fun foregroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun backgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun thumbColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun keyPointColor-0d7_KjU$top_yukonga_miuix_kmp_miuix_ui(): Long`
- `fun keyPointForegroundColor-0d7_KjU$top_yukonga_miuix_kmp_miuix_ui(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.SliderColors`

#### `SliderDefaults`

- `val defaultHapticEffect: basic.SliderDefaults.SliderHapticEffect`
- `val keyPointRadius: Float`
- `val minHeight: Float`
- `fun sliderColors(foregroundColor: ui.graphics.Color, disabledForegroundColor: ui.graphics.Color, backgroundColor: ui.graphics.Color, disabledBackgroundColor: ui.graphics.Color, thumbColor: ui.graphics.Color, disabledThumbColor: ui.graphics.Color, keyPointColor: ui.graphics.Color, keyPointForegroundColor: ui.graphics.Color): basic.SliderColors`

#### `SmallTitleDefaults`

- `val insideMargin: foundation.layout.PaddingValues`

#### `SnackbarColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long, p4: Long)`
- `val actionContainerColor: Long`
- `val actionContentColor: Long`
- `val containerColor: Long`
- `val contentColor: Long`
- `val dismissActionContentColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun component5(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.SnackbarColors`

#### `SnackbarDefaults`

- `val actionCornerRadius: Float`
- `val actionInsideMargin: foundation.layout.PaddingValues`
- `val cornerRadius: Float`
- `val insideMargin: foundation.layout.PaddingValues`
- `val outerPadding: foundation.layout.PaddingValues`
- `fun snackbarColors(containerColor: ui.graphics.Color, contentColor: ui.graphics.Color, actionContentColor: ui.graphics.Color, dismissActionContentColor: ui.graphics.Color, actionContainerColor: ui.graphics.Color): basic.SnackbarColors`

#### `SpinnerDefaults`

- `fun spinnerColors(contentColor: ui.graphics.Color, summaryColor: ui.graphics.Color, containerColor: ui.graphics.Color, selectedContentColor: ui.graphics.Color, selectedSummaryColor: ui.graphics.Color, selectedContainerColor: ui.graphics.Color, selectedIndicatorColor: ui.graphics.Color): basic.DropdownColors`
- `fun dialogSpinnerColors(contentColor: ui.graphics.Color, summaryColor: ui.graphics.Color, containerColor: ui.graphics.Color, selectedContentColor: ui.graphics.Color, selectedSummaryColor: ui.graphics.Color, selectedContainerColor: ui.graphics.Color, selectedIndicatorColor: ui.graphics.Color): basic.DropdownColors`

#### `SurfaceDefaults`

- `val shadowElevation: Float`
- `val shape: ui.graphics.Shape`

#### `SwitchColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long, p4: Long, p5: Long, p6: Long, p7: Long)`
- `fun checkedThumbColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun uncheckedThumbColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun checkedTrackColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun uncheckedTrackColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.SwitchColors`

#### `SwitchDefaults`

- `fun switchColors(checkedThumbColor: ui.graphics.Color, uncheckedThumbColor: ui.graphics.Color, disabledCheckedThumbColor: ui.graphics.Color, disabledUncheckedThumbColor: ui.graphics.Color, checkedTrackColor: ui.graphics.Color, uncheckedTrackColor: ui.graphics.Color, disabledCheckedTrackColor: ui.graphics.Color, disabledUncheckedTrackColor: ui.graphics.Color): basic.SwitchColors`

#### `TabRowColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long)`
- `fun backgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun contentColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Boolean): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.TabRowColors`

#### `TabRowDefaults`

- `val tabRowCornerRadius: Float`
- `val tabRowHeight: Float`
- `val tabRowMaxWidth: Float`
- `val tabRowMinWidth: Float`
- `val tabRowWithContourCornerRadius: Float`
- `val tabRowWithContourHeight: Float`
- `val tabRowWithContourMaxWidth: Float`
- `val tabRowWithContourMinWidth: Float`
- `fun tabRowColors(backgroundColor: ui.graphics.Color, contentColor: ui.graphics.Color, selectedBackgroundColor: ui.graphics.Color, selectedContentColor: ui.graphics.Color): basic.TabRowColors`

#### `TextButtonColors`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long)`
- `val color: Long`
- `val disabledColor: Long`
- `val disabledTextColor: Long`
- `val textColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.TextButtonColors`

#### `TextFieldColors`

- `constructor(p0: Long, p1: Long, p2: Long)`
- `val backgroundColor: Long`
- `val borderColor: Long`
- `val labelColor: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): basic.TextFieldColors`

#### `TextFieldDefaults`

- `val borderWidth: Float`
- `val cornerRadius: Float`
- `val insideMargin: Long`
- `val labelFontSizeFloating: Float`
- `val labelFontSizeNormal: Float`
- `fun textFieldColors(backgroundColor: ui.graphics.Color, labelColor: ui.graphics.Color, borderColor: ui.graphics.Color): basic.TextFieldColors`

#### `TooltipDefaults`

- `val caretSize: Long`
- `val plainTooltipCornerRadius: Float`
- `val plainTooltipInsideMargin: foundation.layout.PaddingValues`
- `val plainTooltipMaxWidth: Float`
- `val richTooltipActionCornerRadius: Float`
- `val richTooltipActionInsideMargin: foundation.layout.PaddingValues`
- `val richTooltipCornerRadius: Float`
- `val richTooltipInsideMargin: foundation.layout.PaddingValues`
- `val richTooltipMaxWidth: Float`
- `val spacingBetweenTooltipAndAnchor: Float`
- `fun getPlainTooltipContainerColor(): Long`
- `fun getPlainTooltipContentColor(): Long`
- `fun caretShape(): ui.graphics.Shape`
- `fun richTooltipColors(containerColor: ui.graphics.Color, contentColor: ui.graphics.Color, titleContentColor: ui.graphics.Color, actionContentColor: ui.graphics.Color): basic.RichTooltipColors`
- `fun rememberTooltipPositionProvider(positioning: basic.TooltipAnchorPosition, spacingBetweenTooltipAndAnchor: ui.unit.Dp): ui.window.PopupPositionProvider`

#### `TopAppBarDefaults`

- `val actionIconPadding: Float`
- `val collapsedHeight: Float`
- `val largeTitleBottomPadding: Float`
- `val navigationIconPadding: Float`
- `val smallTopAppBarCenterHeight: Float`
- `val subtitleBottomPadding: Float`
- `val titlePadding: Float`

### 1.3 其余 basic 公开类型（状态类 / 数据类 / 枚举）

#### `ColorSpace`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<basic.ColorSpace>`
- `static fun valueOf(字节码未给出: String): basic.ColorSpace`

#### `DropdownEntry`

- `constructor(p0: List, p1: Boolean)`
- `val enabled: Boolean`
- `val items: List`
- `fun copy(字节码未给出: List, 字节码未给出: Boolean): basic.DropdownEntry`

#### `DropdownItem`

- `constructor(p0: String, p1: Boolean, p2: Boolean, p3: () -> Unit, p4: Function3, p5: String, p6: List)`
- `constructor(p0: Function3, p1: String, p2: String)`
- `val children: List`
- `val enabled: Boolean`
- `val icon: Function3`
- `val onClick: () -> Unit`
- `val selected: Boolean`
- `val summary: String`
- `val text: String`
- `fun copy(字节码未给出: String, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: () -> Unit, 字节码未给出: Function3, 字节码未给出: String, 字节码未给出: List): basic.DropdownItem`

#### `FabPlacement`

- `val height: Int`
- `val left: Int`
- `val width: Int`

#### `FabPosition`

- `static fun toString(): String`
- `static fun hashCode(): Int`
- `static fun equals(字节码未给出: Int, 字节码未给出: Any): Boolean`
- `static fun constructor(): Int`
- `static fun box(): basic.FabPosition`
- `fun unbox(): Int`
- `static fun equals(): Boolean`

#### `ListPopupLayoutInfo`

- `constructor(p0: ui.unit.IntRect, p1: ui.unit.IntRect, p2: Long, p3: Long, p4: basic.PopupLayoutPosition)`
- `val effectiveTransformOrigin: Long`
- `val localTransformOrigin: Long`
- `val popupLayoutPosition: basic.PopupLayoutPosition`
- `val popupMargin: ui.unit.IntRect`
- `val windowBounds: ui.unit.IntRect`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun copy(字节码未给出: ui.unit.IntRect, 字节码未给出: ui.unit.IntRect, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: basic.PopupLayoutPosition): basic.ListPopupLayoutInfo`

#### `MutableWindowInsets`

- `constructor(p0: foundation.layout.WindowInsets)`
- `var insets: foundation.layout.WindowInsets`
- `fun getLeft(字节码未给出: ui.unit.Density, 字节码未给出: ui.unit.LayoutDirection): Int`
- `fun getTop(字节码未给出: ui.unit.Density): Int`
- `fun getRight(字节码未给出: ui.unit.Density, 字节码未给出: ui.unit.LayoutDirection): Int`
- `fun getBottom(字节码未给出: ui.unit.Density): Int`

#### `NavigationBarDisplayMode`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<basic.NavigationBarDisplayMode>`
- `static fun valueOf(字节码未给出: String): basic.NavigationBarDisplayMode`

#### `NavigationItem`

- `constructor(p0: String, p1: ui.graphics.vector.ImageVector)`
- `val icon: ui.graphics.vector.ImageVector`
- `val label: String`
- `fun copy(字节码未给出: String, 字节码未给出: ui.graphics.vector.ImageVector): basic.NavigationItem`

#### `NavigationRailExpandInfo`

- `constructor(p0: runtime.State, p1: Float)`
- `val collapsedWidth: Float`
- `val progress: runtime.State`

#### `NavigationRailState`

- `constructor(p0: basic.NavigationRailValue)`
- `val currentValue: basic.NavigationRailValue`
- `val isExpanded: Boolean`
- `fun expand()`
- `fun collapse()`
- `fun toggle()`

#### `NavigationRailValue`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<basic.NavigationRailValue>`
- `static fun valueOf(字节码未给出: String): basic.NavigationRailValue`

#### `PopupLayoutPosition`

- `constructor(p0: Boolean, p1: Boolean, p2: Boolean)`
- `val isRightAligned: Boolean`
- `val showAbove: Boolean`
- `val showBelow: Boolean`
- `fun copy(字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean): basic.PopupLayoutPosition`

#### `PopupPositionProvider`

- `val margins: foundation.layout.PaddingValues`
- `fun calculatePosition(字节码未给出: ui.unit.IntRect, 字节码未给出: ui.unit.IntRect, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: Long, 字节码未给出: ui.unit.IntRect, 字节码未给出: basic.PopupPositionProvider.Align): Long`

#### `PullToRefreshState`

- `constructor(p0: kotlinx.coroutines.CoroutineScope)`
- `var animationJob: kotlinx.coroutines.Job`
- `var cachedNestedScrollConnection: ui.input.nestedscroll.NestedScrollConnection`
- `var coroutineScope: kotlinx.coroutines.CoroutineScope`
- `var currentTouch: Float`
- `var dragOffset: Float`
- `val isRebounding: Boolean`
- `val isRefreshing: Boolean`
- `val isTouching: Boolean`
- `var maxDragDistancePx: Float`
- `val pullProgress: Float`
- `val refreshCompleteAnimProgress: Float`
- `val refreshState: basic.RefreshState`
- `var refreshThresholdOffset: Float`
- `fun animateToSpring$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: Float, 字节码未给出: coroutines.Continuation): Any`
- `fun startRefreshing$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: () -> Unit, 字节码未给出: () -> Unit, 字节码未给出: coroutines.Continuation): Any`
- `fun showRefreshing$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: () -> Unit, 字节码未给出: coroutines.Continuation): Any`
- `fun finishRefreshing$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: () -> Unit, 字节码未给出: coroutines.Continuation): Any`
- `fun handlePointerRelease$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: () -> Unit, 字节码未给出: () -> Unit, 字节码未给出: coroutines.Continuation): Any`
- `fun resetToIdle$top_yukonga_miuix_kmp_miuix_ui()`
- `fun getOrCreateNestedScrollConnection$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: utils.OverScrollState): ui.input.nestedscroll.NestedScrollConnection`

#### `RangeSliderHapticState`

- `constructor()`
- `fun resetStart(字节码未给出: Float, 字节码未给出: ranges.ClosedFloatingPointRange)`
- `fun resetEnd(字节码未给出: Float, 字节码未给出: ranges.ClosedFloatingPointRange)`
- `fun inheritStartKeyPoint()`
- `fun inheritEndKeyPoint()`
- `fun handleStartHapticFeedback(字节码未给出: Float, 字节码未给出: ranges.ClosedFloatingPointRange, 字节码未给出: basic.SliderDefaults.SliderHapticEffect, 字节码未给出: ui.hapticfeedback.HapticFeedback, 字节码未给出: Array<Float>, 字节码未给出: Boolean)`
- `fun handleEndHapticFeedback(字节码未给出: Float, 字节码未给出: ranges.ClosedFloatingPointRange, 字节码未给出: basic.SliderDefaults.SliderHapticEffect, 字节码未给出: ui.hapticfeedback.HapticFeedback, 字节码未给出: Array<Float>, 字节码未给出: Boolean)`

#### `ScrollBarAdapter`

- `val contentSize: Double`
- `val scrollOffset: Double`
- `val viewportSize: Double`
- `fun scrollTo(字节码未给出: Double, 字节码未给出: coroutines.Continuation): Any`

#### `ScrollBehavior`

- `val flingAnimationSpec: animation.core.DecayAnimationSpec`
- `val isPinned: Boolean`
- `val nestedScrollConnection: ui.input.nestedscroll.NestedScrollConnection`
- `val snapAnimationSpec: animation.core.AnimationSpec`
- `val state: basic.TopAppBarState`

#### `SliderHapticState`

- `constructor()`
- `fun reset(字节码未给出: Float, 字节码未给出: ranges.ClosedFloatingPointRange, 字节码未给出: Float)`
- `fun handleHapticFeedback(字节码未给出: Float, 字节码未给出: ranges.ClosedFloatingPointRange, 字节码未给出: basic.SliderDefaults.SliderHapticEffect, 字节码未给出: ui.hapticfeedback.HapticFeedback, 字节码未给出: Array<Float>, 字节码未给出: Boolean)`

#### `SnackbarData`

- `val visuals: basic.SnackbarVisuals`
- `fun dismiss(字节码未给出: coroutines.Continuation): Any`
- `fun performAction(字节码未给出: coroutines.Continuation): Any`

#### `SnackbarHostState`

- `constructor()`
- `val currentSnackbars: List`
- `fun newestSnackbarData(字节码未给出: coroutines.Continuation): Any`
- `fun oldestSnackbarData(字节码未给出: coroutines.Continuation): Any`
- `fun removeEntry$top_yukonga_miuix_kmp_miuix_ui(字节码未给出: basic.SnackbarHostState.SnackbarEntry, 字节码未给出: coroutines.Continuation): Any`
- `fun showSnackbar(字节码未给出: String, 字节码未给出: String, 字节码未给出: Boolean, 字节码未给出: basic.SnackbarDuration, 字节码未给出: coroutines.Continuation): Any`

#### `SnackbarResult`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<basic.SnackbarResult>`
- `static fun valueOf(字节码未给出: String): basic.SnackbarResult`

#### `SnackbarVisuals`

- `constructor(p0: String, p1: String, p2: Boolean, p3: basic.SnackbarDuration)`
- `val actionLabel: String`
- `val duration: basic.SnackbarDuration`
- `val message: String`
- `val withDismissAction: Boolean`
- `fun copy(字节码未给出: String, 字节码未给出: String, 字节码未给出: Boolean, 字节码未给出: basic.SnackbarDuration): basic.SnackbarVisuals`

#### `ToolbarPosition`

- `static fun toString(): String`
- `static fun hashCode(): Int`
- `static fun equals(字节码未给出: Int, 字节码未给出: Any): Boolean`
- `static fun constructor(): Int`
- `static fun box(): basic.ToolbarPosition`
- `fun unbox(): Int`
- `static fun equals(): Boolean`

#### `TooltipAnchorPosition`

- `static fun toString(): String`
- `static fun hashCode(): Int`
- `static fun equals(字节码未给出: Int, 字节码未给出: Any): Boolean`
- `static fun box(): basic.TooltipAnchorPosition`
- `fun unbox(): Int`
- `static fun equals(): Boolean`

#### `TooltipScope`

- `val positioning: Int`
- `fun obtainAnchorBounds(): ui.layout.LayoutCoordinates`

#### `TooltipState`

- `val isPersistent: Boolean`
- `val isVisible: Boolean`
- `val transition: animation.core.MutableTransitionState`
- `fun show(字节码未给出: foundation.MutatePriority, 字节码未给出: coroutines.Continuation): Any`
- `fun dismiss()`
- `fun onDispose()`

#### `TopAppBarState`

- `constructor(p0: Float, p1: Float, p2: Float)`
- `val collapsedFraction: Float`
- `var contentOffset: Float`
- `var heightOffset: Float`
- `var heightOffsetLimit: Float`
- `val overlappedFraction: Float`

### 1.4 全部枚举的取值

这些枚举在字节码里可直接读出全部常量，用于 `when` 分支或参数取值。

| 枚举 | 取值 |
| --- | --- |
| `com.kyant.shapes.RoundedCornerStyle` | `Circular`, `Continuous` |
| `top.yukonga.miuix.kmp.basic.ColorSpace` | `HSV`, `OKHSV`, `OKLAB`, `OKLCH` |
| `top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode` | `IconAndText`, `IconOnly`, `IconWithSelectedLabel` |
| `top.yukonga.miuix.kmp.basic.NavigationRailValue` | `Collapsed`, `Expanded` |
| `top.yukonga.miuix.kmp.basic.SnackbarResult` | `Dismissed`, `ActionPerformed` |
| `top.yukonga.miuix.kmp.preference.CheckboxLocation` | `Start`, `End` |
| `top.yukonga.miuix.kmp.preference.RadioButtonLocation` | `Start`, `End` |
| `top.yukonga.miuix.kmp.theme.ColorSchemeMode` | `System`, `Light`, `Dark`, `MonetSystem`, `MonetLight`, `MonetDark` |
| `top.yukonga.miuix.kmp.theme.ThemeColorSpec` | `Spec2021`, `Spec2025` |
| `top.yukonga.miuix.kmp.theme.ThemePaletteStyle` | `TonalSpot`, `Neutral`, `Vibrant`, `Expressive`, `Rainbow`, `FruitSalad`, `Monochrome`, `Fidelity`, `Content` |
| `top.yukonga.miuix.kmp.utils.PressFeedbackType` | `None`, `Sink`, `Tilt` |

## 2. `top.yukonga.miuix.kmp.theme`

### 2.1 `MiuixTheme(...)` — 主题注入 Composable

- `static fun MiuixTheme(controller: theme.ThemeController, textStyles: theme.TextStyles, content: Function2)`
- `static fun MiuixTheme(controller: theme.Colors, textStyles: theme.TextStyles, content: Function2)`

### 2.2 `MiuixThemeController` / `ThemeController`

工厂函数签名为（7 个参数，`@Metadata` 显示**全部有默认值**，
因此 `MiuixThemeController()` 可以直接编译。参数名未能从字节码确认，按类型顺序给出）：

- `MiuixThemeController(p0: theme.ColorSchemeMode, p1: theme.Colors, p2: theme.Colors, p3: Color, p4: theme.ThemeColorSpec, p5: theme.ThemePaletteStyle, p6: Boolean)`

#### `ThemeController`

- `constructor(p0: theme.ColorSchemeMode, p1: theme.Colors, p2: theme.Colors, p3: Color, p4: theme.ThemeColorSpec, p5: theme.ThemePaletteStyle, p6: Boolean)`
- `val colorSchemeMode: theme.ColorSchemeMode`
- `val colorSpec: theme.ThemeColorSpec`
- `val darkColors: theme.Colors`
- `val isDark: Boolean`
- `val keyColor: Color`
- `val lightColors: theme.Colors`
- `val paletteStyle: theme.ThemePaletteStyle`
- `fun currentColors(): theme.Colors`

### 2.3 `Colors` — `MiuixTheme.colorScheme` 的类型

类型名是 `Colors`（不是 `ColorScheme`）。全部字段为 `Color`，且是 `var`。

共 **53** 个字段：

- `var background: Color`
- `var disabledOnPrimary: Color`
- `var disabledOnPrimaryButton: Color`
- `var disabledOnSecondary: Color`
- `var disabledOnSecondaryVariant: Color`
- `var disabledOnSurface: Color`
- `var disabledPrimary: Color`
- `var disabledPrimaryButton: Color`
- `var disabledPrimarySlider: Color`
- `var disabledSecondary: Color`
- `var disabledSecondaryVariant: Color`
- `var dividerLine: Color`
- `var error: Color`
- `var errorContainer: Color`
- `var onBackground: Color`
- `var onBackgroundVariant: Color`
- `var onError: Color`
- `var onErrorContainer: Color`
- `var onPrimary: Color`
- `var onPrimaryContainer: Color`
- `var onPrimaryVariant: Color`
- `var onSecondary: Color`
- `var onSecondaryContainer: Color`
- `var onSecondaryContainerVariant: Color`
- `var onSecondaryVariant: Color`
- `var onSurface: Color`
- `var onSurfaceContainer: Color`
- `var onSurfaceContainerHigh: Color`
- `var onSurfaceContainerHighest: Color`
- `var onSurfaceContainerVariant: Color`
- `var onSurfaceSecondary: Color`
- `var onSurfaceVariantActions: Color`
- `var onSurfaceVariantSummary: Color`
- `var onTertiaryContainer: Color`
- `var outline: Color`
- `var primary: Color`
- `var primaryContainer: Color`
- `var primaryVariant: Color`
- `var secondary: Color`
- `var secondaryContainer: Color`
- `var secondaryContainerVariant: Color`
- `var secondaryVariant: Color`
- `var sliderBackground: Color`
- `var sliderKeyPoint: Color`
- `var sliderKeyPointForeground: Color`
- `var surface: Color`
- `var surfaceContainer: Color`
- `var surfaceContainerHigh: Color`
- `var surfaceContainerHighest: Color`
- `var surfaceVariant: Color`
- `var tertiaryContainer: Color`
- `var tertiaryContainerVariant: Color`
- `var windowDimming: Color`

### 2.4 `TextStyles` — `MiuixTheme.textStyles` 的类型

共 **14** 个字段，类型均为 `androidx.compose.ui.text.TextStyle`：

- `var body1: TextStyle`
- `var body2: TextStyle`
- `var button: TextStyle`
- `var footnote1: TextStyle`
- `var footnote2: TextStyle`
- `var headline1: TextStyle`
- `var headline2: TextStyle`
- `var main: TextStyle`
- `var paragraph: TextStyle`
- `var subtitle: TextStyle`
- `var title1: TextStyle`
- `var title2: TextStyle`
- `var title3: TextStyle`
- `var title4: TextStyle`

### 2.5 其余 theme 公开类型

#### `ColorSchemeMode`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<theme.ColorSchemeMode>`
- `static fun valueOf(字节码未给出: String): theme.ColorSchemeMode`

#### `MiuixTheme`

- `fun getColorScheme(): theme.Colors`
- `fun getTextStyles(): theme.TextStyles`
- `fun getColorSchemeMode(): theme.ColorSchemeMode`
- `fun isDynamicColor(): Boolean`

#### `MonetRoles`

- `constructor(p0: Long, p1: Long, p2: Long, p3: Long, p4: Long, p5: Long, p6: Long, p7: Long, p8: Long, p9: Long, p10: Long, p11: Long, p12: Long, p13: Long, p14: Long, p15: Long, p16: Long, p17: Long, p18: Long, p19: Long, p20: Long, p21: Long, p22: Long, p23: Long, p24: Long, p25: Long, p26: Long)`
- `val background: Long`
- `val error: Long`
- `val errorContainer: Long`
- `val onBackground: Long`
- `val onError: Long`
- `val onErrorContainer: Long`
- `val onPrimary: Long`
- `val onPrimaryContainer: Long`
- `val onPrimaryFixed: Long`
- `val onSecondary: Long`
- `val onSecondaryContainer: Long`
- `val onSurface: Long`
- `val onSurfaceVariant: Long`
- `val onTertiaryContainer: Long`
- `val outline: Long`
- `val outlineVariant: Long`
- `val primary: Long`
- `val primaryContainer: Long`
- `val primaryFixed: Long`
- `val secondary: Long`
- `val secondaryContainer: Long`
- `val surface: Long`
- `val surfaceContainer: Long`
- `val surfaceContainerHigh: Long`
- `val surfaceContainerHighest: Long`
- `val surfaceVariant: Long`
- `val tertiaryContainer: Long`
- `fun component1(): Long`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component4(): Long`
- `fun component5(): Long`
- `fun component6(): Long`
- `fun component7(): Long`
- `fun component8(): Long`
- `fun component9(): Long`
- `fun component10(): Long`
- `fun component11(): Long`
- `fun component12(): Long`
- `fun component13(): Long`
- `fun component14(): Long`
- `fun component15(): Long`
- `fun component16(): Long`
- `fun component17(): Long`
- `fun component18(): Long`
- `fun component19(): Long`
- `fun component20(): Long`
- `fun component21(): Long`
- `fun component22(): Long`
- `fun component23(): Long`
- `fun component24(): Long`
- `fun component25(): Long`
- `fun component26(): Long`
- `fun component27(): Long`
- `fun copy(字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Long): theme.MonetRoles`

#### `ThemeColorSpec`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<theme.ThemeColorSpec>`
- `static fun valueOf(字节码未给出: String): theme.ThemeColorSpec`

#### `ThemePaletteStyle`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<theme.ThemePaletteStyle>`
- `static fun valueOf(字节码未给出: String): theme.ThemePaletteStyle`

## 3. `top.yukonga.miuix.kmp.icon` — 图标

### 3.1 用法

`MiuixIcons` 是 `miuix-core` 中的单例，暴露 6 个**字重变体**对象：

| 变体 | 属性路径 |
| --- | --- |
| `Light` | `MiuixIcons.Light` |
| `Normal` | `MiuixIcons.Normal` |
| `Regular` | `MiuixIcons.Regular` |
| `Medium` | `MiuixIcons.Medium` |
| `Demibold` | `MiuixIcons.Demibold` |
| `Basic` | `MiuixIcons.Basic` |

`miuix-icons` 里每个图标是**扩展属性**，声明形如
`val MiuixIcons.Regular.Settings: ImageVector`（JVM 上是
`getSettings(MiuixIcons$Regular): ImageVector`）。可用字重：
`Light` / `Normal` / `Regular` / `Medium` / `Demibold`（**没有 `Basic`**）。

```kotlin
import androidx.compose.material3.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings

Icon(imageVector = MiuixIcons.Regular.Settings, contentDescription = null)
```

`miuix-ui` 另内置 7 个 `top.yukonga.miuix.kmp.icon.basic` 图标，
它们只挂在 `MiuixIcons.Basic` 上：

- `ArrowRight` — 变体：icon.MiuixIcons.Basic
- `ArrowUpDown` — 变体：icon.MiuixIcons.Basic
- `Check` — 变体：icon.MiuixIcons.Basic
- `Close` — 变体：icon.MiuixIcons.Basic
- `SearchCleanup` — 变体：icon.MiuixIcons.Basic
- `Search` — 变体：icon.MiuixIcons.Basic
- `Sidebar` — 变体：icon.MiuixIcons.Basic

### 3.2 `miuix-icons` 全部图标（共 **156** 个，字母序）

属性路径 = `MiuixIcons.{Light|Normal|Regular|Medium|Demibold}.<名字>`；
import 路径 = `top.yukonga.miuix.kmp.icon.extended.<名字>`。

| `Add` | `AddCircle` | `AddFolder` | `Alarm` |
| --- | --- | --- | --- |
| `Album` | `All` | `Answer` | `AppRecording` |
| `Back` | `Background` | `Backup` | `BankCards` |
| `Blocklist` | `CallRecording` | `Carrier` | `ChevronBackward` |
| `ChevronForward` | `Clear` | `Close` | `Close2` |
| `CloudFill` | `Community` | `Contacts` | `ContactsBook` |
| `ContactsCircle` | `ConvertFile` | `Copy` | `Create` |
| `Cut` | `Delete` | `Download` | `Edit` |
| `Email` | `ExpandLess` | `ExpandMore` | `Favorites` |
| `FavoritesFill` | `File` | `FileDownloads` | `Filter` |
| `Folder` | `FolderFill` | `Forward` | `GridView` |
| `Help` | `Hide` | `Home` | `HorizontalSplit` |
| `Image` | `Import` | `Info` | `Layers` |
| `Link` | `ListView` | `Location` | `Lock` |
| `MapAlbum` | `Merge` | `Messages` | `Mic` |
| `MicSlash` | `MindMap` | `Months` | `More` |
| `MoreCircle` | `MoveFile` | `Music` | `Notes` |
| `NotesFill` | `Ok` | `Paste` | `Pause` |
| `Phone` | `Photos` | `Pin` | `Play` |
| `Playlist` | `Promotions` | `Recent` | `Recording` |
| `RecordingTape` | `Redo` | `Refresh` | `Remove` |
| `RemoveContact` | `Rename` | `Replace` | `Reply` |
| `ReplyAll` | `Report` | `Reset` | `RotateLeft` |
| `Scan` | `ScreenCapture` | `ScreenMirroring` | `Search` |
| `SearchDevice` | `SelectAll` | `Send` | `Settings` |
| `Share` | `Show` | `Sidebar` | `Sort` |
| `Stopwatch` | `Store` | `Tasks` | `Th1` |
| `Th10` | `Th11` | `Th12` | `Th13` |
| `Th14` | `Th15` | `Th16` | `Th17` |
| `Th18` | `Th19` | `Th2` | `Th20` |
| `Th21` | `Th22` | `Th23` | `Th24` |
| `Th25` | `Th26` | `Th27` | `Th28` |
| `Th29` | `Th3` | `Th30` | `Th31` |
| `Th4` | `Th5` | `Th6` | `Th7` |
| `Th8` | `Th9` | `Theme` | `Timer` |
| `TopDownloads` | `Translate` | `Trim` | `Tune` |
| `Undo` | `Unlock` | `Unpin` | `Update` |
| `UploadCloud` | `VerticalSplit` | `VolumeOff` | `VolumeUp` |
| `Weeks` | `WorldClock` | `Years` | `ZoomOut` |

> 你列举的 `Star`、`Palette`、`DarkMode`、`Storage`、`Cloud`、`Menu`、
> `Check`、`Warning`、`Account`、`Apps`、`Calendar`、`Camera`、`Trash`、
> `Upload`、`Wifi`、`Eye`、`Heart`、`Bookmark`、`ArrowBackward` 等名字
> **在 0.9.3 的 `miuix-icons` 中不存在**。相近的替代见上表，
> 例如 `Favorites`/`FavoritesFill` 代替 `Star`、`Theme` 代替 `Palette`、
> `CloudFill` 代替 `Cloud`、`More`/`MoreCircle` 代替 `Menu`、
> `Ok` 代替 `Check`、`Info` 代替 `Warning`、`Contacts` 代替 `Account`、
> `Delete` 代替 `Trash`、`UploadCloud` 代替 `Upload`、
> `Lock`/`Unlock` 代替 `Wifi`/`Eye`/`Heart`/`Bookmark`（后四个无对应图标）、
> `ChevronBackward` 代替 `ArrowBackward`。

## 4. `top.yukonga.miuix.kmp.preference`（`miuix-preference`）

本包共 **14** 个公开顶层类。参数名完整可得（sourceInformation 覆盖良好）。

### 4.1 可用组件（`*Kt`）

#### `ArrowPreferenceKt`

- `static fun ArrowPreference(title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, startAction: Function2, endActions: Function3, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, onClick: () -> Unit, holdDownState: Boolean, enabled: Boolean)`

#### `CheckboxPreferenceKt`

- `static fun CheckboxPreference(title: String, checked: Boolean, onCheckedChange: Function1, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, checkboxColors: basic.CheckboxColors, startAction: Function2, endActions: Function3, checkboxLocation: preference.CheckboxLocation, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, holdDownState: Boolean, enabled: Boolean)`

#### `OverlayDropdownPreferenceKt`

- `static fun OverlayDropdownPreference(items: List, selectedIndex: Int, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, dropdownColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, renderInRootScaffold: Boolean, onExpandedChange: Function1, onSelectedIndexChange: Function1)`
- `static fun OverlayDropdownPreference-84PZ-4c(字节码未给出: basic.DropdownEntry, 字节码未给出: String, 字节码未给出: Modifier, 字节码未给出: basic.BasicComponentColors, 字节码未给出: String, 字节码未给出: basic.BasicComponentColors, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2, 字节码未给出: Function2, 字节码未给出: foundation.layout.PaddingValues, 字节码未给出: Dp, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Function1)`
- `static fun OverlayDropdownPreference-84PZ-4c(字节码未给出: List, 字节码未给出: String, 字节码未给出: Modifier, 字节码未给出: basic.BasicComponentColors, 字节码未给出: String, 字节码未给出: basic.BasicComponentColors, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2, 字节码未给出: Function2, 字节码未给出: foundation.layout.PaddingValues, 字节码未给出: Dp, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Function1)`

#### `OverlaySpinnerPreferenceKt`

- `static fun OverlaySpinnerPreference(items: List, selectedIndex: Int, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, spinnerColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, renderInRootScaffold: Boolean, onExpandedChange: Function1, onSelectedIndexChange: Function1)`
- `static fun OverlaySpinnerPreference-84PZ-4c(字节码未给出: basic.DropdownEntry, 字节码未给出: String, 字节码未给出: Modifier, 字节码未给出: basic.BasicComponentColors, 字节码未给出: String, 字节码未给出: basic.BasicComponentColors, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2, 字节码未给出: Function2, 字节码未给出: foundation.layout.PaddingValues, 字节码未给出: Dp, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Function1)`
- `static fun OverlaySpinnerPreference-84PZ-4c(字节码未给出: List, 字节码未给出: String, 字节码未给出: Modifier, 字节码未给出: basic.BasicComponentColors, 字节码未给出: String, 字节码未给出: basic.BasicComponentColors, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2, 字节码未给出: Function2, 字节码未给出: foundation.layout.PaddingValues, 字节码未给出: Dp, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Function1)`
- `static fun OverlaySpinnerPreference(items: List, selectedIndex: Int, title: String, dialogButtonString: String, modifier: Modifier, popupModifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, spinnerColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, enabled: Boolean, showValue: Boolean, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1, onSelectedIndexChange: Function1)`
- `static fun OverlaySpinnerPreference(items: basic.DropdownEntry, selectedIndex: String, title: String, modifier: Modifier, titleColor: Modifier, summary: basic.BasicComponentColors, summaryColor: String, spinnerColors: basic.BasicComponentColors, startAction: basic.DropdownColors, bottomAction: Function2, insideMargin: Function2, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, renderInRootScaffold: Boolean, onExpandedChange: Boolean, onSelectedIndexChange: Function1)`
- `static fun OverlaySpinnerPreference(items: List, selectedIndex: String, title: String, modifier: Modifier, titleColor: Modifier, summary: basic.BasicComponentColors, summaryColor: String, spinnerColors: basic.BasicComponentColors, startAction: basic.DropdownColors, bottomAction: Function2, insideMargin: Function2, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, renderInRootScaffold: Boolean, onExpandedChange: Boolean, onSelectedIndexChange: Function1)`

#### `RadioButtonPreferenceKt`

- `static fun RadioButtonPreference(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, radioButtonColors: basic.RadioButtonColors, startAction: Function2, endActions: Function3, radioButtonLocation: preference.RadioButtonLocation, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, holdDownState: Boolean, enabled: Boolean)`

#### `SliderPreferenceKt`

- `static fun SliderPreference(value: Float, onValueChange: Function1, modifier: Modifier, title: String, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, startAction: Function2, valueText: String, endActions: Function3, bottomAction: Function2, onClick: () -> Unit, holdDownState: Boolean, enabled: Boolean, valueRange: ranges.ClosedFloatingPointRange, steps: Int, onValueChangeFinished: () -> Unit, reverseDirection: Boolean, sliderHeight: ui.unit.Dp, sliderColors: basic.SliderColors, hapticEffect: basic.SliderDefaults.SliderHapticEffect, showKeyPoints: Boolean, keyPoints: List, magnetThreshold: Float, insideMargin: foundation.layout.PaddingValues)`
- `static fun RangeSliderPreference(value: ranges.ClosedFloatingPointRange, onValueChange: Function1, modifier: Modifier, title: String, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, startAction: Function2, valueText: String, endActions: Function3, bottomAction: Function2, onClick: () -> Unit, holdDownState: Boolean, enabled: Boolean, valueRange: ranges.ClosedFloatingPointRange, steps: Int, onValueChangeFinished: () -> Unit, sliderHeight: ui.unit.Dp, sliderColors: basic.SliderColors, hapticEffect: basic.SliderDefaults.SliderHapticEffect, showKeyPoints: Boolean, keyPoints: List, magnetThreshold: Float, insideMargin: foundation.layout.PaddingValues)`

#### `SwitchPreferenceKt`

- `static fun SwitchPreference(checked: Boolean, onCheckedChange: Function1, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, startAction: Function2, endActions: Function3, bottomAction: Function2, switchColors: basic.SwitchColors, insideMargin: foundation.layout.PaddingValues, holdDownState: Boolean, enabled: Boolean)`

#### `WindowDropdownPreferenceKt`

- `static fun WindowDropdownPreference(items: List, selectedIndex: Int, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, dropdownColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, onExpandedChange: Function1, onSelectedIndexChange: Function1)`
- `static fun WindowDropdownPreference(entry: basic.DropdownEntry, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, dropdownColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1)`
- `static fun WindowDropdownPreference(entry: List, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, dropdownColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1)`

#### `WindowSpinnerPreferenceKt`

- `static fun WindowSpinnerPreference(items: List, selectedIndex: Int, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, spinnerColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, onExpandedChange: Function1, onSelectedIndexChange: Function1)`
- `static fun WindowSpinnerPreference(entry: basic.DropdownEntry, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, spinnerColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1)`
- `static fun WindowSpinnerPreference(entry: List, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, spinnerColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1)`
- `static fun WindowSpinnerPreference(items: List, selectedIndex: Int, title: String, dialogButtonString: String, modifier: Modifier, popupModifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, spinnerColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, enabled: Boolean, showValue: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1, onSelectedIndexChange: Function1)`
- `static fun WindowSpinnerPreference(items: basic.DropdownEntry, selectedIndex: String, title: String, modifier: Modifier, titleColor: Modifier, summary: basic.BasicComponentColors, summaryColor: String, spinnerColors: basic.BasicComponentColors, startAction: basic.DropdownColors, bottomAction: Function2, insideMargin: Function2, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, onExpandedChange: Boolean, onSelectedIndexChange: Function1)`
- `static fun WindowSpinnerPreference(items: List, selectedIndex: String, title: String, modifier: Modifier, titleColor: Modifier, summary: basic.BasicComponentColors, summaryColor: String, spinnerColors: basic.BasicComponentColors, startAction: basic.DropdownColors, bottomAction: Function2, insideMargin: Function2, maxHeight: ui.unit.Dp, enabled: Boolean, showValue: Boolean, onExpandedChange: Boolean, onSelectedIndexChange: Function1)`

#### `StartActionSlotKt`

- `static fun StartActionSlot-djqs-MU(字节码未给出: Modifier, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Function2)`

### 4.2 其余 preference 公开类型（Defaults / 枚举）

#### `ArrowPreferenceDefaults`

- `fun endActionColors(): preference.EndActionColors`

#### `CheckboxLocation`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<preference.CheckboxLocation>`
- `static fun valueOf(字节码未给出: String): preference.CheckboxLocation`

#### `EndActionColors`

- `constructor(p0: Long, p1: Long)`
- `fun color-vNxB06k$top_yukonga_miuix_kmp_miuix_preference(字节码未给出: Boolean): Long`
- `fun copy-(字节码未给出: Long, 字节码未给出: Long): preference.EndActionColors`

#### `RadioButtonLocation`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<preference.RadioButtonLocation>`
- `static fun valueOf(字节码未给出: String): preference.RadioButtonLocation`

### 4.3 `miuix-preference` 中的 menu / popup 包

#### `OverlayDropdownMenuKt`

- `static fun OverlayDropdownMenu(entry: basic.DropdownEntry, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, dropdownColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1)`
- `static fun OverlayDropdownMenu(entry: List, title: String, modifier: Modifier, titleColor: basic.BasicComponentColors, summary: String, summaryColor: basic.BasicComponentColors, dropdownColors: basic.DropdownColors, startAction: Function2, bottomAction: Function2, insideMargin: foundation.layout.PaddingValues, maxHeight: ui.unit.Dp, enabled: Boolean, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1)`

#### `OverlayIconCascadingDropdownMenuKt`

- `static fun OverlayIconCascadingDropdownMenu(entry: basic.DropdownEntry, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`
- `static fun OverlayIconCascadingDropdownMenu(entry: List, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`

#### `OverlayIconDropdownMenuKt`

- `static fun OverlayIconDropdownMenu(entry: basic.DropdownEntry, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`
- `static fun OverlayIconDropdownMenu(entry: List, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, renderInRootScaffold: Boolean, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`

#### `WindowDropdownMenuKt`

- `static fun WindowDropdownMenu-b(字节码未给出: basic.DropdownEntry, 字节码未给出: String, 字节码未给出: Modifier, 字节码未给出: basic.BasicComponentColors, 字节码未给出: String, 字节码未给出: basic.BasicComponentColors, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2, 字节码未给出: Function2, 字节码未给出: foundation.layout.PaddingValues, 字节码未给出: Dp, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Function1)`
- `static fun WindowDropdownMenu-b(字节码未给出: List, 字节码未给出: String, 字节码未给出: Modifier, 字节码未给出: basic.BasicComponentColors, 字节码未给出: String, 字节码未给出: basic.BasicComponentColors, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2, 字节码未给出: Function2, 字节码未给出: foundation.layout.PaddingValues, 字节码未给出: Dp, 字节码未给出: Boolean, 字节码未给出: Boolean, 字节码未给出: Function1)`

#### `WindowIconCascadingDropdownMenuKt`

- `static fun WindowIconCascadingDropdownMenu(entry: basic.DropdownEntry, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`
- `static fun WindowIconCascadingDropdownMenu(entry: List, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`

#### `WindowIconDropdownMenuKt`

- `static fun WindowIconDropdownMenu(entry: basic.DropdownEntry, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`
- `static fun WindowIconDropdownMenu(entry: List, modifier: Modifier, enabled: Boolean, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, collapseOnSelection: Boolean, onExpandedChange: Function1, backgroundColor: ui.graphics.Color, cornerRadius: ui.unit.Dp, minHeight: ui.unit.Dp, minWidth: ui.unit.Dp, content: Function2)`

#### `DropdownEntriesContentKt`

- `static fun DropdownEntriesPopupContent(entries: List, dropdownColors: basic.DropdownColors, onItemClick: Function2)`
- `static fun dropdownEntriesDialogItems(字节码未给出: foundation.lazy.LazyListScope, 字节码未给出: List, 字节码未给出: basic.DropdownColors, 字节码未给出: Function2)`

#### `OverlayDropdownPopupKt`

- `static fun OverlayDropdownPopup(entry: basic.DropdownEntry, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, renderInRootScaffold: Boolean, collapseOnSelection: Boolean)`
- `static fun OverlayDropdownPopup(entry: List, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, renderInRootScaffold: Boolean, collapseOnSelection: Boolean)`
- `static fun OverlayDropdownDialog(entry: basic.DropdownEntry, title: String, dialogButtonString: String, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, dropdownColors: basic.DropdownColors, popupModifier: Modifier, renderInRootScaffold: Boolean, collapseOnSelection: Boolean)`
- `static fun OverlayDropdownDialog(entry: List, title: String, dialogButtonString: String, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, dropdownColors: basic.DropdownColors, popupModifier: Modifier, renderInRootScaffold: Boolean, collapseOnSelection: Boolean)`

#### `WindowDropdownPopupKt`

- `static fun WindowDropdownPopup(entry: basic.DropdownEntry, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, collapseOnSelection: Boolean)`
- `static fun WindowDropdownPopup(entry: List, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, maxHeight: ui.unit.Dp, dropdownColors: basic.DropdownColors, collapseOnSelection: Boolean)`
- `static fun WindowDropdownDialog(entry: basic.DropdownEntry, title: String, dialogButtonString: String, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, dropdownColors: basic.DropdownColors, popupModifier: Modifier, collapseOnSelection: Boolean)`
- `static fun WindowDropdownDialog(entry: List, title: String, dialogButtonString: String, show: Boolean, onDismiss: () -> Unit, onDismissFinished: () -> Unit, dropdownColors: basic.DropdownColors, popupModifier: Modifier, collapseOnSelection: Boolean)`

## 5. `top.yukonga.miuix.kmp.blur`（`miuix-blur`）

> **重要：0.9.3 的 blur API 与 Haze 库不同。** 没有 `hazeSource` / `hazeEffect`，
> 也没有 `BlurredEdgeTreatment`。0.9.3 用的是
> `rememberLayerBackdrop` + `Modifier.layerBackdrop` + `Modifier.drawBackdrop` /
> `Modifier.textureBlur` 这一套。下表所有名字均来自字节码，未列出的即不存在。

### 5.1 核心入口

| API | 所属类 | 说明 |
| --- | --- | --- |
| `rememberLayerBackdrop(record)` | `LayerBackdropKt` | `@Composable`，记录图层 |
| `Modifier.layerBackdrop(backdrop)` | `LayerBackdropModifierKt` | 绑定 Backdrop |
| `Modifier.drawBackdrop(…)` | `DrawBackdropModifierKt` | 绘制背景模糊 |
| `Modifier.textureBlur(…)` / `Modifier.textureEffect(…)` | `TextureEffectKt` | 纹理模糊效果 |
| `isRuntimeShaderSupported()` | `RuntimeShaderKt` | 普通函数，返回 `Boolean` |
| `BackdropEffectScope.blur(…)` / `noiseDither(…)` / `blendColors(…)` / `textureBlurEffect(…)` | `BackdropEffectsKt` | 效果作用域扩展 |

`Backdrop` 接口（`top.yukonga.miuix.kmp.blur.Backdrop`）只有两个成员：

```kotlin
interface Backdrop {
    val isCoordinatesDependent: Boolean
    fun drawBackdrop(
        drawScope: DrawScope,
        density: Density,
        coordinates: LayoutCoordinates?,
        draw: DrawScope.() -> Unit,
    )
}
```

`BlurDefaults` 的常量：`blurRadius: Float`、`noiseCoefficient: Float`、
`maxBlurRadius: Float`，以及 `@Composable BlurDefaults.blurColors(…)`。

### 5.2 全部公开类型（共 **35** 个）

#### `Backdrop`

- `val isCoordinatesDependent: Boolean`
- `val offsetResidualX: Float`
- `val offsetResidualY: Float`
- `fun drawBackdrop(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.unit.Density, 字节码未给出: ui.layout.LayoutCoordinates, 字节码未给出: Function1)`

#### `BackdropEffectScope`

- `var downscaleFactor: Int`
- `val layoutDirection: ui.unit.LayoutDirection`
- `var noiseCoefficient: Float`
- `var padding: Float`
- `var renderEffect: ui.graphics.RenderEffect`
- `val shape: ui.graphics.Shape`
- `fun getSize-NH(): Long`

#### `BackdropEffectScopeImpl`

- `constructor()`
- `val blendColorsBuffer: Array<Float>`
- `val blendModesBuffer: Array<Float>`
- `var blurBlendExpHi: Int`
- `var blurBlendExpLo: Int`
- `var blurBlendFactor: Float`
- `val blurParamOffsets: Array<Float>`
- `val blurParamWeights: Array<Float>`
- `val blurRawWeights: Array<Double>`
- `var cachedBlendColors: blur.BlurColors`
- `var cachedBlendResult: ui.graphics.RenderEffect`
- `var cachedBlurExp: Int`
- `var cachedBlurRadiusX: Float`
- `var cachedBlurRadiusY: Float`
- `var cachedBlurResult: ui.graphics.RenderEffect`
- `var cachedBlurSizeH: Float`
- `var cachedBlurSizeW: Float`
- `var cachedColorBrightness: Float`
- `var cachedColorContrast: Float`
- `var cachedColorResult: ui.graphics.RenderEffect`
- `var cachedColorSaturation: Float`
- `var density: Float`
- `var downscaleFactor: Int`
- `var fontScale: Float`
- `var forcedDownscaleExp: Int`
- `var layoutDirection: ui.unit.LayoutDirection`
- `var noiseCoefficient: Float`
- `var padding: Float`
- `var renderEffect: ui.graphics.RenderEffect`
- `var runtimeShaderCache: blur.RuntimeShaderCache`
- `fun getSize-NH(): Long`
- `fun obtainShaderOffsetsBuffer$top_yukonga_miuix_kmp_miuix_blur(): Array<Float>`
- `fun obtainShaderWeightsBuffer$top_yukonga_miuix_kmp_miuix_blur(): Array<Float>`
- `fun obtainRuntimeShader(字节码未给出: String, 字节码未给出: String): shader.RuntimeShader`
- `fun update(字节码未给出: ui.graphics.drawscope.DrawScope): Boolean`
- `fun apply(字节码未给出: Function1)`
- `fun reset()`
- `fun toPx(字节码未给出: Float): Float`
- `fun toPx-(字节码未给出: Long): Float`
- `fun roundToPx(字节码未给出: Float): Int`
- `fun roundToPx-(字节码未给出: Long): Int`
- `fun toDp(): Float`
- `fun toDp(字节码未给出: Float): Float`
- `fun toDp(字节码未给出: Long): Float`
- `fun toSp(): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toRect(字节码未给出: ui.unit.DpRect): ui.geometry.Rect`
- `fun toSize(字节码未给出: Long): Long`
- `fun toDpSize-k(字节码未给出: Long): Long`

#### `BackdropEffectScopeKt`

- `static fun colorControls(字节码未给出: blur.BackdropEffectScope, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float)`
- `static fun effect(字节码未给出: blur.BackdropEffectScope, 字节码未给出: ui.graphics.RenderEffect)`
- `static fun runtimeShaderEffect(字节码未给出: blur.BackdropEffectScope, 字节码未给出: String, 字节码未给出: String, 字节码未给出: String, 字节码未给出: Function1)`
- `static fun getImpl(字节码未给出: blur.BackdropEffectScope): blur.BackdropEffectScopeImpl`

#### `BackdropEffectsKt`

- `static fun blur(字节码未给出: blur.BackdropEffectScope, 字节码未给出: Float, 字节码未给出: Float)`
- `static fun noiseDither(字节码未给出: blur.BackdropEffectScope, 字节码未给出: Float)`
- `static fun blendColors(字节码未给出: blur.BackdropEffectScope, 字节码未给出: blur.BlurColors)`
- `static fun textureBlurEffect(字节码未给出: blur.BackdropEffectScope, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.BlurColors)`

#### `BlendColorEntry`

- `constructor(p0: Long, p1: Int)`
- `constructor(p0: Long)`
- `val color: Long`
- `val mode: Int`
- `fun component1(): Long`
- `fun component2(): Int`
- `fun copy-(字节码未给出: Long): blur.BlendColorEntry`

#### `BlurBlendMode`

- `val value: Int`
- `static fun toString(): String`
- `static fun hashCode(): Int`
- `static fun equals(字节码未给出: Int, 字节码未给出: Any): Boolean`
- `static fun constructor(): Int`
- `static fun box(): blur.BlurBlendMode`
- `fun unbox(): Int`
- `static fun equals(): Boolean`

#### `BlurColors`

- `constructor(p0: List, p1: Float, p2: Float, p3: Float)`
- `val blendColors: List`
- `val brightness: Float`
- `val contrast: Float`
- `val saturation: Float`
- `fun copy(字节码未给出: List, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float): blur.BlurColors`

#### `BlurDefaults`

- `val blurRadius: Float`
- `val maxBlurRadius: Float`
- `val noiseCoefficient: Float`
- `fun blurColors(字节码未给出: List, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float): blur.BlurColors`

#### `DrawBackdropModifierKt`

- `static fun drawBackdrop(字节码未给出: Modifier, 字节码未给出: blur.Backdrop, 字节码未给出: () -> Unit, 字节码未给出: Function1, 字节码未给出: Function1, 字节码未给出: Function1, 字节码未给出: Function1, 字节码未给出: Function2, 字节码未给出: Function1, 字节码未给出: Function1, 字节码未给出: Int, 字节码未给出: Boolean): Modifier`

#### `LayerBackdrop`

- `constructor(p0: ui.graphics.layer.GraphicsLayer, p1: Function1)`
- `val graphicsLayer: ui.graphics.layer.GraphicsLayer`
- `val isCoordinatesDependent: Boolean`
- `var layerCoordinates: ui.layout.LayoutCoordinates`
- `var offsetResidualX: Float`
- `var offsetResidualY: Float`
- `val onDraw: Function1`
- `fun drawBackdrop(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.unit.Density, 字节码未给出: ui.layout.LayoutCoordinates, 字节码未给出: Function1)`

#### `LayerBackdropKt`

- `static fun rememberLayerBackdrop(字节码未给出: ui.graphics.layer.GraphicsLayer, 字节码未给出: Function1): blur.LayerBackdrop`

#### `LayerBackdropModifierKt`

- `static fun layerBackdrop(字节码未给出: Modifier, 字节码未给出: blur.LayerBackdrop): Modifier`

#### `RuntimeShaderCache`

- `fun obtainRuntimeShader(字节码未给出: String, 字节码未给出: String): shader.RuntimeShader`

#### `RuntimeShaderCacheImpl`

- `constructor()`
- `fun obtainRuntimeShader(字节码未给出: String, 字节码未给出: String): shader.RuntimeShader`

#### `RuntimeShaderCacheKt`

- `val localRuntimeShaderCache: runtime.ProvidableCompositionLocal`

#### `RuntimeShaderKt`

- `val isRuntimeShaderSupported: Boolean`
- `static fun RuntimeShader(字节码未给出: String): shader.RuntimeShader`
- `static fun asComposeShader(字节码未给出: shader.RuntimeShader): android.graphics.Shader`
- `static fun asBrush(字节码未给出: shader.RuntimeShader): ui.graphics.ShaderBrush`

#### `TextureEffectKt`

- `static fun textureBlur(字节码未给出: Modifier, 字节码未给出: blur.Backdrop, 字节码未给出: ui.graphics.Shape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.BlurColors, 字节码未给出: blur.highlight.Highlight, 字节码未给出: Int, 字节码未给出: Boolean): Modifier`
- `static fun textureBlur(字节码未给出: Modifier, 字节码未给出: blur.Backdrop, 字节码未给出: ui.graphics.Shape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.BlurColors, 字节码未给出: blur.highlight.Highlight, 字节码未给出: Int, 字节码未给出: Boolean): Modifier`
- `static fun textureEffect(字节码未给出: Modifier, 字节码未给出: blur.Backdrop, 字节码未给出: ui.graphics.Shape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.BlurColors, 字节码未给出: blur.highlight.Highlight, 字节码未给出: Int, 字节码未给出: Boolean): Modifier`
- `static fun textureEffect(字节码未给出: Modifier, 字节码未给出: blur.Backdrop, 字节码未给出: ui.graphics.Shape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.BlurColors, 字节码未给出: blur.highlight.Highlight, 字节码未给出: Int, 字节码未给出: Boolean): Modifier`

#### `BloomStroke`

- `constructor(p0: Long, p1: Int, p2: Float, p3: blur.highlight.LightSource, p4: blur.highlight.LightSource, p5: Boolean)`
- `val blendMode: Int`
- `val color: Long`
- `val dualPeak: Boolean`
- `val innerBlurRadius: Float`
- `val primaryLight: blur.highlight.LightSource`
- `val secondaryLight: blur.highlight.LightSource`
- `fun createShader(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.graphics.Shape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.RuntimeShaderCache): android.graphics.Shader`
- `fun component1(): Long`
- `fun component2(): Int`
- `fun component3(): Float`
- `fun copy(字节码未给出: Long, 字节码未给出: Int, 字节码未给出: Float, 字节码未给出: blur.highlight.LightSource, 字节码未给出: blur.highlight.LightSource, 字节码未给出: Boolean): blur.highlight.BloomStroke`

#### `Highlight`

- `constructor(p0: Float, p1: Float, p2: blur.highlight.HighlightStyle)`
- `val alpha: Float`
- `val style: blur.highlight.HighlightStyle`
- `val width: Float`
- `fun component1(): Float`
- `fun copy(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.highlight.HighlightStyle): blur.highlight.Highlight`

#### `HighlightDrawingKt`

- `static fun drawHighlight(字节码未给出: ui.graphics.drawscope.ContentDrawScope, 字节码未给出: blur.highlight.Highlight, 字节码未给出: ui.graphics.Shape, 字节码未给出: blur.RuntimeShaderCache, 字节码未给出: ui.graphics.Paint)`

#### `HighlightStyle`

- `val blendMode: Int`
- `val color: Long`
- `fun createShader(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.graphics.Shape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: blur.RuntimeShaderCache): android.graphics.Shader`

#### `HighlightStyleKt`

- `static fun applyLightUniforms(字节码未给出: shader.RuntimeShader, 字节码未给出: String, 字节码未给出: blur.highlight.LightSource, 字节码未给出: Boolean)`

#### `LightPosition`

- `constructor(p0: Float, p1: Float, p2: Float)`
- `val x: Float`
- `val y: Float`
- `val z: Float`
- `fun copy(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float): blur.highlight.LightPosition`

#### `LightSource`

- `constructor(p0: blur.highlight.LightPosition, p1: Long, p2: Float)`
- `val color: Long`
- `val intensity: Float`
- `val position: blur.highlight.LightPosition`
- `fun component2(): Long`
- `fun copy(字节码未给出: blur.highlight.LightPosition, 字节码未给出: Long, 字节码未给出: Float): blur.highlight.LightSource`

#### `TiltLightKt`

- `static fun rememberTiltLight(字节码未给出: blur.highlight.LightPosition, 字节码未给出: Long, 字节码未给出: Float, 字节码未给出: Float): blur.highlight.LightSource`

#### `BlurEffectKt`

- `static fun createBlurEffect(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Int, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Long, 字节码未给出: blur.BackdropEffectScopeImpl): ui.graphics.RenderEffect`
- `static fun downScaleExpFor(字节码未给出: Float): Int`
- `static fun adjustedVarianceForExp(字节码未给出: Float): Float`
- `static fun computeDownScaleBlend(字节码未给出: Float): blur.internal.DownScaleBlend`
- `static fun computeBlurParamsInto(字节码未给出: Float, 字节码未给出: Array<Double>, 字节码未给出: Array<Float>, 字节码未给出: Array<Float>): Int`

#### `DownScaleBlend`

- `constructor(p0: Int, p1: Int, p2: Float)`
- `val blend: Float`
- `val expHi: Int`
- `val expLo: Int`
- `fun copy(字节码未给出: Int, 字节码未给出: Int, 字节码未给出: Float): blur.internal.DownScaleBlend`

#### `InverseLayerScope`

- `constructor()`
- `var alpha: Float`
- `var ambientShadowColor: Long`
- `var blendMode: Int`
- `var cameraDistance: Float`
- `var clip: Boolean`
- `var colorFilter: ui.graphics.ColorFilter`
- `var density: Float`
- `var fontScale: Float`
- `var renderEffect: ui.graphics.RenderEffect`
- `var rotationX: Float`
- `var rotationY: Float`
- `var rotationZ: Float`
- `var scaleX: Float`
- `var scaleY: Float`
- `var shadowElevation: Float`
- `var shape: ui.graphics.Shape`
- `var spotShadowColor: Long`
- `var transformOrigin: Long`
- `var translationX: Float`
- `var translationY: Float`
- `fun getSize-NH(): Long`
- `fun getCompositingStrategy-(): Int`
- `fun inverseTransform-JM5-EMQ(字节码未给出: ui.graphics.drawscope.DrawTransform, 字节码未给出: ui.unit.Density, 字节码未给出: Long, 字节码未给出: Function1)`
- `fun reset()`
- `fun toPx(字节码未给出: Float): Float`
- `fun toPx-(字节码未给出: Long): Float`
- `fun roundToPx(字节码未给出: Float): Int`
- `fun roundToPx-(字节码未给出: Long): Int`
- `fun toDp(): Float`
- `fun toDp(字节码未给出: Float): Float`
- `fun toDp(字节码未给出: Long): Float`
- `fun toSp(): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toRect(字节码未给出: ui.unit.DpRect): ui.geometry.Rect`
- `fun toSize(字节码未给出: Long): Long`
- `fun toDpSize-k(字节码未给出: Long): Long`

#### `LayerRecorderKt`

- `static fun recordLayer(字节码未给出: ui.node.DelegatableNode, 字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.graphics.layer.GraphicsLayer, 字节码未给出: Long, 字节码未给出: Function1)`

#### `RenderEffectCompat_androidKt`

- `static fun chain(字节码未给出: ui.graphics.RenderEffect, 字节码未给出: ui.graphics.RenderEffect): ui.graphics.RenderEffect`
- `static fun runtimeShaderEffect(字节码未给出: shader.RuntimeShader, 字节码未给出: String): ui.graphics.RenderEffect`

#### `ShadersKt`

- `val bLEND_MODE_SHADER_EXTENDED: String`
- `val bLEND_MODE_SHADER_STANDARD: String`
- `val bLOOM_STROKE_SHADER_DUAL: String`
- `val bLOOM_STROKE_SHADER_SINGLE: String`
- `val bLUR_SHADER_BY_TAP: Array<String>`
- `static fun buildBlurShader(): String`
- `static fun buildBloomStrokeShader(字节码未给出: Boolean): String`
- `static fun buildBlendModeShader(字节码未给出: Boolean): String`

#### `ShapeProvider`

- `constructor(p0: () -> Unit)`
- `val innerShape: ui.graphics.Shape`
- `val shape: ui.graphics.Shape`
- `var shapeBlock: () -> Unit`

#### `DeviceTilt`

- `constructor(p0: Float, p1: Float, p2: Float, p3: Float)`
- `val gravityX: Float`
- `val gravityY: Float`
- `val pitch: Float`
- `val roll: Float`
- `fun copy(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float): blur.sensor.DeviceTilt`

#### `DeviceTilt_androidKt`

- `static fun rememberDeviceTilt(字节码未给出: Float): runtime.State`

## 6. `com.kyant.backdrop`（`io.github.kyant0:backdrop:2.0.0`）

> **包名冲突警告：** `miuix-blur` 内部**自带一份**
> `top.yukonga.miuix.kmp.blur.Backdrop`，与 `com.kyant.backdrop.Backdrop`
> 同名不同包；`Modifier.drawBackdrop` 也有两份。同时 import 两者会编译失败。
> 用 Miuix 组件时优先用 `top.yukonga.miuix.kmp.blur.*`。

### 6.1 核心入口

| API | 所属类 |
| --- | --- |
| `rememberLayerBackdrop(graphicsLayer, record)` | `backdrops.LayerBackdropKt` |
| `Modifier.layerBackdrop(backdrop)` | `backdrops.LayerBackdropModifierKt` |
| `rememberBackdrop(...)` | `backdrops.BackdropKt` |
| `rememberCanvasBackdrop(...)` | `backdrops.CanvasBackdropKt` |
| `Modifier.drawBackdrop(...)` / `drawPlainBackdrop(...)` | `DrawBackdropModifierKt` |
| `isRenderEffectSupported()` / `isRuntimeShaderSupported()` | `PlatformKt` |

`Backdrop` 接口（`com.kyant.backdrop.Backdrop`）的成员：

```kotlin
interface Backdrop {
    val isCoordinatesDependent: Boolean
    fun drawBackdrop(
        drawScope: DrawScope,
        density: Density,
        coordinates: LayoutCoordinates,
        draw: DrawScope.() -> Unit,
    )
}
```

> 字节码里**没有** `record` / `tint` / `blur` 这些成员名。
> `record` 是 `rememberLayerBackdrop` 的 lambda 参数概念；
> `tint` 在本版本不存在（颜色处理走 `effects` 包里的 `blendColors`）。

### 6.2 全部公开类型（共 **42** 个）

#### `AndroidRuntimeShader`

- `constructor(p0: android.graphics.RuntimeShader)`
- `val shader: android.graphics.RuntimeShader`

#### `Backdrop`

- `val isCoordinatesDependent: Boolean`
- `fun drawBackdrop(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.unit.Density, 字节码未给出: ui.layout.LayoutCoordinates, 字节码未给出: Function1)`

#### `BackdropEffectScope`

- `val layoutDirection: ui.unit.LayoutDirection`
- `var padding: Float`
- `var renderEffect: ui.graphics.RenderEffect`
- `val shape: ui.graphics.Shape`
- `fun getSize-NH(): Long`

#### `BackdropEffectScopeImpl`

- `constructor()`
- `var density: Float`
- `var fontScale: Float`
- `var layoutDirection: ui.unit.LayoutDirection`
- `var padding: Float`
- `var renderEffect: ui.graphics.RenderEffect`
- `fun getSize-NH(): Long`
- `fun obtainRuntimeShader(字节码未给出: String, 字节码未给出: String): backdrop.RuntimeShader`
- `fun update(字节码未给出: ui.graphics.drawscope.DrawScope): Boolean`
- `fun apply(字节码未给出: Function1)`
- `fun reset()`
- `fun toPx(字节码未给出: Float): Float`
- `fun toPx-(字节码未给出: Long): Float`
- `fun roundToPx(字节码未给出: Float): Int`
- `fun roundToPx-(字节码未给出: Long): Int`
- `fun toDp(): Float`
- `fun toDp(字节码未给出: Float): Float`
- `fun toDp(字节码未给出: Long): Float`
- `fun toSp(): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toRect(字节码未给出: ui.unit.DpRect): ui.geometry.Rect`
- `fun toSize(字节码未给出: Long): Long`
- `fun toDpSize-k(字节码未给出: Long): Long`

#### `DrawBackdropModifierKt`

- `static fun drawPlainBackdrop(字节码未给出: Modifier, 字节码未给出: backdrop.Backdrop, 字节码未给出: () -> Unit, 字节码未给出: Function1, 字节码未给出: Function1, 字节码未给出: backdrop.backdrops.LayerBackdrop, 字节码未给出: Function1, 字节码未给出: Function2, 字节码未给出: Function1, 字节码未给出: Function1): Modifier`
- `static fun drawBackdrop(字节码未给出: Modifier, 字节码未给出: backdrop.Backdrop, 字节码未给出: () -> Unit, 字节码未给出: Function1, 字节码未给出: () -> Unit, 字节码未给出: () -> Unit, 字节码未给出: () -> Unit, 字节码未给出: Function1, 字节码未给出: backdrop.backdrops.LayerBackdrop, 字节码未给出: Function1, 字节码未给出: Function2, 字节码未给出: Function1, 字节码未给出: Function1): Modifier`

#### `PlatformKt`

- `val isRenderEffectSupported: Boolean`
- `val isRuntimeShaderSupported: Boolean`

#### `RuntimeShader`


#### `RuntimeShaderCache`

- `fun obtainRuntimeShader(字节码未给出: String, 字节码未给出: String): backdrop.RuntimeShader`

#### `RuntimeShaderCacheImpl`

- `constructor()`
- `fun obtainRuntimeShader(字节码未给出: String, 字节码未给出: String): backdrop.RuntimeShader`
- `fun clear()`

#### `RuntimeShaderKt`

- `static fun RuntimeShader(字节码未给出: String): backdrop.RuntimeShader`
- `static fun asComposeShader(字节码未给出: backdrop.RuntimeShader): android.graphics.Shader`
- `static fun asAndroidRuntimeShader(字节码未给出: backdrop.RuntimeShader): android.graphics.RuntimeShader`

#### `BackdropKt`

- `static fun rememberBackdrop(字节码未给出: backdrop.Backdrop, 字节码未给出: Function2): backdrop.Backdrop`

#### `CanvasBackdropKt`

- `static fun rememberCanvasBackdrop(字节码未给出: Function1): backdrop.Backdrop`

#### `CombinedBackdropKt`

- `static fun rememberCombinedBackdrop(字节码未给出: backdrop.Backdrop, 字节码未给出: backdrop.Backdrop): backdrop.Backdrop`
- `static fun rememberCombinedBackdrop(字节码未给出: backdrop.Backdrop, 字节码未给出: backdrop.Backdrop, 字节码未给出: backdrop.Backdrop): backdrop.Backdrop`
- `static fun rememberCombinedBackdrop(字节码未给出: Array<backdrop.Backdrop>): backdrop.Backdrop`

#### `EmptyBackdropKt`

- `static fun emptyBackdrop(): backdrop.Backdrop`

#### `LayerBackdrop`

- `constructor(p0: ui.graphics.layer.GraphicsLayer, p1: Function1)`
- `val graphicsLayer: ui.graphics.layer.GraphicsLayer`
- `val isCoordinatesDependent: Boolean`
- `var layerCoordinates: ui.layout.LayoutCoordinates`
- `val onDraw: Function1`
- `fun drawBackdrop(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.unit.Density, 字节码未给出: ui.layout.LayoutCoordinates, 字节码未给出: Function1)`

#### `LayerBackdropKt`

- `static fun rememberLayerBackdrop(字节码未给出: ui.graphics.layer.GraphicsLayer, 字节码未给出: Function1): backdrop.backdrops.LayerBackdrop`

#### `LayerBackdropModifierKt`

- `static fun layerBackdrop(字节码未给出: Modifier, 字节码未给出: backdrop.backdrops.LayerBackdrop): Modifier`

#### `BlurKt`

- `static fun blur(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: Float)`

#### `ColorFilterKt`

- `static fun colorFilter(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: ui.graphics.ColorFilter)`
- `static fun opacity(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: Float)`
- `static fun colorControls(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float)`
- `static fun vibrancy(字节码未给出: backdrop.BackdropEffectScope)`

#### `LensKt`

- `static fun lens(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Boolean, 字节码未给出: Boolean)`

#### `RenderEffectKt`

- `static fun effect(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: ui.graphics.RenderEffect)`
- `static fun runtimeShaderEffect(字节码未给出: backdrop.BackdropEffectScope, 字节码未给出: String, 字节码未给出: String, 字节码未给出: String, 字节码未给出: Function1)`

#### `Highlight`

- `constructor(p0: Float, p1: Float, p2: Float, p3: backdrop.highlight.HighlightStyle)`
- `val alpha: Float`
- `val blurRadius: Float`
- `val style: backdrop.highlight.HighlightStyle`
- `val width: Float`
- `fun component1(): Float`
- `fun component2(): Float`
- `fun copy(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: backdrop.highlight.HighlightStyle): backdrop.highlight.Highlight`

#### `HighlightElement`

- `constructor(p0: backdrop.internal.ShapeProvider, p1: () -> Unit)`
- `val highlight: () -> Unit`
- `val shapeProvider: backdrop.internal.ShapeProvider`
- `fun create(): backdrop.highlight.HighlightNode`
- `fun update(字节码未给出: backdrop.highlight.HighlightNode)`
- `fun inspectableProperties(字节码未给出: ui.platform.InspectorInfo)`
- `fun create(): ui.Modifier.Node`
- `fun update(字节码未给出: ui.Modifier.Node)`

#### `HighlightNode`

- `constructor(p0: backdrop.internal.ShapeProvider, p1: () -> Unit)`
- `var highlight: () -> Unit`
- `var shapeProvider: backdrop.internal.ShapeProvider`
- `val shouldAutoInvalidate: Boolean`
- `fun draw(字节码未给出: ui.graphics.drawscope.ContentDrawScope)`
- `fun onAttach()`
- `fun onDetach()`
- `fun onMeasureResultChanged()`

#### `HighlightStyle`

- `val blendMode: Int`
- `val color: Long`
- `fun createShader(字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.graphics.Shape, 字节码未给出: backdrop.RuntimeShaderCache): backdrop.RuntimeShader`

#### `InverseLayerScope`

- `constructor()`
- `var alpha: Float`
- `var ambientShadowColor: Long`
- `var blendMode: Int`
- `var cameraDistance: Float`
- `var clip: Boolean`
- `var colorFilter: ui.graphics.ColorFilter`
- `var density: Float`
- `var fontScale: Float`
- `var renderEffect: ui.graphics.RenderEffect`
- `var rotationX: Float`
- `var rotationY: Float`
- `var rotationZ: Float`
- `var scaleX: Float`
- `var scaleY: Float`
- `var shadowElevation: Float`
- `var shape: ui.graphics.Shape`
- `var spotShadowColor: Long`
- `var transformOrigin: Long`
- `var translationX: Float`
- `var translationY: Float`
- `fun getSize-NH(): Long`
- `fun getCompositingStrategy-(): Int`
- `fun inverseTransform(字节码未给出: ui.graphics.drawscope.DrawTransform, 字节码未给出: ui.unit.Density, 字节码未给出: Function1)`
- `fun reset()`
- `fun toPx(字节码未给出: Float): Float`
- `fun toPx-(字节码未给出: Long): Float`
- `fun roundToPx(字节码未给出: Float): Int`
- `fun roundToPx-(字节码未给出: Long): Int`
- `fun toDp(): Float`
- `fun toDp(字节码未给出: Float): Float`
- `fun toDp(字节码未给出: Long): Float`
- `fun toSp(): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toSp(字节码未给出: Float): Long`
- `fun toRect(字节码未给出: ui.unit.DpRect): ui.geometry.Rect`
- `fun toSize(字节码未给出: Long): Long`
- `fun toDpSize-k(字节码未给出: Long): Long`

#### `LayerRecorderKt`

- `static fun recordLayer(字节码未给出: ui.node.DelegatableNode, 字节码未给出: ui.graphics.drawscope.DrawScope, 字节码未给出: ui.graphics.layer.GraphicsLayer, 字节码未给出: Long, 字节码未给出: Function1)`

#### `OutlineKt`

- `static fun clipOutline(字节码未给出: ui.graphics.Canvas, 字节码未给出: ui.graphics.Outline, 字节码未给出: ui.graphics.Path)`

#### `PaintKt`

- `static fun blur(字节码未给出: ui.graphics.Paint, 字节码未给出: Float)`

#### `RenderEffectKt`

- `static fun chain(字节码未给出: ui.graphics.RenderEffect, 字节码未给出: ui.graphics.RenderEffect): ui.graphics.RenderEffect`
- `static fun RuntimeShaderEffect(字节码未给出: backdrop.RuntimeShader, 字节码未给出: String): ui.graphics.RenderEffect`
- `static fun ColorFilterEffect(字节码未给出: ui.graphics.RenderEffect, 字节码未给出: ui.graphics.ColorFilter): ui.graphics.RenderEffect`

#### `ShadersKt`

- `val roundedRectRefractionWithDispersionShaderString: String`

#### `ShapeProvider`

- `constructor(p0: () -> Unit)`
- `val innerShape: ui.graphics.Shape`
- `val shape: ui.graphics.Shape`
- `val shapeBlock: () -> Unit`

#### `InnerShadow`

- `constructor(p0: Float, p1: Long, p2: Long, p3: Float, p4: Int)`
- `constructor(p0: Float, p1: Long, p2: Long, p3: Float)`
- `val alpha: Float`
- `val blendMode: Int`
- `val color: Long`
- `val offset: Long`
- `val radius: Float`
- `fun component1(): Float`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component5(): Int`
- `fun copy(字节码未给出: Float, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Float): backdrop.shadow.InnerShadow`

#### `InnerShadowElement`

- `constructor(p0: backdrop.internal.ShapeProvider, p1: () -> Unit)`
- `val shadow: () -> Unit`
- `val shapeProvider: backdrop.internal.ShapeProvider`
- `fun create(): backdrop.shadow.InnerShadowNode`
- `fun update(字节码未给出: backdrop.shadow.InnerShadowNode)`
- `fun inspectableProperties(字节码未给出: ui.platform.InspectorInfo)`
- `fun create(): ui.Modifier.Node`
- `fun update(字节码未给出: ui.Modifier.Node)`

#### `InnerShadowKt`

- `static fun lerp(字节码未给出: backdrop.shadow.InnerShadow, 字节码未给出: backdrop.shadow.InnerShadow, 字节码未给出: Float): backdrop.shadow.InnerShadow`

#### `InnerShadowNode`

- `constructor(p0: backdrop.internal.ShapeProvider, p1: () -> Unit)`
- `var shadow: () -> Unit`
- `var shapeProvider: backdrop.internal.ShapeProvider`
- `val shouldAutoInvalidate: Boolean`
- `fun draw(字节码未给出: ui.graphics.drawscope.ContentDrawScope)`
- `fun onAttach()`
- `fun onDetach()`
- `fun onMeasureResultChanged()`

#### `Shadow`

- `constructor(p0: Float, p1: Long, p2: Long, p3: Float, p4: Int)`
- `constructor(p0: Float, p1: Long, p2: Long, p3: Float)`
- `val alpha: Float`
- `val blendMode: Int`
- `val color: Long`
- `val offset: Long`
- `val radius: Float`
- `fun component1(): Float`
- `fun component2(): Long`
- `fun component3(): Long`
- `fun component5(): Int`
- `fun copy(字节码未给出: Float, 字节码未给出: Long, 字节码未给出: Long, 字节码未给出: Float): backdrop.shadow.Shadow`

#### `ShadowElement`

- `constructor(p0: backdrop.internal.ShapeProvider, p1: () -> Unit)`
- `val shadow: () -> Unit`
- `val shapeProvider: backdrop.internal.ShapeProvider`
- `fun create(): backdrop.shadow.ShadowNode`
- `fun update(字节码未给出: backdrop.shadow.ShadowNode)`
- `fun inspectableProperties(字节码未给出: ui.platform.InspectorInfo)`
- `fun create(): ui.Modifier.Node`
- `fun update(字节码未给出: ui.Modifier.Node)`

#### `ShadowNode`

- `constructor(p0: backdrop.internal.ShapeProvider, p1: () -> Unit)`
- `var shadow: () -> Unit`
- `var shapeProvider: backdrop.internal.ShapeProvider`
- `val shouldAutoInvalidate: Boolean`
- `fun draw(字节码未给出: ui.graphics.drawscope.ContentDrawScope)`
- `fun onAttach()`
- `fun onDetach()`
- `fun onMeasureResultChanged()`

## 7. `com.kyant.shapes`（`io.github.kyant0:shapes:1.2.0`）

> **没有 `MaterialShape`，也没有 `SmoothRoundedPolygon`。**
> 本版本公开的是 `RoundedRectangularShape` 及其子类，全都实现
> `androidx.compose.ui.graphics.Shape`，可直接用于 `Modifier.clip(...)`、
> `Surface(shape = …)`、`Modifier.background(color, shape)`。

### 7.1 形状一览

| 类型 | 构造 | 说明 |
| --- | --- | --- |
| `RoundedRectangle` | `RoundedRectangle(cornerRadius: Dp, style: RoundedCornerStyle)` | 圆角矩形 |
| `UnevenRoundedRectangle` | `UnevenRoundedRectangle(cornerRadii: RectangleCornerRadii, style: RoundedCornerStyle)` | 四角不同；另有 `(topStart, topEnd, bottomEnd, bottomStart, style)` 重载 |
| `Capsule` | `Capsule(style: RoundedCornerStyle)` | 胶囊 |
| `Rectangle` | `Rectangle()` | 直角矩形 |
| `RectangleCornerRadii` | 四角 `Dp` 数据类 | 供 `UnevenRoundedRectangle` 用 |
| `RoundedCornerStyle` | 枚举 | 圆角风格（`Continuous` 等） |

```kotlin
import androidx.compose.ui.draw.clip
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle

Box(
    Modifier
        .clip(RoundedRectangle(16.dp, RoundedCornerStyle.Continuous))
        .background(Color.Gray)
)
```

### 7.2 全部公开类型（共 **13** 个）

#### `Capsule`

- `constructor(p0: shapes.RoundedCornerStyle)`
- `val style: shapes.RoundedCornerStyle`
- `fun corners(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): shapes.RoundedRectangularShape.Corners`
- `fun createOutline(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): ui.graphics.Outline`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.Capsule`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`

#### `ContinuousCurvatureRoundedRectangleCornerBuilder`

- `constructor(p0: Double, p1: Double)`
- `val arcFraction: Double`
- `val extendedFraction: Double`
- `fun getCornerBezierPoints(字节码未给出: Double, 字节码未给出: Double): Array<Double>`

#### `CopyKt`

- `static fun copy(字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangle`
- `static fun copy(字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: shapes.RectangleCornerRadii, 字节码未给出: shapes.RoundedCornerStyle): shapes.UnevenRoundedRectangle`
- `static fun copy(字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`

#### `LerpKt`

- `static fun lerp(字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`
- `static fun lerp(字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: shapes.RoundedRectangularShape, 字节码未给出: Float): shapes.RoundedRectangularShape`

#### `Rectangle`

- `val style: shapes.RoundedCornerStyle`
- `fun corners(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): shapes.RoundedRectangularShape.Corners`
- `fun createOutline(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): ui.graphics.Outline`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.Rectangle`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`

#### `RectangleCornerRadii`

- `constructor(p0: Float, p1: Float, p2: Float, p3: Float)`
- `val bottomEnd: Float`
- `val bottomStart: Float`
- `val topEnd: Float`
- `val topStart: Float`
- `fun component1(): Float`
- `fun component2(): Float`
- `fun component3(): Float`
- `fun component4(): Float`
- `fun copy(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float): shapes.RectangleCornerRadii`

#### `RectangleCornerRadiiKt`

- `static fun lerp(字节码未给出: shapes.RectangleCornerRadii, 字节码未给出: shapes.RectangleCornerRadii, 字节码未给出: Float): shapes.RectangleCornerRadii`

#### `RoundedCornerStyle`

- `val entries: enums.EnumEntries`
- `static fun values(): Array<shapes.RoundedCornerStyle>`
- `static fun valueOf(字节码未给出: String): shapes.RoundedCornerStyle`

#### `RoundedRectangle`

- `constructor(p0: Float, p1: shapes.RoundedCornerStyle)`
- `val cornerRadius: Float`
- `val style: shapes.RoundedCornerStyle`
- `fun corners(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): shapes.RoundedRectangularShape.Corners`
- `fun createOutline(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): ui.graphics.Outline`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangle`
- `fun copy(字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangle`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`

#### `RoundedRectangleOutlineKt`

- `static fun roundedRectangleOutline(字节码未给出: Long, 字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): ui.graphics.Outline`
- `static fun roundedRectangleOutline(字节码未给出: Long, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): ui.graphics.Outline`

#### `RoundedRectangularShape`

- `val style: shapes.RoundedCornerStyle`
- `fun corners(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): shapes.RoundedRectangularShape.Corners`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`

#### `UnevenRoundedRectangle`

- `constructor(p0: shapes.RectangleCornerRadii, p1: shapes.RoundedCornerStyle)`
- `constructor(p0: Float, p1: Float, p2: Float, p3: Float, p4: shapes.RoundedCornerStyle)`
- `val cornerRadii: shapes.RectangleCornerRadii`
- `val style: shapes.RoundedCornerStyle`
- `fun corners(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): shapes.RoundedRectangularShape.Corners`
- `fun createOutline(字节码未给出: Long, 字节码未给出: ui.unit.LayoutDirection, 字节码未给出: ui.unit.Density): ui.graphics.Outline`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.UnevenRoundedRectangle`
- `fun copy(字节码未给出: shapes.RectangleCornerRadii, 字节码未给出: shapes.RoundedCornerStyle): shapes.UnevenRoundedRectangle`
- `fun copy(字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: Float, 字节码未给出: shapes.RoundedCornerStyle): shapes.UnevenRoundedRectangle`
- `fun copy(字节码未给出: shapes.RoundedCornerStyle): shapes.RoundedRectangularShape`

## 8. 按名字查 API（完整限定名 → JVM 签名）

用来确认「某个名字在 0.9.3 里到底存不存在」。签名已剔除 Compose 的
`Composer` / `Int` 尾部参数。`$` 后缀的内部访问器已过滤。

### miuix-ui

```
top.yukonga.miuix.kmp.anim.AccelerateEasing
    <init>(Float)
    <init>(Float)
    transform(Float)
    <init>()
```
```
top.yukonga.miuix.kmp.anim.DecelerateEasing
    <init>(Float)
    <init>(Float)
    transform(Float)
    <init>()
```
```
top.yukonga.miuix.kmp.anim.MiuixEasingKt
    folmeSpring(Float, Float, Any)
```
```
top.yukonga.miuix.kmp.anim.SinOutEasingKt
    getSinOutEasing()
```
```
top.yukonga.miuix.kmp.basic.BadgeDefaults
    getSize()
    getLargeSize()
    getContainerColor()
    getContentColor()
```
```
top.yukonga.miuix.kmp.basic.BadgeKt
    BadgedBox(Function3, Modifier, Function3)
    Badge(Modifier, Long, Long, Function3)
    getBadgeWithContentHorizontalPadding()
    getBadgeWithContentHorizontalOffset()
    getBadgeWithContentVerticalOffset()
    getBadgeOffset()
    getBadgeTopRuler()
    getBadgeEndRuler()
    badgeBounds(Modifier)
```
```
top.yukonga.miuix.kmp.basic.BasicComponentColors
    getColor()
    getDisabledColor()
    color-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    component1()
    component2()
    copy-(Long, Long)
    <init>(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.BasicComponentDefaults
    getInsideMargin()
    titleColor(Long, Long)
    summaryColor(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ButtonColors
    getColor()
    getDisabledColor()
    getContentColor()
    getDisabledContentColor()
    component1()
    component2()
    component3()
    component4()
    copy(Long, Long, Long, Long)
    <init>(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ButtonDefaults
    getMinWidth()
    getMinHeight()
    getCornerRadius()
    getInsideMargin()
    buttonColors(Long, Long, Long, Long)
    buttonColorsPrimary(Long, Long, Long, Long)
    textButtonColors(Long, Long, Long, Long)
    textButtonColorsPrimary(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ButtonKt
    Button(() -> Unit, Modifier, Boolean, Float, Float, Float, basic.ButtonColors, foundation.layout.PaddingValues, foundation.interaction.MutableInteractionSource, foundation.Indication, Function3)
    TextButton(String, () -> Unit, Modifier, Boolean, Float, Float, Float, basic.TextButtonColors, foundation.layout.PaddingValues, TextStyle, foundation.interaction.MutableInteractionSource, foundation.Indication)
```
```
top.yukonga.miuix.kmp.basic.CardColors
    getColor()
    getContentColor()
    component1()
    component2()
    copy-(Long, Long)
    <init>(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.CardDefaults
    getCornerRadius()
    getInsideMargin()
    defaultColors(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.CardKt
    Card(Modifier, Float, foundation.layout.PaddingValues, basic.CardColors, Function3)
    Card(Modifier, Float, foundation.layout.PaddingValues, basic.CardColors, utils.PressFeedbackType, Boolean, Boolean, () -> Unit, () -> Unit, Function3)
```
```
top.yukonga.miuix.kmp.basic.CheckboxColors
    checkedForegroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    uncheckedForegroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    checkedBackgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    uncheckedBackgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    copy(Long, Long, Long, Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.CheckboxDefaults
    checkboxColors(Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.CheckboxKt
    Checkbox(ui.state.ToggleableState, () -> Unit, Modifier, basic.CheckboxColors, Boolean)
```
```
top.yukonga.miuix.kmp.basic.ColorPaletteKt
    ColorPalette-(Long, Function1, Modifier, Int, Int, Boolean, Boolean, Float, Float)
```
```
top.yukonga.miuix.kmp.basic.ColorPickerKt
    ColorPicker(Long, Function1, Modifier, Boolean, basic.SliderDefaults.SliderHapticEffect, basic.ColorSpace)
    HsvColorPicker(Long, Function1, Modifier, Boolean, basic.SliderDefaults.SliderHapticEffect)
    HsvHueSlider(Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    HsvSaturationSlider(Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    HsvValueSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    HsvAlphaSlider(Float, Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkHsvColorPicker(Long, Function1, Modifier, Boolean, basic.SliderDefaults.SliderHapticEffect)
    OkHsvHueSlider(Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkHsvSaturationSlider(Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkHsvValueSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkHsvAlphaSlider(Float, Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLabColorPicker(Long, Function1, Modifier, Boolean, basic.SliderDefaults.SliderHapticEffect)
    OkLchColorPicker(Long, Function1, Modifier, Boolean, basic.SliderDefaults.SliderHapticEffect)
    OkLchLightnessSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLchChromaSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLchHueSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLchAlphaSlider(Float, Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLabLightnessSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLabAChannelSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLabBChannelSlider(Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    OkLabAlphaSlider(Float, Float, Float, Float, Function1, basic.SliderDefaults.SliderHapticEffect)
    drawCheckerboard(Modifier, Float, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ColorSpace
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.basic.ComponentKt
    BasicComponent(Modifier, String, basic.BasicComponentColors, String, basic.BasicComponentColors, Function2, Function3, Function2, foundation.layout.PaddingValues, () -> Unit, String, ui.semantics.Role, Boolean, Boolean, foundation.interaction.MutableInteractionSource)
    BasicComponent(Modifier, Function2, Function3, Function2, foundation.layout.PaddingValues, () -> Unit, String, ui.semantics.Role, Boolean, Boolean, foundation.interaction.MutableInteractionSource, Function3)
```
```
top.yukonga.miuix.kmp.basic.DividerDefaults
    getThickness()
    getDividerColor()
```
```
top.yukonga.miuix.kmp.basic.DividerKt
    HorizontalDivider(Modifier, Float, Long)
    VerticalDivider(Modifier, Float, Long)
```
```
top.yukonga.miuix.kmp.basic.DropdownColors
    getContentColor()
    getSummaryColor()
    getContainerColor()
    getSelectedContentColor()
    getSelectedSummaryColor()
    getSelectedContainerColor()
    getSelectedIndicatorColor()
    component1()
    component2()
    component3()
    component4()
    component5()
    component6()
    component7()
    copy(Long, Long, Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.DropdownDefaults
    getMinHeight()
    getMinWidth()
    getCheckIconSize()
    getArrowSize()
    getChevronSize()
    getIconMinSize()
    getMaxItemTextWidth()
    getInsideHorizontalPadding()
    getDialogHorizontalPadding()
    getFirstLastVerticalPadding()
    getMiddleVerticalPadding()
    getIconEndPadding()
    getCheckIconStartPadding()
    dropdownColors(Long, Long, Long, Long, Long, Long, Long)
    dialogDropdownColors(Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.DropdownEntry
    <init>(List, Boolean)
    <init>(List, Boolean)
    getItems()
    getEnabled()
    copy(List, Boolean)
```
```
top.yukonga.miuix.kmp.basic.DropdownItem
    <init>(String, Boolean, Boolean, () -> Unit, Function3, String, List)
    <init>(String, Boolean, Boolean, () -> Unit, Function3, String, List)
    getText()
    getEnabled()
    getSelected()
    getOnClick()
    getIcon()
    getSummary()
    getChildren()
    <init>(Function3, String, String)
    <init>(Function3, String, String)
    copy(String, Boolean, Boolean, () -> Unit, Function3, String, List)
```
```
top.yukonga.miuix.kmp.basic.DropdownKt
    DropdownArrowEndAction(foundation.layout.RowScope, Long, Modifier)
    DropdownImpl(basic.DropdownItem, Int, Boolean, Int, basic.DropdownColors, Boolean, Boolean, Boolean, Boolean, Boolean, Function1)
    DropdownImpl(String, Int, Boolean, Int, basic.DropdownColors, Boolean, Boolean, Function1)
    SpinnerItemImpl(basic.DropdownItem, Int, Boolean, Int, basic.DropdownColors, Boolean, Function1)
    SpinnerColors$annotations()
    SpinnerEntry$annotations()
```
```
top.yukonga.miuix.kmp.basic.FabPlacement
    <init>()
    getLeft()
    getWidth()
    getHeight()
```
```
top.yukonga.miuix.kmp.basic.FabPosition
    toString()
    hashCode()
    equals(Int, Any)
    constructor()
    box()
    unbox()
    equals()
```
```
top.yukonga.miuix.kmp.basic.FloatingActionButtonDefaults
    getMinWidth()
    getMinHeight()
    getShadowElevation()
```
```
top.yukonga.miuix.kmp.basic.FloatingActionButtonKt
    FloatingActionButton(() -> Unit, Modifier, ui.graphics.Shape, Long, Float, Float, Float, Function2)
```
```
top.yukonga.miuix.kmp.basic.FloatingNavigationBarDefaults
    getHorizontalOutSidePadding()
    getShadowElevation()
    getHorizontalPadding()
    getItemSpacing()
    getIconSize()
    getIconPadding()
    getSelectedPressedAlpha()
    getUnselectedPressedAlpha()
    getUnselectedAlpha()
```
```
top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
    getCornerRadius()
    defaultColor()
    getOutSidePadding()
```
```
top.yukonga.miuix.kmp.basic.FloatingToolbarKt
    FloatingToolbar(Modifier, Long, Float, foundation.layout.PaddingValues, Float, Boolean, Function2)
```
```
top.yukonga.miuix.kmp.basic.IconButtonDefaults
    getMinWidth()
    getMinHeight()
    getCornerRadius()
```
```
top.yukonga.miuix.kmp.basic.IconButtonKt
    IconButton(() -> Unit, Modifier, Boolean, Boolean, Long, Float, Float, Float, Function2)
```
```
top.yukonga.miuix.kmp.basic.IconKt
    Icon(ui.graphics.vector.ImageVector, String, Modifier, Long)
    Icon(ui.graphics.ImageBitmap, String, Modifier, Long)
    Icon(ui.graphics.painter.Painter, String, Modifier, Long)
    Icon(ui.graphics.painter.Painter, ui.graphics.ColorProducer, String, Modifier)
```
```
top.yukonga.miuix.kmp.basic.ListPopupDefaults
    getFractionAnimationSpec()
    getAlphaEnterAnimationSpec()
    getAlphaExitAnimationSpec()
    getDimEnterAnimationSpec()
    getDimExitAnimationSpec()
    getResetAnimationSpec()
    getMinWidth()
    getMinPopupHeight()
    dropdownPositionProvider(Float, Float)
    getDropdownPositionProvider()
    getContextMenuPositionProvider()
```
```
top.yukonga.miuix.kmp.basic.ListPopupKt
    ListPopupColumn(Function2)
    safeTransformOrigin(Float, Float)
    rememberListPopupLayoutInfo(basic.PopupPositionProvider.Align, basic.PopupPositionProvider, ui.unit.IntRect, Long)
    ListPopupContent(Long, Function1, () -> Unit, () -> Unit, basic.PopupLayoutPosition, Long, Modifier, Function2)
    popupClipReveal(Modifier, () -> Unit, basic.PopupLayoutPosition, Float, Boolean)
```
```
top.yukonga.miuix.kmp.basic.ListPopupLayoutInfo
    getWindowBounds()
    getPopupMargin()
    getEffectiveTransformOrigin()
    getLocalTransformOrigin()
    getPopupLayoutPosition()
    component3()
    component4()
    copy(ui.unit.IntRect, ui.unit.IntRect, Long, Long, basic.PopupLayoutPosition)
    <init>(ui.unit.IntRect, ui.unit.IntRect, Long, Long, basic.PopupLayoutPosition)
```
```
top.yukonga.miuix.kmp.basic.MutableWindowInsets
    <init>(foundation.layout.WindowInsets)
    <init>(foundation.layout.WindowInsets)
    getInsets()
    setInsets(foundation.layout.WindowInsets)
    getLeft(ui.unit.Density, ui.unit.LayoutDirection)
    getTop(ui.unit.Density)
    getRight(ui.unit.Density, ui.unit.LayoutDirection)
    getBottom(ui.unit.Density)
    <init>()
```
```
top.yukonga.miuix.kmp.basic.NavigationBarDefaults
    getItemHeight()
    getIconSize()
    getLabelFontSize()
    getIconTopPadding()
    getBottomPadding()
    getSelectedPressedAlpha()
    getUnselectedPressedAlpha()
    getUnselectedAlpha()
```
```
top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.basic.NavigationBarKt
    NavigationBar(Modifier, Long, Boolean, Boolean, basic.NavigationBarDisplayMode, Function3)
    NavigationBarItem(foundation.layout.RowScope, Boolean, () -> Unit, ui.graphics.vector.ImageVector, String, Modifier, Boolean, Function2)
    FloatingNavigationBar(Modifier, Long, Float, ui.Alignment.Horizontal, Float, Float, Boolean, Boolean, Function2)
    FloatingNavigationBarItem(Boolean, () -> Unit, ui.graphics.vector.ImageVector, String, Modifier, Boolean, Function2)
    NavigationItemIcon(Function2, Modifier, Function3)
    getLocalNavigationBarDisplayMode()
```
```
top.yukonga.miuix.kmp.basic.NavigationItem
    <init>(String, ui.graphics.vector.ImageVector)
    getLabel()
    getIcon()
    copy(String, ui.graphics.vector.ImageVector)
```
```
top.yukonga.miuix.kmp.basic.NavigationRailDefaults
    getMinWidth()
    getExpandedWidth()
    getVerticalPadding()
    getHeaderSpacing()
    getIconSize()
    getIconTextSpacing()
    getItemVerticalPadding()
    getLabelFontSize()
    getExpandedLabelFontSize()
    getExpandedItemHorizontalMargin()
    getExpandedItemCornerRadius()
    getCollapsedIndicatorVerticalPadding()
    getExpandedItemContentHorizontalPadding()
    getExpandedItemContentVerticalPadding()
    getExpandedItemIconTextSpacing()
    getExpandContentDescription()
    getCollapseContentDescription()
```
```
top.yukonga.miuix.kmp.basic.NavigationRailExpandInfo
    getProgress()
    getCollapsedWidth()
    <init>(runtime.State, Float)
```
```
top.yukonga.miuix.kmp.basic.NavigationRailKt
    NavigationRail(Modifier, basic.NavigationRailState, Function3, Long, Boolean, Boolean, Float, Float, String, String, foundation.ScrollState, Function3)
    NavigationRailItem(Boolean, () -> Unit, ui.graphics.vector.ImageVector, String, Modifier, Boolean, Function2)
    getLocalNavigationRailExpandInfo()
    rememberNavigationRailState(basic.NavigationRailValue)
```
```
top.yukonga.miuix.kmp.basic.NavigationRailState
    <init>(basic.NavigationRailValue)
    <init>(basic.NavigationRailValue)
    getCurrentValue()
    isExpanded()
    expand()
    collapse()
    toggle()
    <init>()
```
```
top.yukonga.miuix.kmp.basic.NavigationRailValue
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.basic.NumberPickerColors
    selectedTextColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    unselectedTextColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    copy(Long, Long, Long, Long)
    <init>(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.NumberPickerDefaults
    getItemHeight()
    colors(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.NumberPickerKt
    NumberPicker(Int, Function1, Modifier, Boolean, ranges.IntRange, Function1, Int, Boolean, basic.NumberPickerColors, TextStyle, Float)
```
```
top.yukonga.miuix.kmp.basic.PopupLayoutPosition
    <init>(Boolean, Boolean, Boolean)
    getShowBelow()
    getShowAbove()
    isRightAligned()
    copy(Boolean, Boolean, Boolean)
```
```
top.yukonga.miuix.kmp.basic.PopupPositionProvider
    calculatePosition(ui.unit.IntRect, ui.unit.IntRect, ui.unit.LayoutDirection, Long, ui.unit.IntRect, basic.PopupPositionProvider.Align)
    getMargins()
```
```
top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
    foregroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    backgroundColor-0d7_KjU$top_yukonga_miuix_kmp_miuix_ui()
    copy(Long, Long, Long)
    <init>(Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
    getDefaultLinearProgressIndicatorHeight()
    getDefaultCircularProgressIndicatorStrokeWidth()
    getDefaultCircularProgressIndicatorSize()
    getDefaultInfiniteProgressIndicatorStrokeWidth()
    getDefaultInfiniteProgressIndicatorOrbitingDotSize()
    getDefaultInfiniteProgressIndicatorSize()
    progressIndicatorColors(Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ProgressIndicatorKt
    LinearProgressIndicator-(Modifier, Float, basic.ProgressIndicatorColors, Float)
    CircularProgressIndicator(Modifier, Float, basic.ProgressIndicatorColors, Float, Float)
    InfiniteProgressIndicator(Modifier, Long, Float, Float, Float)
```
```
top.yukonga.miuix.kmp.basic.PullToRefreshDefaults
    getColor()
    getCircleSize()
    getRefreshTexts()
    getRefreshTextStyle()
```
```
top.yukonga.miuix.kmp.basic.PullToRefreshKt
    PullToRefresh(Boolean, () -> Unit, Modifier, basic.PullToRefreshState, foundation.layout.PaddingValues, basic.ScrollBehavior, Long, Float, List, TextStyle, Function2)
    rememberPullToRefreshState()
    getLocalPullToRefreshState()
```
```
top.yukonga.miuix.kmp.basic.PullToRefreshState
    <init>(kotlinx.coroutines.CoroutineScope)
    getCoroutineScope$top_yukonga_miuix_kmp_miuix_ui()
    setCoroutineScope$top_yukonga_miuix_kmp_miuix_ui(kotlinx.coroutines.CoroutineScope)
    getMaxDragDistancePx$top_yukonga_miuix_kmp_miuix_ui()
    setMaxDragDistancePx$top_yukonga_miuix_kmp_miuix_ui(Float)
    getRefreshThresholdOffset$top_yukonga_miuix_kmp_miuix_ui()
    setRefreshThresholdOffset$top_yukonga_miuix_kmp_miuix_ui(Float)
    getDragOffset()
    setDragOffset(Float)
    getCachedNestedScrollConnection()
    setCachedNestedScrollConnection(ui.input.nestedscroll.NestedScrollConnection)
    getRefreshState()
    getPullProgress()
    getCurrentTouch$top_yukonga_miuix_kmp_miuix_ui()
    setCurrentTouch$top_yukonga_miuix_kmp_miuix_ui(Float)
    isRefreshing$top_yukonga_miuix_kmp_miuix_ui()
    setRefreshing$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    isTouching$top_yukonga_miuix_kmp_miuix_ui()
    setTouching$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    isRebounding$top_yukonga_miuix_kmp_miuix_ui()
    setRebounding$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    getRefreshCompleteAnimProgress$top_yukonga_miuix_kmp_miuix_ui()
    getAnimationJob$top_yukonga_miuix_kmp_miuix_ui()
    setAnimationJob$top_yukonga_miuix_kmp_miuix_ui(kotlinx.coroutines.Job)
    animateToSpring$top_yukonga_miuix_kmp_miuix_ui(Float, coroutines.Continuation)
    startRefreshing$top_yukonga_miuix_kmp_miuix_ui(() -> Unit, () -> Unit, coroutines.Continuation)
    showRefreshing$top_yukonga_miuix_kmp_miuix_ui(() -> Unit, coroutines.Continuation)
    finishRefreshing$top_yukonga_miuix_kmp_miuix_ui(() -> Unit, coroutines.Continuation)
    handlePointerRelease$top_yukonga_miuix_kmp_miuix_ui(() -> Unit, () -> Unit, coroutines.Continuation)
    resetToIdle$top_yukonga_miuix_kmp_miuix_ui()
    getOrCreateNestedScrollConnection$top_yukonga_miuix_kmp_miuix_ui(utils.OverScrollState)
```
```
top.yukonga.miuix.kmp.basic.RadioButtonColors
    color-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    copy-(Long, Long)
    <init>(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.RadioButtonDefaults
    radioButtonColors(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.RadioButtonKt
    RadioButton(Boolean, () -> Unit, Modifier, basic.RadioButtonColors, Boolean)
```
```
top.yukonga.miuix.kmp.basic.RangeSliderHapticState
    <init>()
    resetStart(Float, ranges.ClosedFloatingPointRange)
    resetEnd(Float, ranges.ClosedFloatingPointRange)
    inheritStartKeyPoint()
    inheritEndKeyPoint()
    handleStartHapticFeedback(Float, ranges.ClosedFloatingPointRange, basic.SliderDefaults.SliderHapticEffect, ui.hapticfeedback.HapticFeedback, Array<Float>, Boolean)
    handleEndHapticFeedback(Float, ranges.ClosedFloatingPointRange, basic.SliderDefaults.SliderHapticEffect, ui.hapticfeedback.HapticFeedback, Array<Float>, Boolean)
```
```
top.yukonga.miuix.kmp.basic.RichTooltipColors
    getContainerColor()
    getContentColor()
    getTitleContentColor()
    getActionContentColor()
    component1()
    component2()
    component3()
    component4()
    copy(Long, Long, Long, Long)
    <init>(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ScaffoldKt
    Scaffold(Modifier, Function2, Function2, Function2, Int, Function2, Int, Function2, Function2, Long, foundation.layout.WindowInsets, Function3)
    getVertical(ui.Alignment)
    getHorizontal(ui.Alignment)
    toAlignment()
```
```
top.yukonga.miuix.kmp.basic.ScrollBarAdapter
    getScrollOffset()
    getContentSize()
    getViewportSize()
    scrollTo(Double, coroutines.Continuation)
```
```
top.yukonga.miuix.kmp.basic.ScrollBarColors
    getThumbColor()
    getTrackColor()
    component1()
    component2()
    copy-(Long, Long)
    <init>(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ScrollBarDefaults
    getThumbWidth()
    getEndPadding()
    getThumbMinLength()
    getCornerRadius()
    getFadeDelayMillis()
    getFadeDurationMillis()
    getTouchTargetWidth()
    getDragThumbWidth()
    getThumbAlpha()
    getDragThumbAlpha()
    getDragAnimationDurationMillis()
    scrollBarColors-(Long, Long)
```
```
top.yukonga.miuix.kmp.basic.ScrollBarKt
    getMaxScrollOffset(basic.ScrollBarAdapter)
    getMaxScrollOffset$annotations(basic.ScrollBarAdapter)
    rememberScrollBarAdapter(foundation.ScrollState)
    rememberScrollBarAdapter(foundation.lazy.LazyListState)
    rememberScrollBarAdapter(foundation.lazy.grid.LazyGridState)
    VerticalScrollBar(basic.ScrollBarAdapter, Modifier, Boolean, foundation.layout.PaddingValues, basic.ScrollBarColors, Float, Float, Float, Float)
    HorizontalScrollBar(basic.ScrollBarAdapter, Modifier, Boolean, foundation.layout.PaddingValues, basic.ScrollBarColors, Float, Float, Float, Float)
```
```
top.yukonga.miuix.kmp.basic.ScrollBehavior
    getState()
    isPinned()
    getSnapAnimationSpec()
    getFlingAnimationSpec()
    getNestedScrollConnection()
```
```
top.yukonga.miuix.kmp.basic.SearchBarDefaults
    getInsideMargin()
    getInputFieldMinHeight()
    getInputFieldFontSize()
    getLeadingIconStartPadding()
    getLeadingIconEndPadding()
    getTrailingIconStartPadding()
    getTrailingIconEndPadding()
```
```
top.yukonga.miuix.kmp.basic.SearchBarKt
    SearchBar(Function2, Function1, Modifier, Long, Boolean, Function2, Function3)
    InputField(String, Function1, Function1, Boolean, Function1, Modifier, String, Boolean, TextStyle, Function2, Function2, foundation.interaction.MutableInteractionSource)
```
```
top.yukonga.miuix.kmp.basic.SliderColors
    foregroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    backgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    thumbColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    keyPointColor-0d7_KjU$top_yukonga_miuix_kmp_miuix_ui()
    keyPointForegroundColor-0d7_KjU$top_yukonga_miuix_kmp_miuix_ui()
    copy(Long, Long, Long, Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SliderDefaults
    getMinHeight()
    getKeyPointRadius()
    getDefaultHapticEffect()
    sliderColors(Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SliderHapticState
    <init>()
    reset(Float, ranges.ClosedFloatingPointRange, Float)
    handleHapticFeedback(Float, ranges.ClosedFloatingPointRange, basic.SliderDefaults.SliderHapticEffect, ui.hapticfeedback.HapticFeedback, Array<Float>, Boolean)
```
```
top.yukonga.miuix.kmp.basic.SliderKt
    Slider(Float, Function1, Modifier, Boolean, ranges.ClosedFloatingPointRange, Int, () -> Unit, Boolean, Float, basic.SliderColors, basic.SliderDefaults.SliderHapticEffect, Boolean, List, Float)
    VerticalSlider(Float, Function1, Modifier, Boolean, ranges.ClosedFloatingPointRange, Int, () -> Unit, Boolean, Float, basic.SliderColors, Boolean, basic.SliderDefaults.SliderHapticEffect, Boolean, List, Float)
    RangeSlider-xKBSf-U(ranges.ClosedFloatingPointRange, Function1, Modifier, Boolean, ranges.ClosedFloatingPointRange, Int, () -> Unit, Float, basic.SliderColors, basic.SliderDefaults.SliderHapticEffect, Boolean, List, Float)
```
```
top.yukonga.miuix.kmp.basic.SmallTitleDefaults
    getInsideMargin()
```
```
top.yukonga.miuix.kmp.basic.SmallTitleKt
    SmallTitle(String, Modifier, Long, foundation.layout.PaddingValues)
```
```
top.yukonga.miuix.kmp.basic.SnackbarColors
    getContainerColor()
    getContentColor()
    getActionContentColor()
    getDismissActionContentColor()
    getActionContainerColor()
    component1()
    component2()
    component3()
    component4()
    component5()
    copy(Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SnackbarData
    getVisuals()
    dismiss(coroutines.Continuation)
    performAction(coroutines.Continuation)
```
```
top.yukonga.miuix.kmp.basic.SnackbarDefaults
    getCornerRadius()
    getInsideMargin()
    getOuterPadding()
    getActionCornerRadius()
    getActionInsideMargin()
    snackbarColors(Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SnackbarHostState
    <init>()
    getCurrentSnackbars$top_yukonga_miuix_kmp_miuix_ui()
    newestSnackbarData(coroutines.Continuation)
    oldestSnackbarData(coroutines.Continuation)
    removeEntry$top_yukonga_miuix_kmp_miuix_ui(basic.SnackbarHostState.SnackbarEntry, coroutines.Continuation)
    showSnackbar(String, String, Boolean, basic.SnackbarDuration, coroutines.Continuation)
```
```
top.yukonga.miuix.kmp.basic.SnackbarKt
    toMillis(basic.SnackbarDuration, Boolean, ui.platform.AccessibilityManager)
    SnackbarHost(basic.SnackbarHostState, Modifier, Boolean, Function3)
    Snackbar(basic.SnackbarData, Modifier, Float, basic.SnackbarColors, foundation.layout.PaddingValues)
```
```
top.yukonga.miuix.kmp.basic.SnackbarResult
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.basic.SnackbarVisuals
    <init>(String, String, Boolean, basic.SnackbarDuration)
    getMessage()
    getActionLabel()
    getWithDismissAction()
    getDuration()
    copy(String, String, Boolean, basic.SnackbarDuration)
```
```
top.yukonga.miuix.kmp.basic.SpinnerDefaults
    spinnerColors(Long, Long, Long, Long, Long, Long, Long)
    dialogSpinnerColors(Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SurfaceDefaults
    getShape()
    getShadowElevation()
```
```
top.yukonga.miuix.kmp.basic.SurfaceKt
    Surface-F(Modifier, ui.graphics.Shape, Long, Long, foundation.BorderStroke, Float, Function2)
    Surface(() -> Unit, Modifier, Boolean, ui.graphics.Shape, Long, Long, foundation.BorderStroke, Float, foundation.interaction.MutableInteractionSource, foundation.Indication, Function2)
```
```
top.yukonga.miuix.kmp.basic.SwitchColors
    checkedThumbColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    uncheckedThumbColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    checkedTrackColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    uncheckedTrackColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    copy(Long, Long, Long, Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SwitchDefaults
    switchColors(Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.SwitchKt
    Switch(Boolean, Function1, Modifier, basic.SwitchColors, Boolean)
```
```
top.yukonga.miuix.kmp.basic.TabRowColors
    backgroundColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    contentColor-vNxB06k$top_yukonga_miuix_kmp_miuix_ui(Boolean)
    copy(Long, Long, Long, Long)
    <init>(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.TabRowDefaults
    getTabRowHeight()
    getTabRowWithContourHeight()
    getTabRowCornerRadius()
    getTabRowWithContourCornerRadius()
    getTabRowMinWidth()
    getTabRowWithContourMinWidth()
    getTabRowMaxWidth()
    getTabRowWithContourMaxWidth()
    tabRowColors(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.TabRowKt
    TabRow(List, Int, Function1, Modifier, basic.TabRowColors, Float, Float, Float, Float, Float, ui.Alignment, foundation.lazy.LazyListState, foundation.interaction.MutableInteractionSource, foundation.Indication)
    TabRowWithContour(List, Int, Function1, Modifier, basic.TabRowColors, Float, Float, Float, Float, Float, ui.Alignment, foundation.lazy.LazyListState, foundation.interaction.MutableInteractionSource, foundation.Indication)
```
```
top.yukonga.miuix.kmp.basic.TextButtonColors
    getColor()
    getDisabledColor()
    getTextColor()
    getDisabledTextColor()
    component1()
    component2()
    component3()
    component4()
    copy(Long, Long, Long, Long)
    <init>(Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.TextFieldColors
    getBackgroundColor()
    getLabelColor()
    getBorderColor()
    component1()
    component2()
    component3()
    copy(Long, Long, Long)
    <init>(Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.TextFieldDefaults
    getCornerRadius()
    getInsideMargin()
    getBorderWidth-D9Ej5fM$top_yukonga_miuix_kmp_miuix_ui()
    getLabelFontSizeFloating-D9Ej5fM$top_yukonga_miuix_kmp_miuix_ui()
    getLabelFontSizeNormal-D9Ej5fM$top_yukonga_miuix_kmp_miuix_ui()
    textFieldColors(Long, Long, Long)
```
```
top.yukonga.miuix.kmp.basic.TextFieldKt
    TextField(foundation.text.input.TextFieldState, Modifier, Long, basic.TextFieldColors, Float, String, Boolean, Boolean, Boolean, foundation.text.input.InputTransformation, TextStyle, foundation.text.KeyboardOptions, foundation.text.input.KeyboardActionHandler, foundation.text.input.TextFieldLineLimits, Function2, Function2, Function2, foundation.interaction.MutableInteractionSource, ui.graphics.Brush, foundation.text.input.OutputTransformation, foundation.ScrollState)
    TextField(ui.text.input.TextFieldValue, Function1, Modifier, Long, basic.TextFieldColors, Float, String, Boolean, Boolean, Boolean, TextStyle, foundation.text.KeyboardOptions, foundation.text.KeyboardActions, Function2, Function2, Boolean, Int, Int, ui.text.input.VisualTransformation, Function1, foundation.interaction.MutableInteractionSource, ui.graphics.Brush)
    TextField(String, Function1, Modifier, Long, basic.TextFieldColors, Float, String, Boolean, Boolean, Boolean, TextStyle, foundation.text.KeyboardOptions, foundation.text.KeyboardActions, Function2, Function2, Boolean, Int, Int, ui.text.input.VisualTransformation, Function1, foundation.interaction.MutableInteractionSource, ui.graphics.Brush)
```
```
top.yukonga.miuix.kmp.basic.TextKt
    Text(String, Modifier, Long, foundation.text.TextAutoSize, Long, ui.text.font.FontStyle, ui.text.font.FontWeight, ui.text.font.FontFamily, Long, ui.text.style.TextDecoration, ui.text.style.TextAlign, Long, Int, Boolean, Int, Int, Function1, TextStyle)
    Text(String, ui.graphics.ColorProducer, Modifier, foundation.text.TextAutoSize, Long, ui.text.font.FontStyle, ui.text.font.FontWeight, ui.text.font.FontFamily, Long, ui.text.style.TextDecoration, ui.text.style.TextAlign, Long, Int, Boolean, Int, Int, Function1, TextStyle)
    Text(ui.text.AnnotatedString, Modifier, Long, foundation.text.TextAutoSize, Long, ui.text.font.FontStyle, ui.text.font.FontWeight, ui.text.font.FontFamily, Long, ui.text.style.TextDecoration, ui.text.style.TextAlign, Long, Int, Boolean, Int, Int, Map, Function1, TextStyle)
    Text(ui.text.AnnotatedString, ui.graphics.ColorProducer, Modifier, foundation.text.TextAutoSize, Long, ui.text.font.FontStyle, ui.text.font.FontWeight, ui.text.font.FontFamily, Long, ui.text.style.TextDecoration, ui.text.style.TextAlign, Long, Int, Boolean, Int, Int, Map, Function1, TextStyle)
```
```
top.yukonga.miuix.kmp.basic.ToolbarPosition
    toString()
    hashCode()
    equals(Int, Any)
    constructor()
    box()
    unbox()
    equals()
```
```
top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
    toString()
    hashCode()
    equals(Int, Any)
    box()
    unbox()
    equals()
```
```
top.yukonga.miuix.kmp.basic.TooltipDefaults
    getSpacingBetweenTooltipAndAnchor()
    getCaretSize()
    getPlainTooltipMaxWidth()
    getPlainTooltipCornerRadius()
    getPlainTooltipInsideMargin()
    getPlainTooltipContainerColor()
    getPlainTooltipContentColor()
    getRichTooltipMaxWidth()
    getRichTooltipCornerRadius()
    getRichTooltipInsideMargin()
    getRichTooltipActionCornerRadius()
    getRichTooltipActionInsideMargin()
    caretShape()
    richTooltipColors(Long, Long, Long, Long)
    rememberTooltipPositionProvider(Int, Float)
```
```
top.yukonga.miuix.kmp.basic.TooltipKt
    rememberTooltipState(Boolean, Boolean, foundation.MutatorMutex)
    TooltipBox(ui.window.PopupPositionProvider, Function3, basic.TooltipState, Modifier, Boolean, Boolean, Function2)
    PlainTooltip(basic.TooltipScope, Modifier, ui.graphics.Shape, Float, Float, Long, Long, foundation.layout.PaddingValues, Function2)
    RichTooltip(basic.TooltipScope, Modifier, Function2, Function2, ui.graphics.Shape, Float, Float, basic.RichTooltipColors, foundation.layout.PaddingValues, Function2)
    TooltipBox(String, Modifier, basic.TooltipState, Boolean, Int, Long, Long, Function2)
    RichTooltipBox(String, Modifier, basic.TooltipState, String, String, () -> Unit, Boolean, Int, basic.RichTooltipColors, Function2)
```
```
top.yukonga.miuix.kmp.basic.TooltipScope
    getPositioning()
    obtainAnchorBounds()
```
```
top.yukonga.miuix.kmp.basic.TooltipState
    getTransition()
    isVisible()
    isPersistent()
    show(foundation.MutatePriority, coroutines.Continuation)
    dismiss()
    onDispose()
```
```
top.yukonga.miuix.kmp.basic.TopAppBarDefaults
    getTitlePadding()
    getNavigationIconPadding()
    getActionIconPadding()
    getCollapsedHeight()
    getSmallTopAppBarCenterHeight()
    getLargeTitleBottomPadding()
    getSubtitleBottomPadding()
```
```
top.yukonga.miuix.kmp.basic.TopAppBarKt
    TopAppBar(String, Modifier, Long, Long, String, Long, String, Long, Function2, Function3, basic.ScrollBehavior, Boolean, Float, Float, Float, Function2)
    SmallTopAppBar(String, Modifier, Long, Long, String, Long, Function2, Function3, basic.ScrollBehavior, Boolean, Float, Float, Float, Function2)
    MiuixScrollBehavior(basic.TopAppBarState, () -> Unit, animation.core.AnimationSpec, animation.core.DecayAnimationSpec)
    rememberTopAppBarState(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.basic.TopAppBarState
    <init>(Float, Float, Float)
    getHeightOffsetLimit()
    setHeightOffsetLimit(Float)
    getHeightOffset()
    setHeightOffset(Float)
    getContentOffset()
    setContentOffset(Float)
    getCollapsedFraction()
    getOverlappedFraction()
```
```
top.yukonga.miuix.kmp.color.api.ExtensionsKt
    toOkLab(Long)
    toHsv(Long)
    toOkLch(Long)
```
```
top.yukonga.miuix.kmp.color.core.Transforms
    rgbToOkLab(Array<Float>)
    okLabToRgb(Array<Float>)
    okLabToOklch(Array<Float>)
    oklchToOkLab(Array<Float>)
    okLabToColor(Array<Float>, Float)
    oklchToColor(Float, Float, Float, Float)
    colorToOkLab(Long)
    colorToOklch(Long)
    rgbToHsv(Int, Int, Int, Array<Float>)
    colorToHsv(Long)
    srgbToOkhsv(Float, Float, Float)
    okhsvToSrgb(Float, Float, Float)
    colorToOkhsv(Long)
    okhsvToColor(Float, Float, Float, Float)
    generateHsvHueColors()
    generateOkHsvHueColors()
    generateOkLchHueColors(Float, Float)
    normalizeOklch(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.color.space.Hsv
    <init>(Float, Float, Float)
    getH()
    getS()
    getV()
    toColor(Float)
    copy(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.color.space.OkHsv
    <init>(Float, Float, Float)
    getH()
    getS()
    getV()
    toColor(Float)
    copy(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.color.space.OkLab
    <init>(Float, Float, Float)
    getL()
    getA()
    getB()
    toColor(Float)
    copy(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.color.space.OkLch
    <init>(Float, Float, Float)
    getL()
    getC()
    getH()
    toColor(Float)
    copy(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.icon.basic.ArrowRightKt
    getArrowRight(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.icon.basic.ArrowUpDownKt
    getArrowUpDown(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.icon.basic.CheckKt
    getCheck(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.icon.basic.CloseKt
    getClose(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.icon.basic.SearchCleanupKt
    getSearchCleanup(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.icon.basic.SearchKt
    getSearch(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.icon.basic.SidebarKt
    getSidebar(icon.MiuixIcons.Basic)
```
```
top.yukonga.miuix.kmp.interfaces.HoldDownInteractionKt
    collectIsHeldDownAsState(foundation.interaction.InteractionSource)
    HoldDownObserver(Boolean, foundation.interaction.MutableInteractionSource)
```
```
top.yukonga.miuix.kmp.layout.BottomSheetContentLayoutKt
    BottomSheetContentLayout(Boolean, Long, Float, Float, Long, Long, Long, Function4, Modifier, String, Function2, Function2, Boolean, () -> Unit, () -> Unit, Boolean, Boolean, Boolean, Dp, Function2)
    BottomSheetContent(String, Long, Float, Float, Long, Long, Boolean, Long, Boolean, runtime.MutableIntState, animation.core.Animatable, runtime.MutableFloatState, kotlinx.coroutines.channels.Channel, () -> Unit, Modifier, Dp, Boolean, Function2, Function2, Function2)
```
```
top.yukonga.miuix.kmp.layout.BottomSheetDefaults
    backgroundColor()
    dragHandleColor()
    getCornerRadius()
    getMaxWidth()
    getOutsideMargin()
    getInsideMargin()
```
```
top.yukonga.miuix.kmp.layout.CascadingListPopupLayoutKt
    getCascadingPopupCornerRadius()
    CascadingListPopupLayout(Boolean, Function4, List, () -> Unit, basic.DropdownColors, Modifier, () -> Unit, basic.PopupPositionProvider, basic.PopupPositionProvider.Align, Boolean, Dp, Float, Boolean)
    computeSecondaryRect(ui.unit.IntRect, Long, ui.unit.IntRect, ui.unit.LayoutDirection)
```
```
top.yukonga.miuix.kmp.layout.CascadingMorphContentKt
    CascadingPrimaryContent(List, basic.DropdownItem, Function1, Function1, Function2, () -> Unit, () -> Unit, basic.PopupLayoutPosition, Long, Long, basic.DropdownColors, () -> Unit, () -> Unit, Long, () -> Unit)
    CascadingSecondaryContent(basic.DropdownItem, Long, ui.unit.IntRect, ui.unit.IntRect, Int, Boolean, Int, () -> Unit, () -> Unit, basic.PopupLayoutPosition, Long, () -> Unit, () -> Unit, () -> Unit, Function1, basic.DropdownColors, Long)
    CascadingSecondaryColumn(basic.DropdownItem, basic.DropdownColors, () -> Unit, () -> Unit, Int, Int, () -> Unit, Function1)
```
```
top.yukonga.miuix.kmp.layout.DialogContentLayoutKt
    DialogContentLayout(Boolean, Long, Long, Long, Long, Long, Function4, Modifier, String, String, Boolean, () -> Unit, () -> Unit, Boolean, Dp, Function2)
    DialogContent(String, Long, String, Long, Long, Long, Long, Boolean, animation.core.Animatable, runtime.MutableIntState, () -> Unit, Modifier, Dp, Function2)
```
```
top.yukonga.miuix.kmp.layout.DialogDefaults
    isLargeScreen$top_yukonga_miuix_kmp_miuix_ui()
    titleColor()
    summaryColor()
    backgroundColor()
    getMaxWidth()
    getOutsideMargin()
    getInsideMargin()
```
```
top.yukonga.miuix.kmp.layout.ListPopupLayoutKt
    ListPopupLayout(Boolean, Function4, Modifier, basic.PopupPositionProvider, basic.PopupPositionProvider.Align, Boolean, () -> Unit, () -> Unit, Dp, Float, Function2)
```
```
top.yukonga.miuix.kmp.layout.MorphHeaderRowKt
    MorphHeaderRow(basic.DropdownItem, () -> Unit, () -> Unit, Int, Int, basic.DropdownColors, () -> Unit)
```
```
top.yukonga.miuix.kmp.overlay.OverlayBottomSheetKt
    OverlayBottomSheet(Boolean, Modifier, String, Function2, Function2, Long, Boolean, Float, Float, () -> Unit, () -> Unit, Long, Long, Boolean, Long, Boolean, Boolean, Boolean, Function2)
```
```
top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopupKt
    OverlayCascadingListPopup(Boolean, List, () -> Unit, Modifier, () -> Unit, basic.PopupPositionProvider, basic.PopupPositionProvider.Align, Boolean, Dp, Float, Boolean, basic.DropdownColors, Boolean)
```
```
top.yukonga.miuix.kmp.overlay.OverlayDialogKt
    OverlayDialog(Boolean, Modifier, String, Long, String, Long, Long, Boolean, () -> Unit, () -> Unit, Long, Long, Boolean, Boolean, Function2)
```
```
top.yukonga.miuix.kmp.overlay.OverlayListPopupKt
    OverlayListPopup(Boolean, Modifier, basic.PopupPositionProvider, basic.PopupPositionProvider.Align, Boolean, () -> Unit, () -> Unit, Dp, Float, Boolean, Function2)
```
```
top.yukonga.miuix.kmp.theme.ColorSchemeMode
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.theme.Colors
    getPrimary()
    setPrimary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnPrimary()
    setOnPrimary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getPrimaryVariant()
    setPrimaryVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnPrimaryVariant()
    setOnPrimaryVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getError()
    setError-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnError()
    setOnError-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getErrorContainer()
    setErrorContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnErrorContainer()
    setOnErrorContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledPrimary()
    setDisabledPrimary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledOnPrimary()
    setDisabledOnPrimary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledPrimaryButton()
    setDisabledPrimaryButton-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledOnPrimaryButton()
    setDisabledOnPrimaryButton-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledPrimarySlider()
    setDisabledPrimarySlider-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getPrimaryContainer()
    setPrimaryContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnPrimaryContainer()
    setOnPrimaryContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSecondary()
    setSecondary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSecondary()
    setOnSecondary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSecondaryVariant()
    setSecondaryVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSecondaryVariant()
    setOnSecondaryVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledSecondary()
    setDisabledSecondary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledOnSecondary()
    setDisabledOnSecondary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledSecondaryVariant()
    setDisabledSecondaryVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledOnSecondaryVariant()
    setDisabledOnSecondaryVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSecondaryContainer()
    setSecondaryContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSecondaryContainer()
    setOnSecondaryContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSecondaryContainerVariant()
    setSecondaryContainerVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSecondaryContainerVariant()
    setOnSecondaryContainerVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getTertiaryContainer()
    setTertiaryContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnTertiaryContainer()
    setOnTertiaryContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getTertiaryContainerVariant()
    setTertiaryContainerVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getBackground()
    setBackground-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnBackground()
    setOnBackground-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnBackgroundVariant()
    setOnBackgroundVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSurface()
    setSurface-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurface()
    setOnSurface-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSurfaceVariant()
    setSurfaceVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceSecondary()
    setOnSurfaceSecondary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceVariantSummary()
    setOnSurfaceVariantSummary-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceVariantActions()
    setOnSurfaceVariantActions-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDisabledOnSurface()
    setDisabledOnSurface-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSurfaceContainer()
    setSurfaceContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceContainer()
    setOnSurfaceContainer-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceContainerVariant()
    setOnSurfaceContainerVariant-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSurfaceContainerHigh()
    setSurfaceContainerHigh-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceContainerHigh()
    setOnSurfaceContainerHigh-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSurfaceContainerHighest()
    setSurfaceContainerHighest-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOnSurfaceContainerHighest()
    setOnSurfaceContainerHighest-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getOutline()
    setOutline-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getDividerLine()
    setDividerLine-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getWindowDimming()
    setWindowDimming-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSliderKeyPoint()
    setSliderKeyPoint-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSliderKeyPointForeground()
    setSliderKeyPointForeground-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    getSliderBackground()
    setSliderBackground-8_81llA$top_yukonga_miuix_kmp_miuix_ui(Long)
    copy(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.theme.ColorsKt
    lightColorScheme(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long)
    darkColorScheme(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long)
    updateColorsFrom(theme.Colors, theme.Colors)
    getLocalColors()
```
```
top.yukonga.miuix.kmp.theme.ContentColorKt
    getLocalContentColor()
```
```
top.yukonga.miuix.kmp.theme.DismissStateKt
    getLocalDismissState()
```
```
top.yukonga.miuix.kmp.theme.DynamicColors_androidKt
    platformDynamicColors(Boolean)
```
```
top.yukonga.miuix.kmp.theme.MiuixTheme
    getColorScheme()
    getTextStyles()
    getColorSchemeMode()
    isDynamicColor()
```
```
top.yukonga.miuix.kmp.theme.MiuixThemeKt
    MiuixTheme(theme.ThemeController, theme.TextStyles, Function2)
    MiuixTheme(theme.Colors, theme.TextStyles, Function2)
```
```
top.yukonga.miuix.kmp.theme.MonetMappingKt
    mapMd3RolesToMiuixColorsCommon(theme.MonetRoles, Boolean)
```
```
top.yukonga.miuix.kmp.theme.MonetRoles
    getPrimary()
    getOnPrimary()
    getPrimaryFixed()
    getOnPrimaryFixed()
    getError()
    getOnError()
    getErrorContainer()
    getOnErrorContainer()
    getPrimaryContainer()
    getOnPrimaryContainer()
    getSecondary()
    getOnSecondary()
    getSecondaryContainer()
    getOnSecondaryContainer()
    getTertiaryContainer()
    getOnTertiaryContainer()
    getBackground()
    getOnBackground()
    getSurface()
    getOnSurface()
    getSurfaceVariant()
    getSurfaceContainer()
    getSurfaceContainerHigh()
    getSurfaceContainerHighest()
    getOutline()
    getOutlineVariant()
    getOnSurfaceVariant()
    component1()
    component2()
    component3()
    component4()
    component5()
    component6()
    component7()
    component8()
    component9()
    component10()
    component11()
    component12()
    component13()
    component14()
    component15()
    component16()
    component17()
    component18()
    component19()
    component20()
    component21()
    component22()
    component23()
    component24()
    component25()
    component26()
    component27()
    copy(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long)
    <init>(Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long, Long)
```
```
top.yukonga.miuix.kmp.theme.TextStyles
    <init>(TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle)
    getMain()
    setMain$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getParagraph()
    setParagraph$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getBody1()
    setBody1$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getBody2()
    setBody2$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getButton()
    setButton$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getFootnote1()
    setFootnote1$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getFootnote2()
    setFootnote2$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getHeadline1()
    setHeadline1$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getHeadline2()
    setHeadline2$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getSubtitle()
    setSubtitle$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getTitle1()
    setTitle1$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getTitle2()
    setTitle2$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getTitle3()
    setTitle3$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    getTitle4()
    setTitle4$top_yukonga_miuix_kmp_miuix_ui(TextStyle)
    copy(TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle)
```
```
top.yukonga.miuix.kmp.theme.TextStylesKt
    defaultTextStyles(TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle, TextStyle)
    updateTextStylesFrom(theme.TextStyles, theme.TextStyles)
    getLocalTextStyles()
```
```
top.yukonga.miuix.kmp.theme.ThemeColorSpec
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.theme.ThemeController
    <init>(theme.ColorSchemeMode, theme.Colors, theme.Colors, Color, theme.ThemeColorSpec, theme.ThemePaletteStyle, Boolean)
    getColorSchemeMode()
    getLightColors()
    getDarkColors()
    getKeyColor()
    getColorSpec()
    getPaletteStyle()
    isDark()
    currentColors()
    <init>(theme.ColorSchemeMode, theme.Colors, theme.Colors, Color, theme.ThemeColorSpec, theme.ThemePaletteStyle, Boolean)
```
```
top.yukonga.miuix.kmp.theme.ThemeControllerKt
    colorsFromSeed(Long, theme.ThemeColorSpec, theme.ThemePaletteStyle, Boolean)
    monetSystemColors(Boolean)
    getLocalColorSchemeMode()
```
```
top.yukonga.miuix.kmp.theme.ThemePaletteStyle
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.utils.AbstractPressableNode
    getShouldAutoInvalidate()
    applyAdditionalSemantics(ui.semantics.SemanticsPropertyReceiver)
    onAttach()
    onDetach()
    onPointerEvent(ui.input.pointer.PointerEvent, ui.input.pointer.PointerEventPass, Long)
    onCancelPointerInput()
    getShouldMergeDescendantSemantics()
    applySemantics(ui.semantics.SemanticsPropertyReceiver)
    getTraverseKey()
    onDensityChange()
    interceptOutOfBoundsChildEvents()
    sharePointerInputWithSiblings()
    onViewConfigurationChange()
    getTouchBoundsExpansion()
    getShouldClearDescendantSemantics()
    isImportantForBounds()
    <init>(foundation.interaction.MutableInteractionSource, foundation.IndicationNodeFactory, Boolean, ui.semantics.Role, Long)
```
```
top.yukonga.miuix.kmp.utils.MiuixIndication
    <init>(Long)
    create(foundation.interaction.InteractionSource)
    rememberUpdatedInstance(foundation.interaction.InteractionSource)
    <init>(Long)
```
```
top.yukonga.miuix.kmp.utils.MiuixOverscrollEffect
    <init>()
    getOffsetX$top_yukonga_miuix_kmp_miuix_ui()
    getOffsetY$top_yukonga_miuix_kmp_miuix_ui()
    getInvalidateNodePlacement$top_yukonga_miuix_kmp_miuix_ui()
    setInvalidateNodePlacement$top_yukonga_miuix_kmp_miuix_ui(() -> Unit)
    getScrollRangeV$top_yukonga_miuix_kmp_miuix_ui()
    setScrollRangeV$top_yukonga_miuix_kmp_miuix_ui(Float)
    getScrollRangeH$top_yukonga_miuix_kmp_miuix_ui()
    setScrollRangeH$top_yukonga_miuix_kmp_miuix_ui(Float)
    getLaunchAnimation$top_yukonga_miuix_kmp_miuix_ui()
    setLaunchAnimation$top_yukonga_miuix_kmp_miuix_ui(Function1)
    getGetPullToRefreshState$top_yukonga_miuix_kmp_miuix_ui()
    setGetPullToRefreshState$top_yukonga_miuix_kmp_miuix_ui(() -> Unit)
    getGetOverScrollState$top_yukonga_miuix_kmp_miuix_ui()
    setGetOverScrollState$top_yukonga_miuix_kmp_miuix_ui(() -> Unit)
    isInProgress()
    getNode()
    resetAll$top_yukonga_miuix_kmp_miuix_ui()
    applyToScroll(Long, Int, Function1)
    applyToFling(Long, Function2, coroutines.Continuation)
    getEffectModifier()
```
```
top.yukonga.miuix.kmp.utils.MiuixOverscrollFactory
    createOverscrollEffect()
```
```
top.yukonga.miuix.kmp.utils.MiuixPopupUtils
    <init>()
```
```
top.yukonga.miuix.kmp.utils.MiuixPopupUtilsKt
    isLargeScreen()
    getLocalPopupStates()
    getLocalDialogStates()
    getLocalRootDialogStates()
    getLocalRootPopupStates()
```
```
top.yukonga.miuix.kmp.utils.OverScrollState
    <init>()
    isOverScrollActive()
    setOverScrollActive$top_yukonga_miuix_kmp_miuix_ui(Boolean)
```
```
top.yukonga.miuix.kmp.utils.OverscrollKt
    overScrollVertical(Modifier, Boolean, () -> Unit)
    overScrollHorizontal(Modifier, Boolean, () -> Unit)
    overScrollOutOfBound(Modifier, Boolean, Boolean, () -> Unit)
    getLocalOverScrollState()
```
```
top.yukonga.miuix.kmp.utils.PressFeedbackType
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.utils.PressGestureScopeImpl
    <init>(ui.unit.Density)
    cancel()
    release()
    reset(coroutines.Continuation)
    awaitRelease(coroutines.Continuation)
    tryAwaitRelease(coroutines.Continuation)
    toPx(Float)
    toPx-(Long)
    roundToPx(Float)
    roundToPx-(Long)
    toDp()
    toDp(Float)
    toDp(Long)
    toSp()
    toSp(Float)
    toSp(Float)
    toRect(ui.unit.DpRect)
    toSize(Long)
    toDpSize-k(Long)
    getDensity()
    getFontScale()
```
```
top.yukonga.miuix.kmp.utils.PressableKt
    pressable(Modifier, foundation.interaction.MutableInteractionSource, foundation.Indication, Boolean, ui.semantics.Role, Long)
    pressableWithIndicationIfNeeded(Modifier, foundation.interaction.MutableInteractionSource, foundation.Indication, Function2)
```
```
top.yukonga.miuix.kmp.utils.PressableNode
    update(foundation.interaction.MutableInteractionSource, foundation.IndicationNodeFactory, Boolean, ui.semantics.Role, Long)
    <init>(foundation.interaction.MutableInteractionSource, foundation.IndicationNodeFactory, Boolean, ui.semantics.Role, Long)
```
```
top.yukonga.miuix.kmp.utils.ScrollEndHapticKt
    scrollEndHaptic-zg(Modifier)
```
```
top.yukonga.miuix.kmp.utils.SinkFeedback
    <init>(Float, animation.core.AnimationSpec)
    <init>(Float, animation.core.AnimationSpec)
    getSinkAmount()
    getAnimationSpec()
    create(foundation.interaction.InteractionSource)
    copy(Float, animation.core.AnimationSpec)
    rememberUpdatedInstance(foundation.interaction.InteractionSource)
    <init>()
```
```
top.yukonga.miuix.kmp.utils.SpringEngine
    <init>()
    getVelocity()
    setVelocity(Double)
    getCurrentPos()
    setCurrentPos(Double)
    start(Float, Float, Float)
    step(Float)
```
```
top.yukonga.miuix.kmp.utils.SpringMath
    obtainDampingDistance(Float, Float)
    obtainTouchDistance(Float, Float)
```
```
top.yukonga.miuix.kmp.utils.SpringUtilsKt
    runSettleAnimation(utils.SpringEngine, Float, Float, Float, Function1, () -> Unit, coroutines.Continuation)
```
```
top.yukonga.miuix.kmp.utils.TiltFeedback
    <init>(Float, animation.core.AnimationSpec)
    <init>(Float, animation.core.AnimationSpec)
    getTiltAmount()
    getAnimationSpec()
    create(foundation.interaction.InteractionSource)
    copy(Float, animation.core.AnimationSpec)
    rememberUpdatedInstance(foundation.interaction.InteractionSource)
    <init>()
```
```
top.yukonga.miuix.kmp.window.WindowBottomSheetKt
    WindowBottomSheet(Boolean, Modifier, String, Function2, Function2, Long, Boolean, Float, Float, () -> Unit, () -> Unit, Long, Long, Boolean, Long, Boolean, Boolean, Function2)
```
```
top.yukonga.miuix.kmp.window.WindowCascadingListPopupKt
    WindowCascadingListPopup(Boolean, List, () -> Unit, Modifier, () -> Unit, basic.PopupPositionProvider, basic.PopupPositionProvider.Align, Boolean, Dp, Float, basic.DropdownColors, Boolean)
```
```
top.yukonga.miuix.kmp.window.WindowDialogKt
    WindowDialog(Boolean, Modifier, String, Long, String, Long, Long, Boolean, () -> Unit, () -> Unit, Long, Long, Boolean, Function2)
```
```
top.yukonga.miuix.kmp.window.WindowListPopupKt
    WindowListPopup(Boolean, Modifier, basic.PopupPositionProvider, basic.PopupPositionProvider.Align, Boolean, () -> Unit, () -> Unit, Dp, Float, Function2)
```

### miuix-preference

```
top.yukonga.miuix.kmp.menu.OverlayDropdownMenuKt
    OverlayDropdownMenu(basic.DropdownEntry, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1)
    OverlayDropdownMenu(List, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1)
```
```
top.yukonga.miuix.kmp.menu.OverlayIconCascadingDropdownMenuKt
    OverlayIconCascadingDropdownMenu(basic.DropdownEntry, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Boolean, Function1, Long, Float, Float, Float, Function2)
    OverlayIconCascadingDropdownMenu(List, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Boolean, Function1, Long, Float, Float, Float, Function2)
```
```
top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenuKt
    OverlayIconDropdownMenu(basic.DropdownEntry, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Boolean, Function1, Long, Float, Float, Float, Function2)
    OverlayIconDropdownMenu(List, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Boolean, Function1, Long, Float, Float, Float, Function2)
```
```
top.yukonga.miuix.kmp.menu.WindowDropdownMenuKt
    WindowDropdownMenu-b(basic.DropdownEntry, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Function1)
    WindowDropdownMenu-b(List, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Function1)
```
```
top.yukonga.miuix.kmp.menu.WindowIconCascadingDropdownMenuKt
    WindowIconCascadingDropdownMenu(basic.DropdownEntry, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Function1, Long, Float, Float, Float, Function2)
    WindowIconCascadingDropdownMenu(List, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Function1, Long, Float, Float, Float, Function2)
```
```
top.yukonga.miuix.kmp.menu.WindowIconDropdownMenuKt
    WindowIconDropdownMenu(basic.DropdownEntry, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Function1, Long, Float, Float, Float, Function2)
    WindowIconDropdownMenu(List, Modifier, Boolean, Dp, basic.DropdownColors, Boolean, Function1, Long, Float, Float, Float, Function2)
```
```
top.yukonga.miuix.kmp.popup.DropdownEntriesContentKt
    DropdownEntriesPopupContent(List, basic.DropdownColors, Function2)
    dropdownEntriesDialogItems(foundation.lazy.LazyListScope, List, basic.DropdownColors, Function2)
```
```
top.yukonga.miuix.kmp.popup.OverlayDropdownPopupKt
    OverlayDropdownPopup(basic.DropdownEntry, Boolean, () -> Unit, () -> Unit, Dp, basic.DropdownColors, Boolean, Boolean)
    OverlayDropdownPopup(List, Boolean, () -> Unit, () -> Unit, Dp, basic.DropdownColors, Boolean, Boolean)
    OverlayDropdownDialog(basic.DropdownEntry, String, String, Boolean, () -> Unit, () -> Unit, basic.DropdownColors, Modifier, Boolean, Boolean)
    OverlayDropdownDialog(List, String, String, Boolean, () -> Unit, () -> Unit, basic.DropdownColors, Modifier, Boolean, Boolean)
```
```
top.yukonga.miuix.kmp.popup.WindowDropdownPopupKt
    WindowDropdownPopup(basic.DropdownEntry, Boolean, () -> Unit, () -> Unit, Dp, basic.DropdownColors, Boolean)
    WindowDropdownPopup(List, Boolean, () -> Unit, () -> Unit, Dp, basic.DropdownColors, Boolean)
    WindowDropdownDialog(basic.DropdownEntry, String, String, Boolean, () -> Unit, () -> Unit, basic.DropdownColors, Modifier, Boolean)
    WindowDropdownDialog(List, String, String, Boolean, () -> Unit, () -> Unit, basic.DropdownColors, Modifier, Boolean)
```
```
top.yukonga.miuix.kmp.preference.ArrowPreferenceDefaults
    endActionColors()
```
```
top.yukonga.miuix.kmp.preference.ArrowPreferenceKt
    ArrowPreference(String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, Function2, Function3, Function2, foundation.layout.PaddingValues, () -> Unit, Boolean, Boolean)
```
```
top.yukonga.miuix.kmp.preference.CheckboxLocation
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.preference.CheckboxPreferenceKt
    CheckboxPreference(String, Boolean, Function1, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.CheckboxColors, Function2, Function3, preference.CheckboxLocation, Function2, foundation.layout.PaddingValues, Boolean, Boolean)
```
```
top.yukonga.miuix.kmp.preference.EndActionColors
    color-vNxB06k$top_yukonga_miuix_kmp_miuix_preference(Boolean)
    copy-(Long, Long)
    <init>(Long, Long)
```
```
top.yukonga.miuix.kmp.preference.OverlayDropdownPreferenceKt
    OverlayDropdownPreference(List, Int, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1, Function1)
    OverlayDropdownPreference-84PZ-4c(basic.DropdownEntry, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Boolean, Function1)
    OverlayDropdownPreference-84PZ-4c(List, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Boolean, Function1)
```
```
top.yukonga.miuix.kmp.preference.OverlaySpinnerPreferenceKt
    OverlaySpinnerPreference(List, Int, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1, Function1)
    OverlaySpinnerPreference-84PZ-4c(basic.DropdownEntry, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Boolean, Function1)
    OverlaySpinnerPreference-84PZ-4c(List, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Boolean, Function1)
    OverlaySpinnerPreference(List, Int, String, String, Modifier, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Boolean, Boolean, Boolean, Boolean, Function1, Function1)
    OverlaySpinnerPreference(basic.DropdownEntry, String, String, Modifier, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Boolean, Boolean, Boolean, Boolean, Function1)
    OverlaySpinnerPreference(List, String, String, Modifier, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Boolean, Boolean, Boolean, Boolean, Function1)
```
```
top.yukonga.miuix.kmp.preference.RadioButtonLocation
    values()
    valueOf(String)
    getEntries()
```
```
top.yukonga.miuix.kmp.preference.RadioButtonPreferenceKt
    RadioButtonPreference(String, Boolean, () -> Unit, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.RadioButtonColors, Function2, Function3, preference.RadioButtonLocation, Function2, foundation.layout.PaddingValues, Boolean, Boolean)
```
```
top.yukonga.miuix.kmp.preference.SliderPreferenceKt
    SliderPreference(Float, Function1, Modifier, String, basic.BasicComponentColors, String, basic.BasicComponentColors, Function2, String, Function3, Function2, () -> Unit, Boolean, Boolean, ranges.ClosedFloatingPointRange, Int, () -> Unit, Boolean, Float, basic.SliderColors, basic.SliderDefaults.SliderHapticEffect, Boolean, List, Float, foundation.layout.PaddingValues)
    RangeSliderPreference(ranges.ClosedFloatingPointRange, Function1, Modifier, String, basic.BasicComponentColors, String, basic.BasicComponentColors, Function2, String, Function3, Function2, () -> Unit, Boolean, Boolean, ranges.ClosedFloatingPointRange, Int, () -> Unit, Float, basic.SliderColors, basic.SliderDefaults.SliderHapticEffect, Boolean, List, Float, foundation.layout.PaddingValues)
```
```
top.yukonga.miuix.kmp.preference.SwitchPreferenceKt
    SwitchPreference(Boolean, Function1, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, Function2, Function3, Function2, basic.SwitchColors, foundation.layout.PaddingValues, Boolean, Boolean)
```
```
top.yukonga.miuix.kmp.preference.WindowDropdownPreferenceKt
    WindowDropdownPreference(List, Int, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Function1, Function1)
    WindowDropdownPreference(basic.DropdownEntry, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1)
    WindowDropdownPreference(List, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1)
```
```
top.yukonga.miuix.kmp.preference.WindowSpinnerPreferenceKt
    WindowSpinnerPreference(List, Int, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Function1, Function1)
    WindowSpinnerPreference(basic.DropdownEntry, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1)
    WindowSpinnerPreference(List, String, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Dp, Boolean, Boolean, Boolean, Function1)
    WindowSpinnerPreference(List, Int, String, String, Modifier, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Boolean, Boolean, Boolean, Function1, Function1)
    WindowSpinnerPreference(basic.DropdownEntry, String, String, Modifier, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Boolean, Boolean, Boolean, Function1)
    WindowSpinnerPreference(List, String, String, Modifier, Modifier, basic.BasicComponentColors, String, basic.BasicComponentColors, basic.DropdownColors, Function2, Function2, foundation.layout.PaddingValues, Boolean, Boolean, Boolean, Function1)
```
```
top.yukonga.miuix.kmp.preference.internal.StartActionSlotKt
    StartActionSlot-djqs-MU(Modifier, Float, Float, Function2)
```

### miuix-blur

```
top.yukonga.miuix.kmp.blur.Backdrop
    isCoordinatesDependent()
    getOffsetResidualX()
    getOffsetResidualY()
    drawBackdrop(ui.graphics.drawscope.DrawScope, ui.unit.Density, ui.layout.LayoutCoordinates, Function1)
```
```
top.yukonga.miuix.kmp.blur.BackdropEffectScope
    getSize-NH()
    getLayoutDirection()
    getShape()
    getPadding()
    setPadding(Float)
    getRenderEffect()
    setRenderEffect(ui.graphics.RenderEffect)
    getDownscaleFactor()
    setDownscaleFactor()
    getNoiseCoefficient()
    setNoiseCoefficient(Float)
```
```
top.yukonga.miuix.kmp.blur.BackdropEffectScopeImpl
    <init>()
    getDensity()
    setDensity(Float)
    getFontScale()
    setFontScale(Float)
    getSize-NH()
    setSize(Long)
    getLayoutDirection()
    setLayoutDirection(ui.unit.LayoutDirection)
    getPadding()
    setPadding(Float)
    getRenderEffect()
    setRenderEffect(ui.graphics.RenderEffect)
    getDownscaleFactor()
    setDownscaleFactor()
    getNoiseCoefficient()
    setNoiseCoefficient(Float)
    getRuntimeShaderCache()
    setRuntimeShaderCache(blur.RuntimeShaderCache)
    getBlurRawWeights$top_yukonga_miuix_kmp_miuix_blur()
    getBlurParamOffsets$top_yukonga_miuix_kmp_miuix_blur()
    getBlurParamWeights$top_yukonga_miuix_kmp_miuix_blur()
    obtainShaderOffsetsBuffer$top_yukonga_miuix_kmp_miuix_blur()
    obtainShaderWeightsBuffer$top_yukonga_miuix_kmp_miuix_blur()
    getBlendModesBuffer$top_yukonga_miuix_kmp_miuix_blur()
    getBlendColorsBuffer$top_yukonga_miuix_kmp_miuix_blur()
    getCachedBlurRadiusX$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlurRadiusX$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedBlurRadiusY$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlurRadiusY$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedBlurSizeW$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlurSizeW$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedBlurSizeH$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlurSizeH$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedBlurExp$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlurExp$top_yukonga_miuix_kmp_miuix_blur()
    getCachedBlurResult$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlurResult$top_yukonga_miuix_kmp_miuix_blur(ui.graphics.RenderEffect)
    getCachedBlendColors$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlendColors$top_yukonga_miuix_kmp_miuix_blur(blur.BlurColors)
    getCachedBlendResult$top_yukonga_miuix_kmp_miuix_blur()
    setCachedBlendResult$top_yukonga_miuix_kmp_miuix_blur(ui.graphics.RenderEffect)
    getCachedColorBrightness$top_yukonga_miuix_kmp_miuix_blur()
    setCachedColorBrightness$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedColorContrast$top_yukonga_miuix_kmp_miuix_blur()
    setCachedColorContrast$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedColorSaturation$top_yukonga_miuix_kmp_miuix_blur()
    setCachedColorSaturation$top_yukonga_miuix_kmp_miuix_blur(Float)
    getCachedColorResult$top_yukonga_miuix_kmp_miuix_blur()
    setCachedColorResult$top_yukonga_miuix_kmp_miuix_blur(ui.graphics.RenderEffect)
    getForcedDownscaleExp$top_yukonga_miuix_kmp_miuix_blur()
    setForcedDownscaleExp$top_yukonga_miuix_kmp_miuix_blur()
    getBlurBlendExpLo$top_yukonga_miuix_kmp_miuix_blur()
    setBlurBlendExpLo$top_yukonga_miuix_kmp_miuix_blur()
    getBlurBlendExpHi$top_yukonga_miuix_kmp_miuix_blur()
    setBlurBlendExpHi$top_yukonga_miuix_kmp_miuix_blur()
    getBlurBlendFactor$top_yukonga_miuix_kmp_miuix_blur()
    setBlurBlendFactor$top_yukonga_miuix_kmp_miuix_blur(Float)
    obtainRuntimeShader(String, String)
    update(ui.graphics.drawscope.DrawScope)
    apply(Function1)
    reset()
    toPx(Float)
    toPx-(Long)
    roundToPx(Float)
    roundToPx-(Long)
    toDp()
    toDp(Float)
    toDp(Long)
    toSp()
    toSp(Float)
    toSp(Float)
    toRect(ui.unit.DpRect)
    toSize(Long)
    toDpSize-k(Long)
```
```
top.yukonga.miuix.kmp.blur.BackdropEffectScopeKt
    colorControls(blur.BackdropEffectScope, Float, Float, Float)
    effect(blur.BackdropEffectScope, ui.graphics.RenderEffect)
    runtimeShaderEffect(blur.BackdropEffectScope, String, String, String, Function1)
    getImpl(blur.BackdropEffectScope)
```
```
top.yukonga.miuix.kmp.blur.BackdropEffectsKt
    blur(blur.BackdropEffectScope, Float, Float)
    noiseDither(blur.BackdropEffectScope, Float)
    blendColors(blur.BackdropEffectScope, blur.BlurColors)
    textureBlurEffect(blur.BackdropEffectScope, Float, Float, Float, blur.BlurColors)
```
```
top.yukonga.miuix.kmp.blur.BlendColorEntry
    <init>(Long)
    getColor()
    getMode()
    component1()
    component2()
    copy-(Long)
    <init>(Long)
```
```
top.yukonga.miuix.kmp.blur.BlurBlendMode
    getValue()
    toString()
    hashCode()
    equals(Int, Any)
    constructor()
    box()
    unbox()
    equals()
```
```
top.yukonga.miuix.kmp.blur.BlurColors
    <init>(List, Float, Float, Float)
    <init>(List, Float, Float, Float)
    getBlendColors()
    getBrightness()
    getContrast()
    getSaturation()
    copy(List, Float, Float, Float)
    <init>()
```
```
top.yukonga.miuix.kmp.blur.BlurDefaults
    getBlurRadius()
    getNoiseCoefficient()
    getMaxBlurRadius()
    blurColors(List, Float, Float, Float)
```
```
top.yukonga.miuix.kmp.blur.DrawBackdropModifierKt
    drawBackdrop(Modifier, blur.Backdrop, () -> Unit, Function1, Function1, Function1, Function1, Function2, Function1, Function1, Int, Boolean)
```
```
top.yukonga.miuix.kmp.blur.LayerBackdrop
    <init>(ui.graphics.layer.GraphicsLayer, Function1)
    getGraphicsLayer()
    getOnDraw$top_yukonga_miuix_kmp_miuix_blur()
    isCoordinatesDependent()
    getLayerCoordinates$top_yukonga_miuix_kmp_miuix_blur()
    setLayerCoordinates$top_yukonga_miuix_kmp_miuix_blur(ui.layout.LayoutCoordinates)
    getOffsetResidualX()
    setOffsetResidualX$top_yukonga_miuix_kmp_miuix_blur(Float)
    getOffsetResidualY()
    setOffsetResidualY$top_yukonga_miuix_kmp_miuix_blur(Float)
    drawBackdrop(ui.graphics.drawscope.DrawScope, ui.unit.Density, ui.layout.LayoutCoordinates, Function1)
```
```
top.yukonga.miuix.kmp.blur.LayerBackdropKt
    rememberLayerBackdrop(ui.graphics.layer.GraphicsLayer, Function1)
```
```
top.yukonga.miuix.kmp.blur.LayerBackdropModifierKt
    layerBackdrop(Modifier, blur.LayerBackdrop)
```
```
top.yukonga.miuix.kmp.blur.RuntimeShaderCache
    obtainRuntimeShader(String, String)
```
```
top.yukonga.miuix.kmp.blur.RuntimeShaderCacheImpl
    <init>()
    obtainRuntimeShader(String, String)
```
```
top.yukonga.miuix.kmp.blur.RuntimeShaderCacheKt
    getLocalRuntimeShaderCache()
```
```
top.yukonga.miuix.kmp.blur.RuntimeShaderKt
    RuntimeShader(String)
    asComposeShader(shader.RuntimeShader)
    asBrush(shader.RuntimeShader)
    isRuntimeShaderSupported()
```
```
top.yukonga.miuix.kmp.blur.TextureEffectKt
    textureBlur(Modifier, blur.Backdrop, ui.graphics.Shape, Float, Float, blur.BlurColors, blur.highlight.Highlight, Int, Boolean)
    textureBlur(Modifier, blur.Backdrop, ui.graphics.Shape, Float, Float, Float, blur.BlurColors, blur.highlight.Highlight, Int, Boolean)
    textureEffect(Modifier, blur.Backdrop, ui.graphics.Shape, Float, Float, blur.BlurColors, blur.highlight.Highlight, Int, Boolean)
    textureEffect(Modifier, blur.Backdrop, ui.graphics.Shape, Float, Float, Float, blur.BlurColors, blur.highlight.Highlight, Int, Boolean)
```
```
top.yukonga.miuix.kmp.blur.highlight.BloomStroke
    <init>(Long, Int, Float, blur.highlight.LightSource, blur.highlight.LightSource, Boolean)
    getColor()
    getBlendMode()
    getInnerBlurRadius()
    getPrimaryLight()
    getSecondaryLight()
    getDualPeak()
    createShader(ui.graphics.drawscope.DrawScope, ui.graphics.Shape, Float, Float, blur.RuntimeShaderCache)
    component1()
    component2()
    component3()
    copy(Long, Int, Float, blur.highlight.LightSource, blur.highlight.LightSource, Boolean)
    <init>(Long, Int, Float, blur.highlight.LightSource, blur.highlight.LightSource, Boolean)
```
```
top.yukonga.miuix.kmp.blur.highlight.Highlight
    <init>(Float, Float, blur.highlight.HighlightStyle)
    getWidth()
    getAlpha()
    getStyle()
    component1()
    copy(Float, Float, blur.highlight.HighlightStyle)
    <init>(Float, Float, blur.highlight.HighlightStyle)
```
```
top.yukonga.miuix.kmp.blur.highlight.HighlightDrawingKt
    drawHighlight(ui.graphics.drawscope.ContentDrawScope, blur.highlight.Highlight, ui.graphics.Shape, blur.RuntimeShaderCache, ui.graphics.Paint)
```
```
top.yukonga.miuix.kmp.blur.highlight.HighlightStyle
    getColor()
    getBlendMode()
    createShader(ui.graphics.drawscope.DrawScope, ui.graphics.Shape, Float, Float, blur.RuntimeShaderCache)
```
```
top.yukonga.miuix.kmp.blur.highlight.HighlightStyleKt
    applyLightUniforms(shader.RuntimeShader, String, blur.highlight.LightSource, Boolean)
    setCornerRadiiUniform(ui.graphics.drawscope.DrawScope, shader.RuntimeShader, ui.graphics.Shape)
```
```
top.yukonga.miuix.kmp.blur.highlight.LightPosition
    <init>(Float, Float, Float)
    getX()
    getY()
    getZ()
    copy(Float, Float, Float)
```
```
top.yukonga.miuix.kmp.blur.highlight.LightSource
    <init>(blur.highlight.LightPosition, Long, Float)
    getPosition()
    getColor()
    getIntensity()
    component2()
    copy(blur.highlight.LightPosition, Long, Float)
    <init>(blur.highlight.LightPosition, Long, Float)
```
```
top.yukonga.miuix.kmp.blur.highlight.TiltLightKt
    rememberTiltLight(blur.highlight.LightPosition, Long, Float, Float)
```
```
top.yukonga.miuix.kmp.blur.internal.BlurEffectKt
    createBlurEffect(Float, Float, Int, Float, Float, Long, blur.BackdropEffectScopeImpl)
    downScaleExpFor(Float)
    adjustedVarianceForExp(Float)
    computeDownScaleBlend(Float)
    computeBlurParamsInto(Float, Array<Double>, Array<Float>, Array<Float>)
```
```
top.yukonga.miuix.kmp.blur.internal.DownScaleBlend
    <init>(Int, Int, Float)
    getExpLo()
    getExpHi()
    getBlend()
    copy(Int, Int, Float)
```
```
top.yukonga.miuix.kmp.blur.internal.InverseLayerScope
    <init>()
    getSize-NH()
    setSize(Long)
    getDensity()
    setDensity(Float)
    getFontScale()
    setFontScale(Float)
    getScaleX()
    setScaleX(Float)
    getScaleY()
    setScaleY(Float)
    getAlpha()
    setAlpha(Float)
    getTranslationX()
    setTranslationX(Float)
    getTranslationY()
    setTranslationY(Float)
    getShadowElevation()
    setShadowElevation(Float)
    getAmbientShadowColor()
    setAmbientShadowColor(Long)
    getSpotShadowColor()
    setSpotShadowColor(Long)
    getRotationX()
    setRotationX(Float)
    getRotationY()
    setRotationY(Float)
    getRotationZ()
    setRotationZ(Float)
    getCameraDistance()
    setCameraDistance(Float)
    getTransformOrigin()
    setTransformOrigin(Long)
    getShape()
    setShape(ui.graphics.Shape)
    getClip()
    setClip(Boolean)
    getRenderEffect()
    setRenderEffect(ui.graphics.RenderEffect)
    getBlendMode()
    setBlendMode()
    getColorFilter()
    setColorFilter(ui.graphics.ColorFilter)
    getCompositingStrategy-()
    setCompositingStrategy()
    inverseTransform-JM5-EMQ(ui.graphics.drawscope.DrawTransform, ui.unit.Density, Long, Function1)
    reset()
    toPx(Float)
    toPx-(Long)
    roundToPx(Float)
    roundToPx-(Long)
    toDp()
    toDp(Float)
    toDp(Long)
    toSp()
    toSp(Float)
    toSp(Float)
    toRect(ui.unit.DpRect)
    toSize(Long)
    toDpSize-k(Long)
```
```
top.yukonga.miuix.kmp.blur.internal.LayerRecorderKt
    recordLayer(ui.node.DelegatableNode, ui.graphics.drawscope.DrawScope, ui.graphics.layer.GraphicsLayer, Long, Function1)
```
```
top.yukonga.miuix.kmp.blur.internal.RenderEffectCompat_androidKt
    chain(ui.graphics.RenderEffect, ui.graphics.RenderEffect)
    runtimeShaderEffect(shader.RuntimeShader, String)
```
```
top.yukonga.miuix.kmp.blur.internal.ShadersKt
    buildBlurShader()
    getBLUR_SHADER_BY_TAP()
    buildBloomStrokeShader(Boolean)
    getBLOOM_STROKE_SHADER_SINGLE()
    getBLOOM_STROKE_SHADER_DUAL()
    buildBlendModeShader(Boolean)
    getBLEND_MODE_SHADER_STANDARD()
    getBLEND_MODE_SHADER_EXTENDED()
```
```
top.yukonga.miuix.kmp.blur.internal.ShapeProvider
    <init>(() -> Unit)
    getShapeBlock()
    setShapeBlock(() -> Unit)
    getInnerShape()
    getShape()
```
```
top.yukonga.miuix.kmp.blur.sensor.DeviceTilt
    <init>(Float, Float, Float, Float)
    <init>(Float, Float, Float, Float)
    getPitch()
    getRoll()
    getGravityX()
    getGravityY()
    copy(Float, Float, Float, Float)
```
```
top.yukonga.miuix.kmp.blur.sensor.DeviceTilt_androidKt
    rememberDeviceTilt(Float)
```

## 9. 未能从字节码确认的内容（如实说明）

以下信息本清单**没有**给出，原因是字节码里确实取不到，而不是遗漏：

1. **非 Composable、且无 Compose `sourceInformation` 的参数名。**
   Kotlin 的 `@Metadata` 注解把参数名存在名为 `d1` 的二进制 protobuf 块中，
   该块以 JVM modified-UTF-8 字符串存放于常量池。本次逆向已确认它的存在与规模
   （例：`SwitchKt` 的 `d1` 声明长度 351 字节，JVM 解出 310 个 UTF-16 单元、含 9 个 NUL），
   但位级布局未能在本次工作中完全还原，因此对这些声明**只给出类型与顺序**，
   参数名标注为 `字节码未给出`。

   影响范围（其余部分的参数名是完整的）：

   - `miuix-ui` 的 `*Defaults` / `*Colors` 单例函数；
   - `ThemeController` 构造器、`Colors` / `TextStyles` 之外的属性访问器；
   - 整个 `miuix-blur`、`com.kyant.backdrop`、`com.kyant.shapes`（这些模块不含 Compose 源码信息）；
   - `backdrop` / `shapes` 的数据类构造器，以 `constructor(p0: …, p1: …)` 形式给出。

   这些位置**类型是精确的**，按顺序传参即可编译；只是无法告知形参叫什么。

2. **默认值表达式本身。** 字节码里没有默认值字面量（它们位于合成方法或 Compose 控制位中），
   因此文档不给 `= XxxDefaults.yyy` 这类默认值表达式。需要默认值时，
   从同名的 `XxxDefaults` / `XxxColors` 小节里挑成员。

3. **`@Composable` 注解的精确归属。** 判定依据是 JVM 参数尾部是否出现
   `Composer` + `Int`（Compose 编译产物特征），满足即为 Composable。
   没有该特征的方法可能是普通函数或属性访问器，文档按普通 `fun` 列出。

4. **本清单未展开的包。** `miuix-ui` 中还有若干公开包，已收进第 8 节索引，
   但没有展开成第 1–7 节那样的逐参数表格：
   `top.yukonga.miuix.kmp.layout`、`overlay`、`window`、`utils`、`interfaces`、
   `color.api`、`color.core`、`color.space`、`anim`。

5. **`miuix-core` 本身**只有 3 个公开类型：`MiuixIcons`（含 6 个变体内嵌类）、
   `Platform`、`Utils_androidKt`。图标相关内容见第 3 节。

6. **`monkey` 主题。** `ThemeController` 有 `colorSchemeMode` / `lightColors` /
   `darkColors` / `keyColor` / `colorSpec` / `paletteStyle` / `isDark` 与
   `currentColors()`（Composable）。字节码中没有名为 `monkey` 的公开 API；
   动态取色走 `colorSchemeMode` + `keyColor` + `colorSpec` + `paletteStyle`。
   相关类型见 2.5 节的 `ColorSchemeMode` / `ThemeColorSpec` / `ThemePaletteStyle`。
