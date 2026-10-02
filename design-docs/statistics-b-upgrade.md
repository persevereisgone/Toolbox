# 统计 B 版升级落地文档（待审核，审完再开工）

> 定稿：`statistics_redesign_preview.html` S-1~S-16（用户已确认全部帧）。
> 双 skill：mobile-android-design + ui-ux-pro-max。沿用定时 B 版语言（白底/行式/48dp/5 档字阶）。

## 1. 技术可行性识别（逐项可做，无虚构）

| 帧 | 内容 | 实现方式 | 风险 |
|---|---|---|---|
| S-1 主页 | 圆角页容器+分区+横滑？不，主页列表复用现有 RecyclerView，加组卡/新增卡/总计/危险区/导入导出/全屏键 | `function_statistics.xml` 重排，id 全保留 | 无 |
| S-1 行 | 名称 flex 大头+单行省略，行内整体等比缩小、正方形按钮保持 | `item_statistics.xml`：名称 `0dp+weight`，−/＋/× 40dp 正方形（用户为省空间明确确定，触摸 48dp 项记例外），行高 56dp，间距 6/4 两档 | 行高压缩，长名展示优先 |
| S-2 全屏 | 现 `dialog_statistics_fullscreen` 改样式：搜索+排序+总计+关闭 | 逻辑不动，只换布局边距/字阶 | 无 |
| S-3/S-4/S-6 | 确认框走 `AppDialogStyle` | 已切 style；S-6 行删除确认若现状无确认则新增 | S-6 需确认现状：有则保留，无则加 |
| S-5 改数 | `dialog_edit_quantity` 样式统一 | 布局微调 | 无 |
| S-7~S-11 | 导出/导入链：二选一→文件/文本→报错清单 | 全部已有 dialog（`dialog_import_export/import_data/import_errors`），只统一按钮色（已切 AppDialogStyle）+ 必查项验收 | 无 |
| S-12~S-15 | 组管理链：列表/新建/重命名/删组 | 已有（`showManageSetsDialog` 等），统一样式 | 无 |
| S-16 空状态 | 虚线框+保留入口 | 新增空态 view，`items.isEmpty()` 时显示 | 小改 adapter/Manager |
| 通用 | 按钮 flex 行定宽规则 | XML：行内按钮 `wrap_content+minWidth`，禁用 `match_parent` 进 flex 行 | 写入规范 |

## 2. 功能×新入口对照（现状 13 类表面 → 新位置）

组切换/管理→组卡；新增→新增卡；列表增删改→列表行（48dp）；总计→chip；批量删/清空→危险区+确认框；导出/导入→工具行+原链路；全屏→通栏键+原框；空态→S-16。零丢失。

## 3. 逐文件改动清单

- `res/layout/function_statistics.xml`：重写——圆角页容器（复用 `function_page_bg`）、组卡、新增卡（名称 weight 2.4 / 数量 1 / 添加固定宽）、列表、总计 chip、危险区、工具行、全屏键、空态 view。
- `res/layout/item_statistics.xml`：名称 `0dp+weight+单行省略`，−/＋/× 改 48dp（触摸必查项）。
- `res/layout/dialog_statistics_fullscreen.xml`：统一样式（搜索/排序/关闭排布按 S-2）。
- `StatisticsManager.java`：空态显示/隐藏；S-6 若无确认则加；其余逻辑不动。
- dialog 系列布局：按钮色已随 AppDialogStyle 生效，只做必查项验收，不改逻辑。

## 4. 通用规则沉淀（写入两条 rules 验收）

- 内容优先于铬：输入/列表拿 flex 大头，按钮固定宽；装饰间距只取 8/12 两档。
- flex 行按钮必须显式定宽（`wrap_content+minWidth`），禁用 `match_parent/width:100%` 进 flex 行。
- flex 中间列必须 `flex:1+min-width:0`（`0dp+weight`），文字单行省略，防挤成竖排。

## 6. 冗余重构记录（本次一并落地）

- **多选入口统一**：删除旧"批量删除多选弹窗"，`批量删除`按钮改为直接进入行内多选（与长按多选同一套）；`一键清空`改用同一 danger 确认框。
- **导出归并**：导出选项 3→2（CSV 文件/复制文本），"分享"只在"导出成功"框里；CSV 拼装抽为 `buildCsvBody`+`writeTextFile` 单一份，删 `shareData`。
- **持久化收敛**：删 `saveStatisticsSets/saveCurrentSet/saveCurrentSetData/loadData`，统一 `saveData()`+`persist()` 一存一读；删未用键 `statistics_data`。
- **对话框统一**：`DialogUtils` 全部走 `AppDialogStyle`（含新增 `AppDialogStyle.Danger` 危险确认），删所有硬编码 holo/黑按钮色补丁；`StatisticsManager` 剩余裸 dialog 全部接 `AppDialogStyle`。- **全屏列表复用同一 Adapter**：可改数/可删除/同布局，删 `ArrayAdapter` 只读字符串写法；排序记忆进 prefs。

## 8. F 定稿实装记录（展开式 FAB：stats_F_final.html 10 帧）

- **全屏入口删除**：主页全量展示+页内滚动，`maxVisible` 恢复不限；`showFullscreenListDialog` 标 `@Deprecated` 停用（下次清理可删）；搜索+排序上主页（`et_search`+`spinner_sort`，排序记忆复用 `list_sort` 键）。
- **adapter 显示映射**：新增 `visibleIndex`（数据下标映射），回调永远给数据下标；`displayPositionOf` 供新增后滚动；`setVisibleIndex` 未调用=全显。
- **组 pills**：`refreshSetPills` 代码建横滑 pills+末尾＋进组管理（`pill_on/off_bg`），切组/新建/重命名/删组后刷新；Spinner 相关 id 与字段删除。
- **速拨 FAB**：`fab_main`（单＋）→`layout_speed_dial` 向左展开新增/导入/导出（mini FAB+文字标），scrim 点透收回，返回键先收速拨再退多选；多选态隐藏 FAB。
- **底弹链**：新增走 `sheet_add_item.xml` 的 `BottomSheetDialog`，成功滚到新行（显示下标）；导入/导出方式二选一、粘贴导入、导出成功分享沿用原 dialog（已验证按钮可见）。
- 建议 ①~⑥ 全做：搜索排序上主页、多选藏 FAB、新增滚到新行、空白/返回收速拨、空态 FAB 脉冲提示、pills 末尾＋进组管理。

## 7. 验收标准

S-1~S-16 逐项打勾；−/＋/× 及所有键 48dp；对话框按钮深浅可见；深浅 4.5:1；pro-rules 过。
