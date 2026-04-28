# 深色模式适配计划

## 目标

为 Toolbox 应用建立完整的深色模式（Dark Mode）支持，实现跟随系统自动切换深/浅主题。

## 当前状态分析

### 已有基础
- 项目已使用 `Theme.MaterialComponents.DayNight.NoActionBar` 作为主题
- `values-night/themes.xml` 已存在，但配置不完整
- 所有颜色资源使用 color 定义，便于深色模式适配

### 存在问题
1. `values-night/themes.xml` 未使用项目自定义颜色变量
2. 缺少 `values-night/colors.xml` 深色模式专用颜色
3. 缺少深色模式专用的 drawable 资源
4. 部分硬编码颜色需要替换为颜色资源引用

---

## 阶段规划

### 阶段 1：深色模式颜色系统建立
- **状态：** pending
- **任务：**
  - 创建 `values-night/colors.xml` 深色模式专用颜色文件
  - 定义深色模式下的背景、表面、文本色
  - 定义深色模式下的组件颜色（按钮、输入框等）
  - 确保所有状态色（success/warning/danger/info）可读性

### 阶段 2：深色模式主题配置完善
- **状态：** pending
- **任务：**
  - 更新 `values-night/themes.xml` 使用项目颜色变量
  - 配置深色模式下的状态栏和导航栏颜色
  - 确保 Dialog、Popup 等组件在深色模式下一致性

### 阶段 3：深色模式 Drawable 资源
- **状态：** pending
- **任务：**
  - 创建 `drawable-night/` 目录结构
  - 为需要深色版本的核心 drawable 创建深色变体
  - 重点处理：卡片背景、输入框背景、按钮样式等

### 阶段 4：布局文件颜色引用审查
- **状态：** pending
- **任务：**
  - 检查所有布局文件中的硬编码颜色
  - 将硬编码颜色替换为颜色资源引用
  - 确保深色模式下布局显示正确

### 阶段 5：测试与验证
- **状态：** pending
- **任务：**
  - 运行 lint 检查确保无资源引用错误
  - 测试深色模式切换是否正常
  - 验证所有功能页面在深色模式下的显示效果

---

## 深色模式配色方案

### 背景与表面
| 颜色名称 | 浅色模式 | 深色模式 | 说明 |
|---------|---------|---------|------|
| app_bg | #F3F7FC | #121820 | 页面背景 |
| app_surface | #FFFFFF | #1E2836 | 卡片/表面背景 |
| app_surface_alt | #F8FBFF | #263040 | 备选表面 |
| app_surface_soft | #F0F5FF | #2E3A4A | 软色表面 |
| app_surface_dark | #1B2538 | #E0E0E0 | 深色文字表面 |

### 文本颜色
| 颜色名称 | 浅色模式 | 深色模式 | 说明 |
|---------|---------|---------|------|
| app_text_primary | #1B2538 | #E8ECF4 | 主要文字 |
| app_text_secondary | #67758D | #9BA8B9 | 次要文字 |
| app_text_tertiary | #91A0B7 | #6B7A8D | 辅助文字 |
| app_text_disabled | #C4CAD4 | #4A5568 | 禁用文字 |
| app_text_inverse | #FFFFFF | #1B2538 | 反色文字 |

### 主题色
| 颜色名称 | 浅色模式 | 深色模式 | 说明 |
|---------|---------|---------|------|
| app_primary | #427DFF | #5B8AFF | 主色调 |
| app_primary_dark | #335FD1 | #7AA2FF | 主色调深色 |
| app_accent | #7567FF | #9D8FFF | 强调色 |

### 边框与分割线
| 颜色名称 | 浅色模式 | 深色模式 | 说明 |
|---------|---------|---------|------|
| app_card_stroke | #16687DA8 | #334D6E | 卡片边框 |
| app_divider | #1F6C7E99 | #334D6E | 分割线 |

### 特殊背景
| 颜色名称 | 浅色模式 | 深色模式 | 说明 |
|---------|---------|---------|------|
| app_overlay_bg | #F7F9FCFF | #2E3A4AFF | 悬浮层背景 |
| app_overlay_dark | #E7333D55 | #B3BEC9CC | 暗色悬浮层 |
| app_nav_bg | #FFFFFF | #1E2836 | 导航栏背景 |

---

## 需要创建的文件清单

### 1. values-night/colors.xml
深色模式专用颜色定义

### 2. values-night/themes.xml
更新使用项目颜色变量的主题配置

### 3. drawable-night/ (目录)
可选：需要为特定 drawable 创建深色版本时使用

---

## 关键注意事项

1. **使用 DayNight 主题**：继续使用 `Theme.MaterialComponents.DayNight` 确保自动跟随系统
2. **优先使用颜色资源**：避免硬编码颜色，确保深浅主题一致性
3. **测试真实设备**：模拟器深色模式与真实设备可能有差异
4. **关注可读性对比度**：深色模式下确保文字与背景对比度 ≥ 4.5:1
5. **保持品牌一致性**：深色模式应保持应用的品牌识别度

---

## 预期成果

- 深色模式与浅色模式无缝切换
- 所有 UI 元素在两种模式下显示正确
- 无资源引用错误
- 保持良好的可访问性对比度