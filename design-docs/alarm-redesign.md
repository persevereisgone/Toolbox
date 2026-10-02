# 闹钟 / 倒计时 C 版设计落地文档（待审核，审完再开工）

> 定稿示例图：`design-previews/alarm_c_detail_preview.html`（C 版 · 状态 hero + 底部操作区，共 10 帧）
> 双 skill：`mobile-android-design`（M3 卡/主题/48dp/深色）+ `ui-ux-pro-max`（P1~P10 + pro-rules 清单）
> 风格基线：首页 B 语言（白底白卡 16dp / 48dp 主键 / 5 档字阶 / 8dp 间距）
> 需求变更：倒计时默认单位由**秒**改为**分钟**；小组件由 1 个 2×2 改为**2 个 2×3**；"再响"文案改为"5 分钟后再响"，且必须**响铃＋震动双通道**

## 1. 功能×新入口对照表（12 项，零丢失）

| # | 功能 | 原入口 | 新入口/形态 | 说明 |
|---|---|---|---|---|
| 1 | 状态显示（剩余/无闹钟） | `tv_alarm_status` 顶部一行 | 帧1/2 顶部 hero 条 | 空闲浅底 `--:--`，运行蓝底大字 |
| 2 | 倒计时开始 | `et_alarm_seconds`＋`btn_alarm_start_countdown` | 帧1/2 「一键定时」宫格 4 键＋「更多设置」输入框＋开始键 | 单位改分钟，预设 1/5/10/30，自定义支持小数（0.5＝30 秒） |
| 3 | 指定时刻 | `tp_alarm`＋`btn_alarm_set_rtc` | 帧3 「选指定时刻…」→ bottom-sheet 双滚轮 | TimePicker spinner 移入 sheet |
| 4 | 取消闹钟 | `btn_alarm_cancel` 底部全宽 | 帧1/2 hero 条内小键 | 运行中才可用，danger 语义 |
| 5 | 精准闹钟权限引导 | `canExact()` 裸 Toast＋跳系统设置 | 帧4 bottom-sheet「需要精准闹钟权限」 | 拒绝后 hero 内嵌"去设置"提示条 |
| 6 | 空状态引导 | 无（仅一行文字） | 帧1 hero 浅底＋说明 | 新增引导，不新增功能点 |
| 7 | 响铃全屏页 | `AlarmRingActivity` 代码建视图 | 帧5 独立布局，关闭/5 分钟后再响 | 按钮层级：关闭白底主键、再响弱化 |
| 8 | 响铃通知 | `AlarmReceiver.showNotif` | 帧6 高优先级＋双动作 | 动作文案改"5 分钟后再响" |
| 9 | 响铃＋震动双通道 | 已有，但文案未强调 | 帧5/6 说明项 | 逻辑不变，增强：两通道各自独立启动与判活，任一失败重试本通道 |
| 10 | 10 秒自动关闭 | `Handler.postDelayed(10_000)` | 帧5/6 说明项 | 逻辑不变 |
| 11 | 小组件 A「定时提醒」 | `widget_alarm.xml` 2×2 单组件 | 帧7/8：2×3，时间居中，顶部蓝底身份条「定时提醒 · 到点响」＋三段式副文案「时刻＋今天/明天＋还剩」＋±15 分＋选时刻＋取消 | 新增：组件内 ±15 分微调（broadcast） |
| 12 | 小组件 B「倒计时」 | 无 | 帧9/10：2×3，时间居中，顶部深底身份条「倒计时 · 剩多久响」＋句式副文案「再过 x 响」＋±1 分＋开始/重定＋取消 | 新增：组件内自定义倒计时时长（± 步进替代输入框） |

**已确认决策**：保留两个独立小组件；± 间隔定为组件 A ±15 分 / 组件 B ±1 分。

## 1.1 两组件区分方案（三重保险，防"数字撞脸"）

问题：两组件都可能出现 `01:30`，用户无法判断是"1 小时 30 分后响"还是"凌晨 1:30 响"。三处同时区分：

| 维度 | 组件 A 定时提醒 | 组件 B 倒计时 |
|---|---|---|
| ① 顶部身份条（常驻） | 蓝底白字「**定时提醒 · 到点响**」 | 深底白字「**倒计时 · 剩多久响**」 |
| ② 数字副文案句式 | 「**今天/明天 ＋ 还剩 x**」（如"今天 · 还剩 4:32"） | 「**再过 x 响**」（如"再过 4 分 32 秒响"） |
| ③ 大字数值语义 | 时钟格式 `09:30`（中性 `widget_on_surface` 色） | 时长格式 `04:32`／`1:30:00`（`app_primary` 蓝字） |
| ④ 选择器描述文案 | "定时提醒：到某个时间点响" | "倒计时：多久之后响" |
| ⑤ 关键词落点 | 数值区永远是"何时" | 数值区永远是"多久" |

