# Toolbox 设计系统指南

> 版本: 1.0  
> 日期: 2026-04-28  
> 适用范围: Toolbox Android 应用

---

## 目录

1. [颜色系统](#1-颜色系统)
2. [间距系统](#2-间距系统)
3. [尺寸系统](#3-尺寸系统)
4. [字体系统](#4-字体系统)
5. [组件样式规范](#5-组件样式规范)
6. [布局模式](#6-布局模式)
7. [深色模式适配](#7-深色模式适配)
8. [最佳实践](#8-最佳实践)
9. [资源命名规范](#9-资源命名规范)

---

## 1. 颜色系统

### 1.1 颜色分类

| 类别 | 资源名称前缀 | 说明 |
|------|-------------|------|
| 背景色 | `app_bg*` | 页面背景、卡片背景等 |
| 表面色 | `app_surface*` | 卡片、按钮等表面元素 |
| 主色调 | `app_primary*` | 品牌主色 |
| 强调色 | `app_accent*` | 辅助强调色 |
| 文本色 | `app_text_*` | 文字颜色 |
| 状态色 | `app_success/warning/danger/info` | 成功、警告、错误、信息 |
| 导航色 | `app_nav_*` | 导航组件专用 |
| 分割线 | `app_divider*` | 分割线、边框 |

### 1.2 核心颜色映射

| 资源名称 | 浅色模式 | 深色模式 | 用途 |
|---------|---------|---------|------|
| `app_bg` | #F3F7FC | #121820 | 页面背景 |
| `app_surface` | #FFFFFF | #1E2836 | 卡片表面 |
| `app_surface_alt` | #F8FBFF | #263040 | 次要表面 |
| `app_primary` | #427DFF | #5B8AFF | 主色调 |
| `app_primary_dark` | #335FD1 | #7AA2FF | 主色调深色 |
| `app_accent` | #7567FF | #9D8FFF | 强调色 |
| `app_text_primary` | #1B2538 | #E8ECF4 | 主要文字 |
| `app_text_secondary` | #67758D | #9BA8B9 | 次要文字 |
| `app_text_tertiary` | #91A0B7 | #6B7A8D | 辅助文字 |
| `app_success` | #25A06A | #3BC786 | 成功状态 |
| `app_warning` | #DC8C2F | #F0A84C | 警告状态 |
| `app_danger` | #E15A5A | #FF7878 | 错误状态 |
| `app_info` | #4B83FF | #6BA3FF | 信息状态 |

### 1.3 使用原则

- **文本颜色**: 标题和主要内容使用 `app_text_primary`，辅助信息使用 `app_text_secondary`，禁用状态使用 `app_text_disabled`
- **背景颜色**: 页面根布局使用 `app_bg`，卡片使用 `app_surface`，交替列表项使用 `app_surface_alt`
- **状态颜色**: 仅用于状态指示（如成功提示、错误消息），不用于主要 UI 元素

---

## 2. 间距系统

### 2.1 间距等级

| 资源名称 | 值 | 用途 |
|---------|-----|------|
| `space_xxs` | 4dp | 极微小间距（图标内边距） |
| `space_xs` | 6dp | 微小间距（文字与图标间距） |
| `space_sm` | 8dp | 小间距（元素内边距） |
| `space_md` | 12dp | 中等间距（组件间距） |
| `space_lg` | 16dp | 大间距（卡片间距） |
| `space_xl` | 20dp | 极大间距（页面边距） |
| `space_2xl` | 24dp | 超大间距（区块间距） |
| `space_3xl` | 32dp | 特大面积间距 |
| `space_4xl` | 40dp | 最大间距 |

### 2.2 使用原则

- **内部间距**: 组件内部元素使用 `space_sm` ~ `space_md`
- **外部间距**: 组件之间使用 `space_lg` ~ `space_xl`
- **卡片内边距**: 使用 `card_padding_sm/md/lg`

---

## 3. 尺寸系统

### 3.1 圆角等级

| 资源名称 | 值 | 用途 |
|---------|-----|------|
| `radius_xs` | 4dp | 小图标、小按钮 |
| `radius_sm` | 8dp | 按钮、输入框 |
| `radius_md` | 12dp | 卡片、弹窗 |
| `radius_lg` | 16dp | 大卡片、浮层 |
| `radius_xl` | 20dp | 对话框、大容器 |
| `radius_2xl` | 24dp | 大模态框 |
| `radius_full` | 9999dp | 胶囊形状 |

### 3.2 组件高度

| 资源名称 | 值 | 用途 |
|---------|-----|------|
| `button_height` | 44dp | 小按钮 |
| `button_height_md` | 48dp | 标准按钮 |
| `button_height_lg` | 56dp | 大按钮 |
| `button_height_xl` | 64dp | 超大按钮 |
| `input_height` | 48dp | 标准输入框 |
| `input_height_lg` | 56dp | 大输入框 |
| `toolbar_height` | 56dp | 标准工具栏 |
| `toolbar_height_lg` | 64dp | 大工具栏 |
| `bottom_nav_height` | 64dp | 底部导航栏 |

### 3.3 图标尺寸

| 资源名称 | 值 | 用途 |
|---------|-----|------|
| `icon_size_xs` | 16dp | 小图标 |
| `icon_size_sm` | 20dp | 小号图标 |
| `icon_size_md` | 24dp | 标准图标 |
| `icon_size_lg` | 32dp | 大图标 |
| `icon_size_xl` | 40dp | 超大图标 |

### 3.4 阴影高度

| 资源名称 | 值 | 用途 |
|---------|-----|------|
| `elevation_none` | 0dp | 无阴影 |
| `elevation_low` | 2dp | 低阴影（卡片） |
| `elevation_md` | 4dp | 中阴影（浮层） |
| `elevation_lg` | 6dp | 高阴影（弹窗） |
| `elevation_high` | 8dp | 极高阴影 |
| `elevation_xl` | 12dp | 最高阴影（悬浮面板） |

---

## 4. 字体系统

### 4.1 字体大小等级

| 资源名称 | 值 | 样式名称 | 用途 |
|---------|-----|---------|------|
| `text_caption` | 12sp | `TextAppearance.Toolbox.Caption` | 辅助说明文字 |
| `text_caption_lg` | 13sp | - | 稍大的辅助文字 |
| `text_body` | 14sp | `TextAppearance.Toolbox.Body` | 正文文字 |
| `text_body_lg` | 15sp | `TextAppearance.Toolbox.BodyLarge` | 大正文 |
| `text_title` | 16sp | `TextAppearance.Toolbox.Title` | 标题 |
| `text_title_lg` | 18sp | `TextAppearance.Toolbox.TitleLarge` | 大标题 |
| `text_heading` | 20sp | `TextAppearance.Toolbox.Heading` | 页面标题 |
| `text_heading_lg` | 24sp | `TextAppearance.Toolbox.HeadingLarge` | 大页面标题 |
| `text_hero` | 28sp | `TextAppearance.Toolbox.HeroTitle` | Hero 标题 |
| `text_hero_lg` | 32sp | - | 超大标题 |

### 4.2 使用原则

- **页面标题**: 使用 `Heading` 样式（20sp）
- **卡片标题**: 使用 `Title` 样式（16sp），加粗
- **正文内容**: 使用 `Body` 样式（14sp）
- **辅助说明**: 使用 `Caption` 样式（12sp），灰色
- **按钮文字**: 使用 `TextAppearance.Toolbox.Button`（14sp），加粗

---

## 5. 组件样式规范

### 5.1 Button 按钮

| 样式名称 | 用途 | 特点 |
|---------|------|------|
| `Widget.Toolbox.Button.Primary` | 主要操作 | 主色调背景、白色文字 |
| `Widget.Toolbox.Button.Primary.Large` | 大尺寸主按钮 | 更大的点击区域 |
| `Widget.Toolbox.Button.Primary.Small` | 小尺寸主按钮 | 紧凑布局 |
| `Widget.Toolbox.Button.Secondary` | 次要操作 | 透明背景、边框、主文字色 |
| `Widget.Toolbox.Button.Secondary.Large` | 大尺寸次按钮 | 更大的点击区域 |
| `Widget.Toolbox.Button.Danger` | 危险操作 | 透明背景、红色文字 |
| `Widget.Toolbox.Button.Text` | 文字按钮 | 纯文字、无背景 |

### 5.2 Input 输入框

| 样式名称 | 用途 |
|---------|------|
| `Widget.Toolbox.Input` | 标准输入框 |
| `Widget.Toolbox.Input.Large` | 大输入框 |

### 5.3 Switch/Checkbox/RadioButton

| 样式名称 | 用途 |
|---------|------|
| `Widget.Toolbox.Switch` | 开关控件 |
| `Widget.Toolbox.Checkbox` | 复选框 |
| `Widget.Toolbox.RadioButton` | 单选按钮 |

### 5.4 Card 卡片

| 样式名称 | 用途 |
|---------|------|
| `Widget.Toolbox.CardContainer` | 标准卡片容器 |
| `Widget.Toolbox.CardContainer.Small` | 小卡片 |
| `Widget.Toolbox.CardContainer.Large` | 大卡片 |
| `Widget.Toolbox.SurfaceAlt` | 备用表面容器 |

### 5.5 Toolbar 工具栏

| 样式名称 | 用途 |
|---------|------|
| `Widget.Toolbox.Toolbar` | 标准工具栏 |
| `Widget.Toolbox.Toolbar.Large` | 大工具栏 |

---

## 6. 布局模式

### 6.1 页面布局结构

```xml
<ScrollView
    android:background="@color/app_bg"
    android:padding="@dimen/space_xl">
    
    <!-- 头部区域 -->
    <LinearLayout
        android:background="@drawable/app_hero_bg"
        android:padding="@dimen/space_lg">
        <!-- Hero 内容 -->
    </LinearLayout>
    
    <!-- 卡片区域 -->
    <LinearLayout
        style="@style/Widget.Toolbox.CardContainer"
        android:layout_marginTop="@dimen/space_lg">
        <!-- 卡片内容 -->
    </LinearLayout>
    
    <!-- 按钮区域 -->
    <LinearLayout
        android:layout_marginTop="@dimen/space_lg">
        <Button style="@style/Widget.Toolbox.Button.Primary" />
    </LinearLayout>
</ScrollView>
```

### 6.2 卡片布局模式

```xml
<LinearLayout
    style="@style/Widget.Toolbox.CardContainer"
    android:orientation="vertical">
    
    <!-- 卡片标题 -->
    <TextView
        style="@style/TextAppearance.Toolbox.Title"
        android:text="标题" />
    
    <!-- 卡片内容 -->
    <TextView
        style="@style/TextAppearance.Toolbox.Body.Secondary"
        android:layout_marginTop="@dimen/space_sm"
        android:text="内容" />
    
    <!-- 操作按钮 -->
    <Button
        style="@style/Widget.Toolbox.Button.Primary"
        android:layout_marginTop="@dimen/space_lg"
        android:text="操作" />
</LinearLayout>
```

### 6.3 列表项布局模式

```xml
<LinearLayout
    android:background="@drawable/app_card_bg"
    android:padding="@dimen/space_lg">
    
    <!-- 左侧内容 -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_weight="1">
        <TextView style="@style/TextAppearance.Toolbox.Title" />
        <TextView style="@style/TextAppearance.Toolbox.Caption" />
    </LinearLayout>
    
    <!-- 右侧操作 -->
    <LinearLayout>
        <Button style="@style/Widget.Toolbox.Button.Secondary.Small" />
    </LinearLayout>
</LinearLayout>
```

---

## 7. 深色模式适配

### 7.1 适配原则

1. **自动跟随系统**: 使用 `Theme.MaterialComponents.DayNight` 主题
2. **颜色资源覆盖**: 在 `values-night/colors.xml` 中定义深色模式颜色
3. **Drawable 适配**: 必要时在 `drawable-night/` 中创建深色版本
4. **对比度保证**: 确保文字与背景对比度 ≥ 4.5:1

### 7.2 目录结构

```
res/
├── values/
│   ├── colors.xml          # 浅色模式颜色
│   ├── styles.xml          # 通用样式
│   └── themes.xml          # 浅色模式主题
├── values-night/
│   ├── colors.xml          # 深色模式颜色
│   └── themes.xml          # 深色模式主题
├── drawable/               # 通用 drawable
└── drawable-night/         # 深色模式专用 drawable（如需要）
```

### 7.3 颜色适配示例

```xml
<!-- values-night/colors.xml -->
<resources>
    <color name="app_bg">#121820</color>
    <color name="app_surface">#1E2836</color>
    <color name="app_text_primary">#E8ECF4</color>
    <color name="app_text_secondary">#9BA8B9</color>
    <color name="app_primary">#5B8AFF</color>
    <!-- ... 其他颜色 -->
</resources>
```

---

## 8. 最佳实践

### 8.1 资源引用规范

- ✅ **推荐**: 使用资源引用 `@color/app_text_primary`
- ❌ **避免**: 硬编码颜色值 `#FF1B2538`

### 8.2 布局规范

- 使用 `match_parent` 和 `wrap_content`，避免硬编码尺寸
- 使用 `layout_weight` 实现弹性布局
- 优先使用 `ConstraintLayout` 或 `LinearLayout`
- 保持布局层级简洁，避免过深嵌套

### 8.3 样式规范

- 使用 `style` 属性引用预定义样式
- 避免在布局文件中重复定义样式属性
- 将可复用样式提取到 `styles.xml`

### 8.4 命名规范

- **颜色**: `app_<category>_<variant>` 如 `app_text_primary`
- **尺寸**: `<type>_<size>` 如 `space_lg`, `radius_md`
- **样式**: `TextAppearance.Toolbox.<Name>` 或 `Widget.Toolbox.<Component>.<Variant>`
- **布局**: `function_<feature>.xml` 如 `function_timer.xml`

### 8.5 性能优化

- 使用 `merge` 标签减少视图层级
- 使用 `ViewStub` 延迟加载非关键视图
- 避免过度使用 `wrap_content` 导致多次测量

---

## 9. 资源命名规范

### 9.1 颜色命名

```
app_<category>_<variant>
```

示例:
- `app_bg` - 主背景
- `app_text_primary` - 主文本
- `app_surface_alt` - 备用表面
- `app_primary_dark` - 主色调深色

### 9.2 尺寸命名

```
<type>_<size>
```

示例:
- `space_lg` - 大间距
- `radius_md` - 中等圆角
- `button_height` - 按钮高度

### 9.3 样式命名

```
<Type>.<AppName>.<Component>.<Variant>
```

示例:
- `TextAppearance.Toolbox.Title` - 标题文字样式
- `Widget.Toolbox.Button.Primary` - 主按钮样式

### 9.4 布局文件命名

```
<type>_<name>.xml
```

示例:
- `function_timer.xml` - 功能页面
- `item_lottery.xml` - 列表项
- `dialog_edit.xml` - 对话框
- `overlay_menu.xml` - 悬浮层

---

## 更新记录

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0 | 2026-04-28 | 初始版本，包含颜色、间距、字体、组件规范 |

---

*本文档应随项目演进定期更新*

