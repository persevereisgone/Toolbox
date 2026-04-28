# 发现与决策

## 需求
- 用户目标从“优化首页”扩展为“对整个 Android 项目做一次视觉升级”。
- 当前要求：先使用 `planning-with-files-zh` 做完整规划，用户确认后再分批实施。
- 已确认先开始第一批实施：设计系统基建 + 首页与导航区统一。
- 首页需要增加一个默认隐藏、需特定操作才能进入的测试区，方便新功能开发与灰度承载。

## 项目结构发现
- 主页与导航：`fragment_home.xml`、`activity_main.xml`、`content_main.xml`、`app_bar_main.xml`、`nav_header_main.xml`
- 功能容器：`fragment_function.xml`、`HomeFragment.java`、`FunctionFragment.java`
- 功能页：`function_timer.xml`、`function_statistics.xml`、`function_lottery.xml`、`function_lottery_manage.xml`、`function_recorder.xml`、`function_recorder_playlist.xml`、`function_screen_recorder.xml`
- 列表/条目：`panel_item.xml`、`item_statistics.xml`、`item_lottery.xml`、`item_recording.xml`
- 弹窗与悬浮层：`dialog_*`、`overlay_*`、`floating_panel.xml`
- 基础资源：`colors.xml`、`dimens.xml`、`styles.xml`、`themes.xml`

## 技术决策
| 决策 | 理由 |
|------|------|
| 采用“先设计系统，后逐页升级”的路线 | 降低返工，保证视觉统一 |
| 第一批先处理资源基建和导航外壳 | 先建立全局视觉骨架 |
| 保持浅色主题，不在本轮引入夜间模式 | 控制复杂度 |
| 风格定为现代卡片化 + 少量品牌渐变 | 适合工具类应用 |

## 第一批目标
- 统一颜色、间距、圆角、标题样式与按钮样式。
- 抽离可复用的卡片、提示块、标签、按钮、输入框背景。
- 让首页、Toolbar、抽屉头部与内容外壳具备一致视觉语言。

## 风险点
| 问题 | 应对方案 |
|------|---------|
| 首页已存在 `home_*` 私有资源，可能不利于全局复用 | 第一批尽量抽通用资源并逐步替换 |
| 修改导航壳布局可能影响现有绑定与行为 | 优先保留原有控件 id 和结构关系 |
| 资源新增较多易失控 | 采用语义化命名，优先通用而非页面私有 |

## 视觉/结构判断
- 当前首页已经较新，但导航区与其他页面外壳大概率仍偏旧。
- 第一批最重要的不是继续堆效果，而是建立一套后续页面都能复用的基础视觉系统。
- 导航壳统一后，后续改功能页时会明显更顺手。

---
*在每一批实施前后持续补充此文件，记录关键发现与决策*