补充约束：
- **A 的数值区只放时钟时刻**（`HH:mm`，24h 制），绝不放时长；剩余量只能出现在副文案里。
- **B 的数值区只放时长**（`mm:ss` 或 `h:mm:ss`），绝不放时刻；到时时刻只能作为副文案后缀（可选，如"再过 4 分 32 秒响"）。
- 深色下身份条底色改为 `app_primary_dark` / `widget_on_surface` 反色，保证白字 4.5:1，区分逻辑不变。
- 选择器里的两个组件名与描述也承载区分，用户添加时就能看出差别。


## 2. 版式 / Token 映射

- 背景：`app_bg #FFF3F7FC`；卡片 `app_surface #FFFFFFFF`＋`app_card_stroke` 描边，圆角 `radius_lg 16dp`。
- hero 状态条：运行 `app_primary #FF427DFF` 底＋白字；空闲 `app_primary_light #FFEAF1FF` 底＋`app_primary_dark` 字。
- 字阶（5 档内）：页标题 `text_title_lg 18sp`；卡片标题 `text_body 14sp`；hero 数字 `text_hero_lg 32sp`（spinner 用 monospace）；说明 `text_caption 12sp`。
- 间距：页边 `space_lg 16dp`，卡片内 `space_lg`，元素间 `space_sm 8dp`，卡间距 `space_md 12dp`。
- 按钮：主键 `button_height_md 48dp`＋`Widget.Toolbox.Button.Primary`；次键 `Widget.Toolbox.Button.Secondary`；hero 内 ghost 白描边键 40dp；取消键 `Widget.Toolbox.Button.Danger`。宫格键 48dp 高、圆角 `radius_md 12dp`。
- 动效：`motion_enter 200` / `motion_exit 120` / `motion_stagger 30` / `motion_press_scale_duration 100`；sheet 上滑＋scrim，仅 alpha/transform。
- 深色：走 `values-night` 自动映射（`app_bg`→深、`app_surface`→#1B2538 同系、文字→反色），hero 运行态深色下用 `app_primary` 深色变体保证白字 4.5:1。
- 弹窗：统一 `AppDialogStyle`；确定键 `Widget.Toolbox.DialogButton.Positive`，取消 `Negative`，破坏性 `Danger`（禁止代码写死黑/白）。
- 小组件：**禁止 `?attr`**，只用实色 `@color/widget_*`（浅/深两套 `values-night`），2×3 尺寸 `minWidth 110dp / minHeight 180dp`、`targetCell 2×3`。

## 3. 逐文件改动清单

### 3.1 新建

- `res/layout/dialog_alarm_time_sheet.xml`：时刻 bottom-sheet — 抓手条＋标题「选择时刻」＋`TimePicker`（`timePickerMode="spinner"`，小时/分钟各自滚动）＋确定/取消双键。
- `res/layout/dialog_alarm_permission_sheet.xml`：权限 bottom-sheet — 抓手条＋标题「需要精准闹钟权限」＋说明＋「去设置」/「稍后」。
- `res/layout/activity_alarm_ring.xml`：响铃全屏页布局（替换现有代码建视图），沉浸底＋「ALARM」小字＋时刻大字＋状态＋关闭（白底主键）/「5 分钟后再响」（ghost 键）。
- `res/layout/widget_alarm_countdown.xml`：组件 B 倒计时版 2×3 — 标题行、居中数字块（`wcenter` 结构：上下 `weight` 撑开）、±1 分双键、开始/重定＋取消。
- `res/xml/alarm_countdown_widget_info.xml`：2×3 配置，`initialLayout` 指向上方布局。
- `function/alarm/AlarmCountdownWidgetProvider.java`：组件 B Provider，处理 `±1 分`、`开始`、`重定`、`取消` 四类 broadcast 与刷新。

### 3.2 重写

