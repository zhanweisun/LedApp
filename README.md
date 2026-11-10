# LED Scroller（功能复刻版）

从 `/home/jack/Downloads/LED Scroller.apk`（`com.wssc.ledscroller.pro`，v1.0.3.0）反编译、
学习其功能后用标准 Android 工程重新实现的同功能 App，资源（字体、图标、图片、视频、
表情数据、颜色、尺寸）全部直接沿用原 APK。

## 一、构建

```bash
export JAVA_HOME=/home/jack/Software/jdk21
export ANDROID_HOME=/home/jack/Software/AndroidSDK

# 调试包
/home/jack/Software/gradle-8.9/bin/gradle :app:assembleDebug
# 发布包（使用 debug 签名，便于直接安装）
/home/jack/Software/gradle-8.9/bin/gradle :app:assembleRelease
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

`local.properties` 中已写好 `sdk.dir=/home/jack/Software/AndroidSDK`。
工程使用 AGP 8.7.3 / Gradle 8.9 / compileSdk 35 / minSdk 24。

### 用 Android Studio 打开

1. **Android Studio → File → Open**，选择工程根目录 `/home/jack/LedApp`
   （即 `settings.gradle.kts` 所在目录，**不要**只选 `app` 子目录），点 OK，
   提示 “Trust Project” 时选 **Trust**。
2. 等右下角 Gradle Sync 完成。工程已自带 `gradlew` + `gradle/wrapper`，
   Android Studio 会直接用 Gradle 8.9 同步，不需要本机装 Gradle。
3. 若提示找不到 SDK：`File → Settings → Languages & Frameworks → Android SDK`，
   `Android SDK Location` 填 `/home/jack/Software/AndroidSDK`；或确认工程根目录
   `local.properties` 里是 `sdk.dir=/home/jack/Software/AndroidSDK`。
4. 建议的 JDK：`Settings → Build, Execution, Deployment → Build Tools → Gradle →
   Gradle JDK` 选择 `jdk21`（`/home/jack/Software/jdk21`）。Android Studio 自带 JBR 也可以。
5. 运行：顶部运行配置选 **app**，连接手机（开 USB 调试）或启动模拟器后点 ▶ Run。

常用操作：`Build → Generate Signed Bundle / APK` 可用自己的签名打包；命令行等价命令：

```bash
cd /home/jack/LedApp
./gradlew :app:assembleDebug     # 调试包
./gradlew :app:assembleRelease   # 发布包（默认用 debug 签名）
```

可选参数 `-Pled.fullResources=true` 会把原 APK 的**全部**混淆资源（含未用到的
Material/AppCompat 布局）一起打包进 APK。

## 二、功能对照（原版 → 本工程）

| 原版功能 | 实现位置 |
| --- | --- |
| 启动页（图标 + 渐隐后进入主界面） | `SplashActivity` + `dialog/SplashDialog` |
| 主界面实时预览 LED 文本 | `MainActivity` + `widget/LEDScrollerView` |
| 滚动方向：左到右 / 静止 / 右到左 | `LEDScrollerView.setDirection` |
| 速度 / 闪烁 | `LEDScrollerView.setSpeed / setBlinkPercent`，SeekBar 调节 |
| 屏幕亮度 | `MainActivity.setBrightness`（窗口 `screenBrightness`） |
| LED 点阵大小（灯珠间距） | `LEDScrollerView.setMarkSpace`（生成 45° 点阵遮罩） |
| 字体大小 | `LEDScrollerView.setFontPercent` |
| 字体类型（10 种内置字体） | `Fonts` + `dialog/FontChooseDialog` + `res/font` |
| 字体颜色 / 背景颜色 / 边缘颜色 | `dialog/ColorPickerDialog`（色板 + HEX 输入） |
| 边缘流光灯效开关 / 半径 / 粗细 | `widget/EdgeLightView` |
| 表情输入 | `EmojiPickerDialog`（读取 `assets/emoji.json`，1819 个表情） |
| 全屏展示（横屏、常亮、点击显示关闭按钮） | `LEDScrollerActivity` |
| **背景效果（视频/图片）** | `widget/BackgroundVideoView`（TextureView+MediaPlayer，居中裁剪）+ `LEDScrollerView` 合成点阵遮罩 |
| **全部背景素材** | 图片 26 张（`assets/image/led_image_bg_0~25`）+ 视频 24 个（`assets/video/led_video_bg_0~24`），缩略图在 `image_thumb/`、`video_thumb/`，与原版一致 |
| 背景效果切换 / 导入素材 | 主界面「Background Effect」分段按钮，`+` 从相册导入视频或图片（复制到私有目录） |
| 全屏页背景视频 / 图片 | `LEDScrollerActivity` 的 `VideoView` + `assets/video/led_video_bg_0.mp4`、`assets/image/led_image_bg_0.webp` |
| 设置持久化 | `Settings`（SharedPreferences） |
| 左上角菜单 | `MainActivity.showMenu`（PopupMenu） |

## 三、目录结构

```
LedApp/
├── app/build.gradle.kts        # 模块配置（可 -Pled.fullResources=true 打包原 APK 全部资源）
├── settings.gradle.kts
├── gradle.properties
├── local.properties            # sdk.dir
└── app/src/main/
    ├── AndroidManifest.xml
    ├── assets/                 # 原 APK assets：emoji.json、视频、图片、字体
    ├── java/com/ledapp/scroller/
    │   ├── MainActivity.java            # 主界面
    │   ├── LEDScrollerActivity.java     # 全屏播放页
    │   ├── SplashActivity.java          # 启动页
    │   ├── Settings.java                # 设置持久化
    │   ├── Fonts.java                   # 字体表
    │   ├── EmojiPickerDialog.java       # 表情面板
    │   ├── Backgrounds.java             # 背景素材（内置/导入/缩略图）
    │   ├── MediaSources.java / AssetProvider.java  # assets 素材访问
    │   ├── dialog/                      # 字体、颜色、启动、底部弹窗基类
    │   └── widget/
    │       ├── LEDScrollerView.java     # LED 点阵滚动文本控件（核心）
    │       ├── BackgroundVideoView.java # 背景视频（TextureView，居中裁剪）
    │       ├── EdgeLightView.java       # 边缘流光控件
    │       ├── SegmentTabLayout.java    # 原版分段选择控件
    │       ├── LEDDrawerLayout.java
    │       └── RoundConstraintLayout.java
    └── res/
        ├── layout/activity_main.xml             # 主界面（结构/尺寸/图标取自原版 activity_main）
        ├── layout/activity_led_scroller.xml     # 全屏页
        ├── layout/dialog_font_choose.xml
        ├── layout/dialog_color_picker.xml
        ├── values/*.xml                         # 原 APK 的颜色/尺寸/样式/字符串（混淆名）
        ├── values/app_styles.xml                # 本工程主题（AppTheme）
        ├── values/app_strings.xml               # 本工程文案
        ├── values-zh-rCN/app_strings.xml        # 中文文案
        ├── drawable*/****  layout*/****         # 原 APK 的图标、背景图、矢量图
        ├── font/                                # 原 APK 的 10 个字体（Bebas/Jersey/Rubik/Manrope）
        ├── mipmap*/                             # 原 APK 的应用图标
        ├── color*/ anim*/ animator*/ interpolator*/ xml*/ menu*/ raw/  # 原 APK 其余资源
        └── values-*/                            # 原 APK 的多语言字符串
