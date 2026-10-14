package com.mctaskmanager.client;

/**
 * 界面配色主题。所有颜色都是 0xAARRGGBB。
 *
 * <p>主题是纯数据（无状态），由 {@link MctmConfig#theme} 选择，
 * 页面上色统一通过 {@link com.mctaskmanager.client.page.PageDraw} 暴露的取值器，
 * 所以主题切换会在下一帧立即生效，不需要重建界面。
 */
public final class Theme {

    public final String id;

    // ---- 面板 / 结构 ----
    /** 内容区底色。 */
    public final int bg;
    /** 顶栏与侧边栏底色。 */
    public final int bgMenu;
    /** 侧边栏悬停底色。 */
    public final int menuHover;
    /** 侧边栏选中底色。 */
    public final int menuSelected;
    /** 卡片/输入槽等内嵌块底色。 */
    public final int slot;
    /** 卡片/控件描边。 */
    public final int border;
    /** 分隔线。 */
    public final int sep;
    /** 内容卡片底色；等于 {@link #bg} 表示不使用卡片式层次。 */
    public final int panel;

    // ---- 文字 ----
    public final int text;
    public final int textSecond;

    // ---- 强调色 ----
    /** 主强调色（选中条、顶栏高亮）。 */
    public final int accent;
    /** 次强调色（曲线、进度条，未覆盖时同 {@link #accent}）。 */
    public final int accent2;

    // ---- 语义色 ----
    public final int green;
    public final int yellow;
    public final int red;
    public final int blue;
    public final int gold;
    public final int gray;

    // ---- 派生色 ----
    /** 滚动条轨道。 */
    public final int scrollTrack;
    /** 滚动条滑块。 */
    public final int scrollThumb;
    /** 表格斑马纹叠加色（含透明度）。 */
    public final int rowAlt;
    /** 表格行悬停叠加色。 */
    public final int rowHover;
    /** 表格行选中叠加色。 */
    public final int rowSelected;

    Theme(String id, int bg, int bgMenu, int menuHover, int menuSelected, int slot,
          int border, int sep, int panel, int text, int textSecond,
          int accent, int accent2, int green, int yellow, int red, int blue,
          int gold, int gray, int scrollTrack, int scrollThumb,
          int rowAlt, int rowHover, int rowSelected) {
        this.id = id;
        this.bg = bg;
        this.bgMenu = bgMenu;
        this.menuHover = menuHover;
        this.menuSelected = menuSelected;
        this.slot = slot;
        this.border = border;
        this.sep = sep;
        this.panel = panel;
        this.text = text;
        this.textSecond = textSecond;
        this.accent = accent;
        this.accent2 = accent2;
        this.green = green;
        this.yellow = yellow;
        this.red = red;
        this.blue = blue;
        this.gold = gold;
        this.gray = gray;
        this.scrollTrack = scrollTrack;
        this.scrollThumb = scrollThumb;
        this.rowAlt = rowAlt;
        this.rowHover = rowHover;
        this.rowSelected = rowSelected;
    }

    /** 是否使用卡片式内容区（内容底色与窗口底色不同）。 */
    public boolean hasPanel() {
        return panel != bg;
    }

    // ===== 预设 =====

    /** A. 原版扁平灰：保持与旧版一致的观感。 */
    public static final Theme FLAT = new Theme("flat",
            0xFFC6C6C6, 0xFFB8B8B8, 0xFFD0D0D0, 0xFFD8D8D8, 0xFFB8B8B8,
            0xFF9A9A9A, 0xFFA8A8A8, 0xFFC6C6C6, 0xFF202020, 0xFF606060,
            0xFF3F7FBF, 0xFF3FAA3F, 0xFF3FAA3F, 0xFFB08020, 0xFFAA3030,
            0xFF3060B0, 0xFF9A6A00, 0xFF808080, 0xFFB0B0B0, 0xFF888888,
            0x14000000, 0x22000000, 0x2E2F6FB8);

    /** B. 深色 · 青绿强调：现代 IDE 观感。 */
    public static final Theme SLATE = new Theme("slate",
            0xFF1E2227, 0xFF171A1F, 0xFF2A3038, 0xFF2F3742, 0xFF262B33,
            0xFF39414B, 0xFF2E353E, 0xFF232830, 0xFFE6EAF0, 0xFF98A2B0,
            0xFF2DD4BF, 0xFF2DD4BF, 0xFF4ADE80, 0xFFFBBF24, 0xFFF87171,
            0xFF60A5FA, 0xFFFCD34D, 0xFF6B7280, 0xFF262B33, 0xFF4A5361,
            0x14FFFFFF, 0x24FFFFFF, 0x3D2DD4BF);

    /** C. 深色 · 蓝紫强调：数据可视化对比更强。 */
    public static final Theme INDIGO = new Theme("indigo",
            0xFF1B1D29, 0xFF151724, 0xFF262A3D, 0xFF2C3149, 0xFF232739,
            0xFF3A3F58, 0xFF2C3145, 0xFF20232F, 0xFFE8EAF6, 0xFF9AA0C0,
            0xFF818CF8, 0xFF818CF8, 0xFF34D399, 0xFFFBBF24, 0xFFFB7185,
            0xFF60A5FA, 0xFFFCD34D, 0xFF6C7293, 0xFF232739, 0xFF3F4560,
            0x14FFFFFF, 0x24FFFFFF, 0x3D818CF8);

    /** D. 清爽浅色（默认）：卡片式内容区 + 蓝/绿双色强调。 */
    public static final Theme CLEAN = new Theme("clean",
            0xFFEFF1F4, 0xFFF7F8FA, 0xFFE8ECF1, 0xFFDDE7F5, 0xFFE4E8EE,
            0xFFD6DBE3, 0xFFE3E7ED, 0xFFFFFFFF, 0xFF1F2933, 0xFF6B7684,
            0xFF3572D6, 0xFF1FA971, 0xFF1FA971, 0xFFC98A00, 0xFFD2453F,
            0xFF3572D6, 0xFFB07A00, 0xFF9AA3AF, 0xFFDDE1E7, 0xFFB4BCC7,
            0x0D000000, 0x17000000, 0x243572D6);

    /** 全部预设，顺序即为设置界面的显示顺序。 */
    public static final Theme[] PRESETS = { CLEAN, SLATE, INDIGO, FLAT };

    public static final Theme DEFAULT = CLEAN;

    /** 按 id 查找；未命中时返回 {@link #DEFAULT}（例如配置文件被手改坏）。 */
    public static Theme byId(String id) {
        if (id != null) {
            for (Theme t : PRESETS) {
                if (t.id.equalsIgnoreCase(id)) return t;
            }
        }
        return DEFAULT;
    }

    /** 该主题在语言文件中的显示名键。 */
    public String nameKey() {
        return "mctm.theme." + id;
    }

    /** 该主题在语言文件中的说明键。 */
    public String descKey() {
        return "mctm.theme." + id + ".desc";
    }
}