- `res/layout/function_alarm.xml`：删原 6 个平铺控件，改 hero 条＋「一键定时」宫格卡＋「更多设置」卡；保留 id `tv_alarm_status`；`et_alarm_seconds` 改 `et_alarm_minutes`（单位分钟）；`btn_alarm_start_countdown` 保留 id（重启为"开始"）；`btn_alarm_cancel` 移入 hero；`tp_alarm`、`btn_alarm_set_rtc` 从本布局移除（移入 sheet）。
- `res/layout/widget_alarm.xml`：改 2×3，时间块垂直水平居中，按钮改为「−15 分」「+15 分」「选时刻」「取消」。
- `res/xml/alarm_widget_info.xml`：`2×2 110×110` → `2×3 110×180`，`targetCellWidth 2 / targetCellHeight 3`，`minResizeHeight` 同步。
- `function/alarm/AlarmFunctionManager.java`：重写绑定逻辑 — 宫格 4 预设点击即定；自定义分钟解析（`Float.parseFloat`，`≤0` 回退 5 分）；两处「选时刻/开始」前统一走 `ensureExactPermission()`；权限 sheet 与时刻 sheet 的 `BottomSheetDialog` 封装；hero 刷新含剩余秒级走字与"今天/明天"文案；运行中预设键高亮、点其它预设＝取消旧闹钟重定。
- `function/alarm/AlarmRingActivity.java`：改用 `activity_alarm_ring.xml`＋`setContentView`，删代码建视图；按钮文案改「5 分钟后再响」。
- `function/alarm/AlarmReceiver.java`：`showNotif` 动作文案改「5 分钟后再响」；`startRing` 拆为 `startSound()` / `startVibrate()` 两段独立 try＋失败重试本通道，`stopRing()` 双停；`SNOOZE` 分支复用 `startRing` 保证双通道。
- `function/alarm/AlarmWidgetProvider.java`：适配 2×3 新布局 id；动作由 `ALARM_W_START10`（10 秒）改为 `ALARM_W_ADJUST`（±15 分微调，参数 `delta`）＋`ALARM_W_OPEN_SET`（跳 App 时刻 sheet）＋`ALARM_W_CANCEL`；空闲态「取消」置灰。

### 3.3 增量修改

- `function/alarm/AlarmStore.java`：新增 `getRemainingMinutes()`、`addMinutes(ctx, int delta)`（微调重定）、`KEY_PENDING_MIN`（组件 B 待定时长持久化，默认 5，范围 1 分～12 小时）、`isValidMinutes(float)`。
- `AndroidManifest.xml`：注册 `AlarmCountdownWidgetProvider` ＋ `<meta-data android:name="android.appwidget.provider" android:resource="@xml/alarm_countdown_widget_info" />`。
- `res/values/strings.xml`：新增闹钟页文案（预设、单位说明、sheet 标题、权限说明、按钮、组件文案），统一"5 分钟后再响"。
- `res/values/dimens.xml`：如 hero 大字与宫格高度无现成 token，复用 `text_hero_lg`/`button_height_md`，不新增（保持 token 收敛）。

### 3.4 明确不动

- 响铃/通知/自关的定时逻辑（exact＋allowWhileIdle、10 秒自关）。
- `widget_cookbook.xml`、`widget_todo.xml` 及待办相关文件。

## 4. 技术差异说明（示例图 → 现实）

1. **组件内放不了输入框 / TimePicker**：`RemoteViews` 不支持 `EditText`、`TimePicker`。故组件 B 的"自定义输入倒计时"落地为 **±1 分步进**（帧9 已按此绘制），组件 A 的「选时刻」跳 App bottom-sheet（帧7 已按此绘制），无虚构。
2. **组件内无法每秒刷新**：调度 `setExact(1s)` 耗电且被杀。故组件 A 显示「目标时刻＋剩余 mm:ss」快照，秒级走字由 App 内 hero 承担；组件 B 显示剩余 mm:ss 快照，并在 App 内每次状态变更时 `refreshAll`；用户手动点组件任意键也会刷新。
3. **两个组件共用一个状态源**：`AlarmStore` 单例数据（同一 `alarm_prefs`），A/B 两组件互相同步——任一组件设的闹钟，另一个也显示，避免用户误以为冲突。
4. **± 步进的边界**：组件 A 目标时刻 ±15 分后若落到过去，按 `setRtc` 同规则顺延明天；组件 B 时长钳制在 1 分～12 小时，超界 Toast。
5. **双通道不可缺失**：铃声与震动各自独立启动/判活/重试；系统静音只影响铃声通道，震动仍必须工作（验收时静音与关震各测一次）。

## 5. 验收标准

- 对照帧1～帧10 逐帧打勾；12 项功能入口全部可达，零丢失。
- 触摸 ≥48dp（组件内按钮 ≥40dp 可接受，因组件网格限制）；无文字符号图标（± 用文字标签，符合现有风格）。
- 字阶只用 5 档；间距走 dimens；动效走 integers。
- 深浅双模式各看一次：hero 白字对比 4.5:1；弹窗按钮可见（`AppDialogStyle` 系列，禁止写死黑/白）；破坏性键用 danger。
- 真机：静音下震动仍工作、关震动下铃声仍工作、10 秒自关、5 分钟后再响双通道、锁屏亮屏、组件 A/B 互相同步、App 内改动即刷组件。
