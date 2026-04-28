---
name: ui-test-flow
description: UI界面测试流程 - 用于测试Android应用的UI可见性、对话框按钮颜色、深浅模式适配等问题。当用户说"UI界面测试"或"运行UI测试"时触发此技能，执行完整的UI测试流程，包括模块中所有对话框的可见性测试。
---

# UI界面测试流程

## 目标
为Toolbox项目提供稳定、可复现的UI测试流程，优先发现"用户看不见/看不清/点不到"的UI问题，特别关注深色模式下的按钮可见性问题。**覆盖模块中所有对话框**，而不仅仅是固定位置的对话框。

## 触发条件
当用户说以下短语时触发：
- "UI界面测试"
- "运行UI测试"
- "执行UI界面测试"
- "测试UI显示"
- "测试所有对话框"

## 测试范围

### 1. 全模块对话框按钮可见性测试
**覆盖应用中所有对话框**，包括但不限于：
- 设置页面对话框：用户名编辑、简介编辑、头像设置
- 确认对话框：删除确认、操作确认、权限确认
- 提示对话框：信息提示、警告提示、错误提示
- 输入对话框：各种表单输入对话框
- 其他自定义对话框

### 2. 深色模式专项测试
- 所有页面深色模式显示
- 底部导航栏深色模式显示
- **所有对话框深色模式显示**

### 3. 按钮颜色验证
- 正向按钮（确定/保存/确认）使用主色调 (app_primary)
- 负向按钮（取消/关闭）使用次要文本色 (app_text_secondary)
- 按钮文字非白色（防止白底白字问题）
- 文字与背景对比度 ≥ 4.5:1

### 4. 边框视觉验证
- 边框宽度使用预定义尺寸（1dp/2dp/4dp）
- 边框颜色使用预定义颜色资源（border_primary/border_secondary等）
- 圆角半径使用预定义尺寸（8dp/12dp/16dp/20dp/24dp）
- 边框与背景对比度 ≥ 3:1（WCAG AA标准）

## 测试流程

### 步骤1：自动扫描所有对话框
```bash
# 扫描代码中所有AlertDialog.Builder调用
grep -r "AlertDialog.Builder" app/src/main/java/ --include="*.java"

# 扫描所有布局文件
find app/src/main/res/layout -name "*.xml" -type f
```

### 步骤2：识别对话框类型
根据扫描结果，识别以下类型对话框：
- 带有BUTTON_POSITIVE的对话框
- 带有BUTTON_NEGATIVE的对话框
- 带有BUTTON_NEUTRAL的对话框
- 自定义对话框布局

### 步骤3：边框自动扫描
```bash
# 扫描所有drawable资源文件中的边框定义
find app/src/main/res/drawable -name "*.xml" -type f | xargs grep -l "stroke\|corner"

# 使用Python脚本分析边框属性
python scripts/border_scanner.py app/src/main/res/drawable/
```

### 步骤4：执行Espresso UI测试
```bash
# 运行所有对话框相关测试
./gradlew :app:connectedDebugAndroidTest --tests "*Dialog*"

# 运行单元测试回归
./gradlew :app:testDebugUnitTest
```

### 步骤5：运行时边框检测
在应用运行时自动扫描所有组件的边框属性：
```java
// 在Activity中调用边框扫描
List<BorderScanner.BorderInfo> borderResults = BorderScanner.scanViewHierarchy(findViewById(android.R.id.content));

// 输出扫描结果
for (BorderScanner.BorderInfo info : borderResults) {
    Log.d("BorderScan", String.format(
        "View: %s, Width: %.1fpx, Radius: %.1fpx, Compliant: %s",
        info.viewPath, info.borderWidth, info.cornerRadius, info.isFullyCompliant()
    ));
}
```

### 步骤6：问题检测与报告
检查以下问题类型：
- UI可见性问题：按钮/文本存在但不可见
- 颜色对比度问题：白底白字或对比度不足
- 主题适配问题：深色/浅色模式切换异常
- 对话框按钮缺失：未设置显式颜色
- 边框视觉问题：边框宽度/颜色/圆角不符合设计规范

### 步骤7：边框视觉检测
检查以下边框相关问题：
- 边框宽度不一致：未使用预定义的border_width_*尺寸
- 边框颜色错误：未使用预定义的border_*颜色资源
- 圆角半径不一致：未使用预定义的radius_*尺寸
- 对比度不足：边框与背景对比度 < 3:1

## 对话框扫描与测试策略

### 扫描规则
1. **代码扫描**：查找所有`AlertDialog.Builder`、`AlertDialog.create()`调用
2. **布局扫描**：查找所有自定义对话框布局文件
3. **主题检查**：检查对话框使用的主题配置
4. **边框扫描**：自动扫描所有带有背景Drawable的组件，检测边框宽度、颜色、圆角

