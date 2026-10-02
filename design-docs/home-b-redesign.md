# 首页 B 方案 UI 优化方案（落盘 · 不动代码）

> 风格：B · 浅色效率版。依据 mobile-android-design（M3 卡片/48dp/深色）+ ui-ux-pro-max（P1 无障碍 > P2 触摸 > P5 布局 > P6 字阶）。
> 预览：`home_redesign_preview.html` 中 B 版。功能零删减。

## 1. 版式结构（自上而下）

1. 顶行：`TOOLBOX` 黑底 pill（左）+ 更新日志胶囊按钮（右，红点 badge 8dp，有未读才显示）。
2. 标题区：`常用操作，一步即达`（30sp/800，一步即达用 app_primary 高亮）+ 副标题 13sp tertiary。
3. 搜索框：圆角 999px，`搜索功能…`，hint 色 tertiary，点击跳搜索/过滤（功能不变，只是入口显性化）。
4. 常用功能：标题行（17sp + 右侧 `N 个 · 全部 ›`）+ 列表（非宫格），每行 1 个功能。
5. 小贴士：深底卡（#14181F），白字标题 + 灰字正文。
6. 更多功能（BETA）：标题行 + 列表同样式，badge 标 BETA。

## 2. 列表行规范（核心组件）

- 行高 ≥64dp，图标 44dp 圆角 12dp（各功能浅底色：计时蓝/菜谱橙/待办绿/闹钟紫…），标题 14sp bold + 副标题 11sp（放实时状态：计时剩余/待办未完成数/闹钟时间/收藏数）。
- 右侧 `›` 18sp `#C3CCD9`，整行 ripple + 按压缩放 0.98（motion_press 100ms）。
- 进入 stagger：alpha+16dp 位移，200ms，30ms 递增封顶 5 项。

## 3. Token 映射

- 背景 `app_bg #F3F7FC`，卡片白 `#FFFFFF` + 描边 `#E7EDF6` 1dp + 0 elevation。
- 字阶只用 5 档：TitleLarge 22 / TitleMedium 16b / Body 14 / Caption 12 / Label 11。
- 间距 8dp 节奏：页边 20dp（手机）/ 24dp（平板/横屏），卡间距 12~16dp。
- 动效：enter 200 / exit 120 / stagger 30 / press 100，仅 transform+alpha。

## 4. 无障碍/深色

- 触摸 ≥48dp；图标按钮全 contentDescription；badge 用完整短语播报。
- 深色映射：bg→#12161F，卡→#1B2538，标题→#E6E1E5，正文→#B8C4D4；小贴士深底在深色下改为 surface_alt + 描边，避免黑上黑。

## 5. 落地清单（动代码时按此改）

- `fragment_home.xml`：ScrollView padding 走 dimens；header 渐变块删掉换白底标题区；GridView 换 RecyclerView 列表；tip 卡换深底样式；beta 区复用列表行。
- `home_header_bg/home_chip_bg`：B 版不再用渐变头，如保留 A 版则不动。
- `HomeFragment.java`：宫格 adapter 换列表 adapter，行数据加副标题状态字段；搜索框接过滤；badge 未读逻辑不变。