```

## 四、实现要点

1. **LED 效果**：`LEDScrollerView` 先按“字体比例 × 屏幕短边”算出字号，用
   `StaticLayout` 排版后按帧平移（`Choreographer`），再叠加一层 45° 排列的黑色圆点
   遮罩，形成“灯珠被文字点亮”的效果；闪烁通过每隔约 280 帧隐藏一半时间实现。
2. **背景动画**：主界面预览区由 `BackgroundVideoView`（TextureView + MediaPlayer，静音循环、
   居中裁剪）播放原 APK 自带的 `led_video_bg_0.mp4`，再由 `LEDScrollerView` 每帧取帧并和文字一起
   叠加点阵遮罩，所以背景会和文字一起变成“LED 灯珠”动画；图片模式则直接绘制位图。
   assets 素材通过 `AssetProvider`（`content://com.ledapp.scroller.assets/...`）交给 MediaPlayer 读取。
3. **全部背景素材**：原版的背景素材原本放在云端 CDN
   （`d3kxn1773cbprm.cloudfront.net/background/...`），这里已全部下载并内置进 APK：
   图片 26 张（1920x1080，含 1 张 assets 自带的 0 号）、视频 24 个（含 1 个 assets 自带的 0 号），
   并为每个素材预先截取/压缩了缩略图，素材列表用 RecyclerView 横向滚动（缩略图后台解码）。
   由于素材体积较大（约 160MB），APK 体积约 170MB，且设置了 `largeHeap`。