### 测试策略
1. **遍历所有功能入口**：逐个点击触发所有可能弹出对话框的操作
2. **验证每个对话框**：检查按钮存在性、可见性、颜色
3. **深浅模式双验证**：在两种主题模式下都进行测试
4. **边框自动检测**：自动扫描所有组件的边框属性，验证是否符合设计规范

### 边框自动扫描流程

#### 步骤1：扫描布局文件
```bash
# 扫描所有布局文件中的背景资源引用
grep -r "android:background" app/src/main/res/layout/ --include="*.xml" | grep -E "(drawable|shape)"

# 扫描所有drawable资源文件
find app/src/main/res/drawable -name "*.xml" -type f | xargs grep -l "stroke\|corner"
```

#### 步骤2：识别边框属性
扫描结果中识别以下属性：
- `stroke` - 边框宽度和颜色
- `corners` - 圆角半径
- `solid` - 背景颜色（用于对比度计算）

#### 步骤3：自动化验证
```bash
# 使用lint检查资源一致性
./gradlew lintDebug --checks "ResourceType"

# 使用自定义工具扫描drawable文件
python scripts/border_scanner.py app/src/main/res/drawable/
```

#### 步骤4：生成边框检测报告
自动生成包含以下信息的报告：
- 组件名称和位置
- 当前边框宽度/颜色/圆角
- 是否符合设计规范
- 对比度检测结果

## 测试用例模板

### 通用对话框按钮可见性测试模板
```markdown
### 用例: [对话框名称]按钮可见性
- 场景: 触发对话框显示
- 前置: [必要的前置条件]
- 步骤:
  1) 进入目标页面
  2) 执行触发对话框的操作
- 预期:
  - 对话框成功显示
  - 所有按钮存在且可见
  - 按钮文字非空
  - 按钮文本颜色符合设计规范（非白色）
  - 文字与背景对比度 ≥ 4.5:1
```

### 深色模式对话框测试模板
```markdown
### 用例: [对话框名称]深色模式显示
- 场景: 深色模式下触发对话框显示
- 前置: 系统主题为深色模式
- 步骤:
  1) 确保系统处于深色模式
  2) 进入目标页面
  3) 执行触发对话框的操作
- 预期:
  - 对话框背景颜色正确切换为深色模式配色
  - 所有按钮存在且可见
  - 按钮文字清晰可见（非白色）
  - 文字与背景对比度 ≥ 4.5:1
```

### 边框视觉测试模板
```markdown
### 用例: [组件名称]边框视觉测试
- 场景: 检查组件边框是否符合设计规范
- 前置: 组件已加载完成
- 步骤:
  1) 获取组件的背景Drawable
  2) 验证边框宽度是否为预期值
  3) 验证边框颜色是否为预期值
  4) 验证圆角半径是否为预期值
  5) 验证边框与背景对比度 ≥ 3:1
- 预期:
  - 边框宽度 = [预期宽度，如1dp]
  - 边框颜色 = [预期颜色资源，如border_primary]
  - 圆角半径 = [预期半径，如24dp]
  - 对比度 ≥ 3:1（WCAG AA标准）
```

## 当前模块对话框清单

| 对话框名称 | 位置 | 按钮类型 | 边框宽度 | 边框颜色 | 圆角半径 | 状态 |
|-----------|------|---------|---------|---------|---------|------|
| 用户名编辑对话框 | SettingsFragment | 保存/取消 | 1dp | border_primary | 24dp | ✅ 已测试 |
| 简介编辑对话框 | SettingsFragment | 保存/取消 | 1dp | border_primary | 24dp | ✅ 已测试 |
| 头像设置对话框 | SettingsFragment | 确定 | 1dp | border_primary | 16dp | ✅ 已测试 |
| 数据备份对话框 | SettingsFragment | 确定/取消 | - | - | - | ⏳ 待实现 |
| 数据恢复对话框 | SettingsFragment | 确定/取消 | - | - | - | ⏳ 待实现 |
| 清除缓存对话框 | SettingsFragment | 确定/取消 | - | - | - | ⏳ 待实现 |
| 通用输入对话框 | DialogUtil | 确定/取消 | 1dp | border_primary | 24dp | ✅ 已测试 |

## 问题修复指南

### 对话框按钮不可见（白底白字）
**问题原因**：AlertDialog在某些主题下按钮文字默认为白色，与白色背景冲突

**修复方案**：在对话框显示后显式设置按钮颜色
```java
dialog.setOnShowListener(dialogInterface -> {
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        .setTextColor(ContextCompat.getColor(requireContext(), R.color.app_primary));
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        .setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_secondary));
    if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_secondary));
    }
});
```

### 批量修复脚本
对于多个对话框，可以抽取为工具方法：
```java
public static void setupDialogButtonColors(AlertDialog dialog, Context context) {
    dialog.setOnShowListener(dialogInterface -> {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.app_primary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(context, R.color.app_text_secondary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setTextColor(ContextCompat.getColor(context, R.color.app_text_secondary));
        }
    });
}
```

## 输出格式

