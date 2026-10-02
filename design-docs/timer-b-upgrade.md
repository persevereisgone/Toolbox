# 定时返回 B 版升级落地文档（待审核，审完再开工）

> 定稿：`timer_upgrade_v2.html` 中 **B · 无卡片效率流**（B-1 主页 / B-2 自定义 sheet / B-3 剩余 sheet / B-4 悬浮窗）。
> 双 skill：mobile-android-design（M3 卡/导航/主题/48dp）+ ui-ux-pro-max（P1~P10 + pro-rules 清单）。

## 1. 功能×新入口对照（13 项，零丢失）

| # | 功能 | 新位置/形态 | 说明 |
|---|---|---|---|
| 1 | 瘦标题+说明 | B-1 页顶行内 | 不变 |
| 2 | 倒计时大字+状态 | B-1 裸放，点大字→B-3 sheet | 不变 |
| 3 | 6 预设 | B-1 横滑一条 | 从 3×2 网格改横滑，6 档全保留 |
| 4 | 自动锁屏开关 | B-1 行式（说明内联） | 逻辑不变，缺权限引导设置 |
| 5 | 自定义时长 | B-1 行式“自定义时长 ›”→B-2 sheet | 滚轮+快捷预设+记忆上次 |
| 6 | 上次摘要 | **删除**（已确认） | sheet 内“记住上次”即替代 |
| 7 | 取消定时 | B-1 通栏 danger 键→中心确认框 | 二次确认保留 |
| 8 | 刷新状态 | **删除**（已确认） | 每秒自动刷新替代 |
| 9 | 底部说明 | B-1 页尾居中 | 不变 |
| 10 | MainActivity 八档 dialog | **并入本页**（已确认） | 悬浮窗菜单“调整时长”跳回本页；MainActivity 该 dialog 废弃 |
| 11 | 剩余查询 | B-3 bottom-sheet（统一语言） | MainActivity 侧 Toast 升级为框 |
| 12 | 悬浮窗 | B-4 紧凑条+菜单+收边 | 已实装，不动 |
| 13 | 常驻通知 | 下拉栏，点回本页+取消动作 | 不动 |

## 2. 版式/Token 映射

- 背景白 `#FFFFFF`（B 版主页纯白，非 #F6F8FC），分区靠 `divider #EDF0F5`，无卡片。
- 字阶：标题 17 / 大字 52 / 行标题 14 / 说明 11~12（5 档内）。
- 间距：页边 20（手机）/24（平板横屏），行高 13px padding，预设条横滑。
- 动效：enter200 / exit120 / stagger30 / press100；sheet 上滑+scrim。
- **按钮统一**：所有确定/取消双键等高 48dp、同 padding 同圆角；主页取消为通栏 danger-secondary。

## 3. 逐文件改动清单

- `res/layout/function_timer.xml`：重写——删 hero（已无）、删状态卡（已无）、删 6 预设 GridLayout→横滑 `HorizontalScrollView`+6 胶囊、开关卡→行式、自定义卡→行式、双键→通栏取消；删 `et_custom_hours/minutes`（已无）、删 `btn_refresh_status`、删 `tv_custom_summary`。
- `res/layout/dialog_custom_sheet.xml`：顶部加速捷预设行（15/25/45/60 点即填滚轮）。
- `TimerManager.java`：删刷新逻辑绑定；`updateTimerStatus` 保留每秒；取消走确认框（已有）；sheet 加快捷预设点击；确定键只显示时长（如“25分钟后”），单行不换行。
- `MainActivity.java`：`showLockTimerDialog` 八档 dialog 废弃（悬浮窗菜单跳本页代替）；`showRemainingTime` Toast 改对话框（与 B-3 一致）。
- B-4 悬浮窗/通知：不动（已实装）。

## 4. 技术差异说明

- B-2 滚轮用原生 `NumberPicker`（上下露头天然支持），与示例图一致，无需自定义 View。
- 横滑预设条用 `HorizontalScrollView`+线性胶囊，保证 48dp 高、可滚动发现（尾部露半个暗示可滑）。
- 深色：B 版浅色页按 night 映射（bg→#12161F，行→surface，字→#E6E1E5）自动走 DayNight。

## 5. 验收标准

对照 B-1~B-4 + 上表逐项打勾；按钮等高 48dp；深浅 4.5:1；pro-rules 清单过。