4. **资源沿用**：原 APK 经过资源混淆（资源名被压缩成 `a`、`a0`…），本工程把解包后的
   资源原样放入 `res/`，代码中通过 `R.color.a9h`、`R.drawable.ht` 等直接引用，
   与原版代码里的 `R$color.a9h` 一一对应。
5. **两处资源名调整**：Android/AGP 不允许资源名使用 Java 关键字，原 APK 中名为
   `do`/`if` 的颜色、样式、布局被改名为 `kw_do`/`kw_if`（内容不变）。
6. **控件替换**：原版主界面用自定义的 `SegmentTabLayout`、`SmartDragLayout` 等第三方控件，
   本工程用标准 `SeekBar`、`SwitchCompat`、`MaterialButton` 和自写的 `SegmentTabLayout`
   实现同样外观与交互；原布局 `res/layout/activity_main.xml` 的结构、尺寸、图标、
   配色均按原版 1:1 复刻。
7. **主题注意**：原 APK 的部分自定义属性名与新版 Material 库属性冲突（会被本工程属性表
   覆盖），因此主题 `AppTheme` 直接继承 `Theme.MaterialComponents.NoActionBar`，
   所有颜色全部用 `@color` 直接指定，保证深色界面与原版一致。

## 五、反编译分析

详见 [docs/反编译分析.md](docs/反编译分析.md)：原 APK 的类结构、设置项键名、LED 控件的
字号/速度/点阵/闪烁计算方式、颜色规则等。

## 六、已验证

在 Android 15（API 35）x86_64 模拟器上安装运行，逐项验证通过：
启动页 → 主界面（实时滚动预览、点阵效果、边缘流光、8 个设置项、表情面板、字体面板、
调色面板、预设色）→ 全屏展示页。debug / release 两个包均正常。
## 七、资源命名（语义化）

原 APK 的资源被混淆成 `a9h`、`k2`、`do` 这类短名。本工程把**实际用到的**资源
都改成了看得懂的名字（其余未使用的混淆资源保持原样，避免破坏原版资源）。

### 布局文件

| 文件 | 用途 |
| --- | --- |
| `layout/activity_main.xml` | 主界面（预览 + 全部设置项） |
| `layout/activity_led_scroller.xml` | 全屏播放页 |
| `layout/item_effect.xml` | 背景素材单元格 |
| `layout/dialog_font_choose.xml` / `dialog_color_picker.xml` | 字体 / 颜色对话框 |

### 主界面控件 id（`activity_main.xml`）

| 区域 | id |
| --- | --- |
| 预览区 | `preview_container`、`preview_background_video`、`preview_background_image`、`led_scroller_view`、`edge_light_view` |
| 输入区 | `input_section`、`input_row`、`input_text`、`menu_button`、`emoji_button`、`clear_button` |
| 方向 | `direction_ltr`、`direction_pause`、`direction_rtl` |
| 滑动条 | `speed_seek`、`blink_seek`、`brightness_seek`、`led_size_seek`、`font_size_seek`、`edge_radius_seek`、`edge_size_seek` |
| 字体 | `font_type_row`、`font_type_label`、`font_type_value` |
| 颜色 | `font_color_picker`、`background_color_picker`、`edge_color_picker` |
| 背景效果 | `effect_mode_tabs`、`video_effect_row`、`video_effect_list`、`video_effect_add_button`、`image_effect_row`、`image_effect_list`、`image_effect_add_button` |
| 边缘 / 其它 | `edge_switch`、`edge_color_row`、`full_screen_switch`、`play_bar`、`play_button` |

### 全屏页控件 id

`background_video`、`preview_background_image`、`led_scroller_view`、`edge_light_view`、`close_button`

### 其它资源

| 类型 | 新名字 |
| --- | --- |
| drawable | `icon_button_bg`、`icon_menu`、`icon_emoji`、`icon_close_circle`、`icon_direction_ltr/pause/rtl`、`icon_color_wheel`、`icon_play`、`icon_add`、`icon_checked` |
| 颜色（`values/app_colors.xml`） | `background_dark`、`surface_card`、`surface_divider`、`text_primary`、`text_secondary`、`accent_blue`、`led_default_text` 等 |
| 字体（`font/`） | `bebas_neue`、`jersey_10/15/20`、`manrope_bold/medium/regular`、`rubik_bold/medium/regular` |
| 字符串（`values/app_strings.xml`） | `direction`、`speed`、`blink`、`brightness`、`led_size`、`font_size`、`font_type`、`font_color`、`background_color`、`background_effect`、`edge_color`、`edge_radius`、`edge_size`、`play` 等 |
| 样式（`values/app_styles.xml`） | `AppTheme`、`LedButton`、`LedSlider` |