### 测试结果报告
```markdown
## UI界面测试报告

### 执行时间
[时间戳]

### 扫描结果
- 发现对话框数量: X 个
- 已测试对话框: Y 个
- 待测试对话框: Z 个

### 测试执行结果
- 通过: X 个测试
- 失败: Y 个测试

### 失败测试详情

#### 1. [测试用例名称]
- **测试类型**: 按钮颜色测试/边框视觉测试/对比度测试
- **失败位置**: 文件路径:行号
- **涉及组件**: 对话框名称/组件ID
- **预期结果**: 详细描述期望的行为或属性值
- **实际结果**: 详细描述实际发生的行为或属性值
- **问题类型**: 按钮不可见/边框颜色错误/圆角不一致/对比度不足
- **根因分析**: 简要分析问题产生的原因
- **修复建议**: 具体的修复方案或代码修改建议

#### 2. [测试用例名称]
- **测试类型**: 按钮颜色测试/边框视觉测试/对比度测试
- **失败位置**: 文件路径:行号
- **涉及组件**: 对话框名称/组件ID
- **预期结果**: 详细描述期望的行为或属性值
- **实际结果**: 详细描述实际发生的行为或属性值
- **问题类型**: 按钮不可见/边框颜色错误/圆角不一致/对比度不足
- **根因分析**: 简要分析问题产生的原因
- **修复建议**: 具体的修复方案或代码修改建议

### 边框测试结果
- 通过边框验证: X 个组件
- 边框宽度不一致: Y 个组件
- 边框颜色错误: Z 个组件
- 圆角半径不一致: W 个组件
- 对比度不足: V 个组件

### 边框失败详情

#### 组件: [组件名称]
- **位置**: 布局文件路径:行号
- **边框宽度**: 当前值 [X]dp (预期: [Y]dp)
- **边框颜色**: 当前值 [颜色值/资源] (预期: [颜色资源])
- **圆角半径**: 当前值 [X]dp (预期: [Y]dp)
- **背景颜色**: [颜色值/资源]
- **对比度**: [X]:1 (预期: ≥ 3:1)
- **问题描述**: 简要描述边框不符合规范的具体问题
- **修复建议**: 具体的修复方案

### 测试统计摘要
| 测试类型 | 通过数 | 失败数 | 通过率 |
|---------|-------|-------|-------|
| 按钮颜色测试 | X | Y | Z% |
| 边框视觉测试 | X | Y | Z% |
| 对比度测试 | X | Y | Z% |
| 总计 | X | Y | Z% |

### 验证结果
- 修复后测试通过: 是/否
```

## 自动化断言

### 按钮颜色断言
使用自定义ColorMatcher验证按钮颜色：
- `ColorMatcher.withPrimaryColor()` - 验证主色调
- `ColorMatcher.withSecondaryTextColor()` - 验证次要文本色  
- `ColorMatcher.notWhiteText()` - 验证非白色文本

### 边框视觉断言
使用自定义BorderMatcher验证边框样式：
- `BorderWidthMatcher.withBorderWidth(dp)` - 验证边框宽度（如1dp）
- `BorderColorMatcher.withBorderColor(@ColorRes int)` - 验证边框颜色资源
- `CornerRadiusMatcher.withCornerRadius(@DimenRes int)` - 验证圆角半径
- `BorderContrastMatcher.minContrastRatio(3.0)` - 验证边框与背景对比度 ≥ 3:1

### 边框Matcher实现示例
```java
// 边框宽度Matcher
public class BorderWidthMatcher extends TypeSafeMatcher<View> {
    private final float expectedWidth;
    
    public static BorderWidthMatcher withBorderWidth(float expectedWidthDp) {
        return new BorderWidthMatcher(expectedWidthDp);
    }
    
    @Override
    protected boolean matchesSafely(View view) {
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) background;
            float expectedPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, expectedWidth, 
                view.getResources().getDisplayMetrics()
            );
            return Math.abs(drawable.getStrokeWidth() - expectedPx) <= 0.5f;
        }
        return false;
    }
}

// 圆角半径Matcher
public class CornerRadiusMatcher extends TypeSafeMatcher<View> {
    private final int expectedRadiusResId;
    
    public static CornerRadiusMatcher withCornerRadius(@DimenRes int radiusResId) {
        return new CornerRadiusMatcher(radiusResId);
    }
    
    @Override
    protected boolean matchesSafely(View view) {
        Drawable background = view.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) background;
            float[] radii = drawable.getCornerRadii();
            if (radii != null && radii.length == 8) {
                float expectedPx = view.getResources().getDimension(expectedRadiusResId);
                for (float radius : radii) {
                    if (Math.abs(radius - expectedPx) > 1) return false;
                }
                return true;
            }
        }
        return false;
    }
}
```

## 扩展测试覆盖

当添加新功能模块时，需要：
1. 在代码扫描中发现新的对话框
2. 为新对话框编写测试用例
3. 更新对话框清单
4. 执行测试验证