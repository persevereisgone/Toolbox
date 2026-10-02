# 待办页升级方案（落盘 · B 版 · A+B+C/左滑删除）

> 预览：`todo_redesign_preview.html`（帧1 主列表 / 帧2 左滑删除+撤销 / 帧3 空状态 / 帧4 编辑 bottom-sheet）。
> 风格：B · 浅色效率版，Token 沿用首页 B 版。Widget 只读展示不变。

## 1. 功能—索引对照（有无缺口）

| # | 功能 | 现状 | 本轮 |
|---|------|------|------|
| 1 | 新增待办（输入+添加按钮，空校验 Toast） | 有 | 不变 |
| 2 | 勾选完成/取消完成（自动沉底） | 有 | 不变（沉底逻辑保留） |
| 3 | 单条删除 | **无**（只能清已完成） | **新增：左滑删除 + Snackbar 撤销** |
| 4 | 编辑标题 | 有（AlertDialog+裸 EditText，不合规） | 改为 bottom-sheet（抓手+输入+取消/保存），复用 `dialog_input_common` 形态 |
| 5 | 清除已完成 + 撤销 | 有（Snackbar 恢复快照） | 不变；按钮文案带计数 `清除已完成（N）`，N=0 时置灰不可点 |
| 6 | 计数副标题 | 无（副标题写死） | 新增：`N 项未完成` / 空态 `暂无待办` |
| 7 | 筛选 chips | 无 | 新增：全部/未完成/已完成，带计数 |
| 8 | 空状态页 | 有 | 文案微调 + 清除按钮联动置灰 |
| 9 | 到期提醒/优先级/排序拖拽 | 无 | 留到下轮（D，已告知） |
| 10 | 桌面小组件 | 有（只读+刷新） | 不变，排序变化自动跟随 |

## 2. 版式结构（自上而下）

1. 标题 `待办`（TitleLarge 22/800）+ 副标题 Caption（`N 项未完成 · 已完成沉底`）。
2. 输入行：输入框（`Widget.Toolbox.Input`，hint `新待办…`）+ 添加按钮（Primary，72dp+宽，48dp 高）。
3. 筛选 chips：`全部 N / 未完成 N / 已完成 N`，选中黑底 pill，未选中白底描边。
4. 列表行（复用卡片行）：24dp 圆角 CheckBox + 标题 14sp bold（完成态 primary 灰 + 删除线）+ 创建时间 Caption + 右侧 `编辑` 文字按钮；整行 minHeight 64dp。
5. 左滑行：右滑/左滑露出红色 `#DC2626` 删除底（宽 72dp，白字 `删除`），松手即删 + Snackbar `已删除“XXX” [撤销]`。
6. 底部：`清除已完成（N）` Secondary 危险字按钮。
7. 空态：标题+说明居中（已有），清除按钮置灰。

## 3. Token 映射

- 背景 `app_bg #F3F7FC`，行卡白 `#FFFFFF` + 描边 `#E7EDF6` 1dp，圆角 14dp。
- 主色 `app_primary #2563EB`（添加/编辑/输入聚焦），危险 `#DC2626`，完成绿 `#16A34A`，字 tertiary `#8A97AB`。
- 字阶 5 档：TitleLarge 22 / TitleMedium 16b / Body 14 / Caption 12 / Label 11；间距 8dp 节奏。
- 动效：enter 200 / stagger 30（封顶 5 项）/ press 100 / swipe 跟手 + 松手 150ms 回弹或滑出；仅 transform+alpha。

## 4. 无障碍/深色

- 整行 ≥48dp；CheckBox `contentDescription=标记完成+标题`；删除按钮/左滑操作播报完整短语；chips 用 `android:checkable` 语义。
- 深色映射：bg→#12161F，卡→#1B2538，标题→#E6E1E5，正文→#B8C4D4，描边→#2A3648；红色删除底深色下 `#F87171` 底 + `#450A0A` 字，保证 4.5:1。

## 5. 落地清单

- `function_todo.xml`：标题区加 `tv_todo_summary`；输入行不变；`RecyclerView` 上加 chips 行（`chip_todo_all/undone/done` 或单 `ChipGroup`）；清除按钮 id 不变，文案代码拼计数。
- `item_todo.xml`：CheckBox 换 24dp 圆角样式（drawable 新增 `todo_checkbox_bg` selector）；标题拆 `tv_todo_title` + `tv_todo_meta`；编辑改文字按钮；整行套 `SwipeReveal` 结构——无第三方库，手写 `ItemTouchHelper.SimpleCallback(0, LEFT)`，前景行 + 红色背景。
- 新增 `dialog_edit_todo.xml`（bottom-sheet：抓手+标题+输入+双按钮），或复用 `dialog_input_common`（若字段吻合则复用不新建）。
- `TodoFunctionManager.java`：加 `filterMode`（0/1/2）+ `reload()` 按模式过滤；`TodoManager` 加 `deleteItem(ctx,id)` 返回被删项供撤销；左滑回调删 + Snackbar 撤销；编辑入口改 bottom-sheet；`summary/chips` 计数刷新；`getTopUndone` 不变（首页副标题/小组件仍取未完成前 N）。
- `TodoItem.java`：不动（`createAt/doneAt` 已够显示 `今天/昨天/N天前`）。
- 不碰：`TodoWidgetProvider/Service`、`HomeFragment` 副标题逻辑。

## 6. 验收

- 有/无数据、左滑删+撤销、编辑保存/空校验、清除已完成+撤销、三种筛选+计数、旋转/返回不丢、深浅各看一次按钮可见性（`AppDialogStyle` 走查）。