## 八、资源清理

原 APK 的 `res/` 里有 1030 个文件，其中大部分是 Material/AppCompat 库的混淆资源与
本工程用不到的图片。已用脚本分析引用关系后清理：

| 项目 | 清理前 | 清理后 |
| --- | --- | --- |
| 资源文件 | 1030 个 | 375 个 |
| res 体积 | 7.2 MB | 0.2 MB（另有 font 1.9 MB） |
| layout | 166 | 25（其余由依赖库提供） |

保留的资源：
- 本工程用到的图标 / 形状（已改成语义化名字，如 `icon_menu`、`icon_button_bg`）；
- 原版 `values/`（styles/colors/dimens）——它们仍被原版样式引用，删除会编译失败；
- 已删除全部 `values-vXX`（v25~v34）与 `color-v31`：它们是原 APK 里 Material 主题的
  按版本覆盖样式（主题本身没被使用），删除后编译与运行均正常。
- 应用图标（`mipmap-*`）、字体（`font/`）、多语言字符串。

### 维护脚本

```bash
python3 tools/clean_res.py report   # 统计资源体积、数量、未使用资源
python3 tools/clean_res.py unused   # 删除未使用的资源（先跑 ./gradlew :app:mergeDebugResources）
python3 tools/clean_res.py group    # 把原 APK 遗留的混淆资源归类到 apk_<type> 目录
```

> 说明：`values/` 里的 `colors.xml`、`styles.xml` 等仍是原 APK 的混淆命名（`a9h`、`x5`…），
> 因为整个样式链互相引用且体量很大（约 4600 条），改名收益低于风险，故保留原名；
> 本工程自己的颜色 / 文案 / 样式在 `values/app_colors.xml`、`app_strings.xml`、`app_styles.xml`。

### 多语言与屏幕适配目录

只保留两种语言，并去掉原 APK 遗留的屏幕形态目录：

| 保留 | 说明 |
| --- | --- |
| `values` | 默认资源（英文原文 + 原版样式/尺寸/颜色） |
| `values-en` | 英文 |
| `values-zh-rCN` | 简体中文（含本工程自有的 `app_strings.xml`） |

已删除：
- 其他语言：`cs / de / el / es / fi / fr / in / it / ja / ko / pt / ru / th / tr`；
- 繁体中文：`values-zh-rTW`，以及通用的 `values-zh`（避免繁体机型命中）；
- 屏幕形态 / 尺寸目录：`values-land`、`values-port`、`values-large`、`values-xlarge`、
  `values-small`、`values-night`、`values-ldrtl`、`values-watch`、
  `values-sw320dp ~ values-sw680dp`、`values-w*-land`、`values-h*-land` ——
  这些都是原版 Material 库的响应式尺寸/样式，本工程界面用固定 dp，不依赖它们。

### 动画资源命名（res/anim）

按动画动作重命名，`anim_<动作>_<方向>`：

| 文件 | 动作说明 |
| --- | --- |
| `anim_fade_in` / `anim_fade_out` | 淡入 / 淡出 |
| `anim_fade_in_quad` / `anim_fade_out_quad` | 淡入淡出（quad 缓动，较快） |
| `anim_fade_out_slow` | 较慢的淡出 |
| `anim_slide_up_fade_in` / `anim_slide_down_fade_out` | 上滑淡入 / 下滑淡出（默认转场） |
| `anim_slide_up_fade_in_fast` / `anim_slide_down_fade_out_fast` | 同上（更快的版本，用于底部弹层） |
| `anim_slide_in_right` / `anim_slide_out_right` | 从右侧滑入 / 向右滑出 |
| `anim_scale_fade_in` | 缩小后放大 + 淡入（弹窗出现） |
| `anim_scale_up_fade_in` / `anim_scale_down_fade_out` | 底部放大淡入 / 缩小淡出（M3 底部弹层） |
| `anim_interpolator_ease_in_out` | 缓入缓出插值器（被 animator 引用） |

脚本：`python3 tools/rename_anim.py`（可按同样规则再扩展）。

### 动画资源命名（res/animator）

同样按用途命名，前缀 `animator_`：

