package com.ultraguru.liquidglass.data

/**
 * 内置插件目录 —— 五大顶级开源液态玻璃库。
 *
 * 顺序即主列表展示顺序（用户指定）：kyant0 第一；倒数第二「关于」、最后「设置」
 * 在 UI 层拼接，不在此处。
 */
data class LibraryEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val repoUrl: String,
    val license: String,
    val stars: Int,
    val author: String,
    val coordinate: String,
    val description: String,
    /** 四大演示场景在该库上的能力速评（详情页展示）。 */
    val sceneNotes: Map<String, String>
)

object Catalog {

    val libraries: List<LibraryEntry> = listOf(
        LibraryEntry(
            id = "kyant0",
            title = "Kyant0 · AndroidLiquidGlass",
            subtitle = "Compose 液态玻璃标杆",
            repoUrl = "https://github.com/Kyant0/AndroidLiquidGlass",
            license = "Apache-2.0",
            stars = 4202,
            author = "Kyant0",
            coordinate = "io.github.kyant0:backdrop:1.0.6",
            description = "Compose Multiplatform 液态玻璃效果：layerBackdrop 源捕获 + drawBackdrop " +
                "绘制，折射(lens)/高光/阴影/色散自由组合，iOS 26 质感的最正统 Compose 实现。",
            sceneNotes = mapOf(
                "dark_bar" to "drawBackdrop + blur + lens 组合做栏玻璃",
                "buttons" to "Backdrop Shapes + 折射按钮",
                "liquid" to "lens 折射高度实时动画",
                "biomorphic" to "自定义 Shape 驱动 SDF"
            )
        ),
        LibraryEntry(
            id = "cloudy",
            title = "skydoves · Cloudy",
            subtitle = "GPU 加速模糊 + 液态玻璃",
            repoUrl = "https://github.com/skydoves/Cloudy",
            license = "Apache-2.0",
            stars = 1284,
            author = "skydoves (Jaewoong Eum)",
            coordinate = "com.github.skydoves:cloudy:1.0.0-alpha01",
            description = "Kotlin Multiplatform Compose 模糊与液态玻璃库：cloudy 自身模糊、" +
                "sky+cloudy 背景渐进模糊、liquidGlass 折射色散、陀螺仪动态光源、GPU 加速渲染。",
            sceneNotes = mapOf(
                "dark_bar" to "cloudy 渐进模糊(TopToBottom) 玻璃栏",
                "buttons" to "liquidGlass modifier 按钮组",
                "liquid" to "gyro 光源 + lens 移动",
                "biomorphic" to "GlassCard 組件 + 呼吸动画"
            )
        ),
        LibraryEntry(
            id = "qmdeve",
            title = "QmDeve · AndroidLiquidGlassView",
            subtitle = "View 体系真实折射",
            repoUrl = "https://github.com/QmDeve/AndroidLiquidGlassView",
            license = "MIT",
            stars = 329,
            author = "QmDeve",
            coordinate = "com.qmdeve.liquidglass:core:1.1.0",
            description = "iOS 26 液态玻璃的 Android View 体系复刻：AGSL RuntimeShader 真实折射、" +
                "色散、拖拽弹性、触摸形变，bind 到任意背景 ViewGroup 即刻玻璃化。",
            sceneNotes = mapOf(
                "dark_bar" to "LiquidGlassView bind 滚动背景",
                "buttons" to "折射 + 触摸形变按钮",
                "liquid" to "draggable + elastic 弹性液滴",
                "biomorphic" to "cornerRadius 动画形变"
            )
        ),
        LibraryEntry(
            id = "qwea0",
            title = "QWEA0 · Liquid-Glass-Android",
            subtitle = "SDF 折射 + 传感器视差",
            repoUrl = "https://github.com/QWEA0/Liquid-Glass-Android",
            license = "MIT",
            stars = 214,
            author = "QWEA0",
            coordinate = "com.github.QWEA0:liquidglass:v2.0.11",
            description = "View 体系液态玻璃：真实 SDF 折射、物理色散、传感器驱动视差、" +
                "水银次级形状融合，自带 Button/Fab/TabBar/Chip/Toast/Dialog 等 9 组件。",
            sceneNotes = mapOf(
                "dark_bar" to "LiquidGlass TabBar/顶栏组件",
                "buttons" to "GlassButton/Fab/Chip 组件矩阵",
                "liquid" to "动态背景 + 传感器视差",
                "biomorphic" to "setSecondaryShape 水银融合"
            )
        ),
        LibraryEntry(
            id = "nadeem",
            title = "NadeemIqbal · liquid-glass",
            subtitle = "组件化 Glass UI",
            repoUrl = "https://github.com/NadeemIqbal/liquid-glass",
            license = "Apache-2.0",
            stars = 9,
            author = "Nadeem Iqbal",
            coordinate = "io.github.nadeemiqbal:liquid-glass:0.2.3",
            description = "Compose Multiplatform 液态玻璃组件库：liquidGlassSource/liquidGlass " +
                "modifier + GlassCard/GlassButton/GlassNavBar/GlassDialog/GlassBottomSheet 现成组件，" +
                "三档质量自动降级。",
            sceneNotes = mapOf(
                "dark_bar" to "GlassNavBar 组件",
                "buttons" to "GlassButton 组件矩阵",
                "liquid" to "liquidGlass 折射液滴",
                "biomorphic" to "GlassCard + 形状呼吸"
            )
        )
    )

    fun byId(id: String): LibraryEntry? = libraries.firstOrNull { it.id == id }
}