| 文件 | 用途 |
| --- | --- |
| `animator_button_elevation_pressed` | 按钮按下时的海拔(Z) |
| `animator_card_elevation_default` / `_raised` / `_selected` | 卡片的默认 / 悬浮 / 选中海拔 |
| `animator_checkbox_elevation_selected` | 勾选选中状态的海拔 |
| `animator_chip_elevation` | Chip 按下/悬停海拔 |
| `animator_text_field_elevation_pressed` | 输入框按下海拔 |
| `animator_small_button_elevation_pressed` | 小按钮按下海拔 |
| `animator_button_elevation_tonal` | 色调按钮（tonal）海拔 |
| `animator_button_elevation_expanded_state` / `_colapsed_state` | 展开 / 收起状态的按钮海拔 |
| `animator_button_elevation_layer` / `_raised` | 带层的按钮 / 上浮按钮海拔 |
| `animator_empty_state_list` | 空状态列表（无动画） |
| `animator_size_change_expand` / `_collapse` | 宽高+内边距的展开 / 收缩 |
| `animator_fab_scale_expand` / `_collapse` | FAB 展开 / 收起的透明度+缩放 |
| `animator_fab_icon_expand_slow` / `_collapse_fast` | FAB 图标的展开 / 收起 |

脚本：`python3 tools/rename_animator.py`

### 颜色资源命名（res/color）

`res/color` 下 157 个 ColorStateList（控件状态色）已按“用途 + 状态”重命名，前缀 `color_`：

| 分类 | 示例 |
| --- | --- |
| 文字 | `color_text`、`color_text_primary`、`color_text_secondary`、`color_text_hint`、`color_text_disabled`、`color_text_checked` |
| 按钮 | `color_button_background`、`color_button_background_disabled`、`color_button_text` |
| Chip | `color_chip_background`、`color_chip_background_pressed`、`color_chip_background_checked` |
| 导航 / 抽屉 | `color_navigation_item_checked`、`color_drawer_item_checked` |
| 控件基础色 | `color_control_activated`、`color_control_normal`、`color_control_checked` |
| 对话框 | `color_dialog_surface`、`color_dialog_text`、`color_dialog_button_text` |
| 其它 | `color_primary`、`color_on_primary`、`color_surface`、`color_swipe_refresh`、`color_window_background` |

命名规则是取名时根据“被哪个控件属性引用（textColor / backgroundTint / chipBackgroundColor…）”
再叠加 selector 状态（checked / pressed / disabled / selected），同用途多个变体加序号，
如 `color_chip_background_pressed_2`。

脚本：`python3 tools/rename_color.py`

## 九、res 目录命名规范（全量语义化）

原 APK 的混淆资源名已全部替换为"场景 + 用途"式命名（`kw_do` 等少数保留原名者除外），
命名前后缀规则如下：

| 目录 | 命名规则 | 示例 |
| --- | --- | --- |
| `layout/` | `appcompat_*`（库模板）、`activity_*`、`item_*`、`dialog_*` | `appcompat_dialog_alert_button_bar`、`appcompat_list_item_check_single` |
| `drawable/` | `ic_*` 图标、`bg_*` 形状、`ripple_*` 水波纹、`selector_*` 状态选择器、`inset_*`、`layer_*`、`anim_*` 动画矢量、`trans_*` 转场 | `ic_share`、`ic_chevron_left`、`bg_card_rounded`、`selector_tab_text` |
| `drawable-mdpi/` | `np_*` 九宫格（.9.png） | `np_button_bg`、`np_dialog_background` |
| `anim/` | `anim_<动作>_<方向>` | `anim_slide_up_fade_in` |
| `animator/` | `animator_<用途>` | `animator_button_elevation_pressed` |
| `color/` | `color_<用途>[_状态]` | `color_text_primary`、`color_chip_background_pressed` |
| `interpolator/` | `interpolator_<曲线>` | `interpolator_fast_out_slow_in` |
| `mipmap/` | `ic_launcher`、`ic_launcher_round`、`ic_launcher_foreground`、`ic_splash_logo` | — |
| `font/` | 字体原名 | `rubik_medium`、`jersey_15` |
| `values*` | 见"资源命名"与"多语言"章节 | `app_colors.xml`、`app_strings.xml`、`app_styles.xml` |

对应脚本都在 `tools/` 下：`rename_layout.py`、`rename_drawable.py`、`rename_anim.py`、
`rename_animator.py`、`rename_color.py`、`rename_interpolator.py`、`clean_res.py`。
