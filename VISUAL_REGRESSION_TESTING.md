## 视觉回归测试方案

### 1. 概述

视觉回归测试用于检测应用界面在代码变更后是否出现视觉上的意外变化。本方案主要关注深色模式下的UI显示问题，特别是对话框按钮颜色可见性问题。

### 2. 测试工具选择

| 工具名称 | 用途 | 推荐指数 |
|---------|------|---------|
| Firebase Test Lab | 云端设备测试和截图对比 | ★★★★★ |
| Shot (Pinterest) | 开源截图测试库 | ★★★★☆ |
| Robolectric + PixelCopy | 本地单元测试截图 | ★★★☆☆ |
| Percy | 第三方视觉回归服务 | ★★★★☆ |

### 3. Firebase Test Lab 配置指南

#### 3.1 前提条件
- 已创建 Firebase 项目
- 已配置 Google Cloud 服务账号
- 安装 gcloud CLI 工具

#### 3.2 配置步骤

1. **启用 Firebase Test Lab API**
```bash
gcloud services enable testing.googleapis.com
```

2. **创建测试配置文件** (test-lab-config.yaml)
```yaml
type: instrumentation
appApk: app/build/outputs/apk/debug/app-debug.apk
testApk: app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
testRunnerClass: androidx.test.runner.AndroidJUnitRunner
deviceIds:
  - Pixel2
  - Pixel4
  - Nexus6P
osVersionIds:
  - 29
  - 30
  - 31
locales:
  - zh-CN
orientations:
  - portrait
  - landscape
```

3. **上传APK并运行测试**
```bash
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=Pixel2,version=30,locale=zh-CN,orientation=portrait \
  --device model=Pixel4,version=31,locale=zh-CN,orientation=portrait \
  --timeout 30m \
  --results-bucket gs://your-bucket-name
```

### 4. 视觉检查点定义

| 检查点名称 | 屏幕位置 | 检查内容 | 预期结果 |
|-----------|---------|---------|---------|
| 对话框按钮颜色-深色模式 | 设置页面编辑对话框 | 保存/取消按钮文字颜色 | 主色调(#FF427DFF) |
| 对话框按钮颜色-浅色模式 | 设置页面编辑对话框 | 保存/取消按钮文字颜色 | 主色调(#FF427DFF) |
| 导航栏高亮 | 底部导航栏 | 当前选中项背景 | 与导航栏背景一致 |
| 导航图标颜色 | 底部导航栏 | 选中/未选中图标 | 主色调/灰色 |
| 文字对比度 | 所有页面 | 文本与背景对比度 | >= 4.5:1 |

### 5. 截图对比流程

```
┌─────────────────────────────────────────────────────────────┐
│                    视觉回归测试流程                          │
├─────────────────────────────────────────────────────────────┤
│  1. 构建APK                                                 │
│     ↓                                                       │
│  2. 上传到Firebase Test Lab                                  │
│     ↓                                                       │
│  3. 在多设备上执行测试并截图                                  │
│     ↓                                                       │
│  4. 自动对比基线截图与新截图                                  │
│     ↓                                                       │
│  5. 生成差异报告                                             │
│     ↓                                                       │
│  6. 人工审核差异点                                           │
│     ↓                                                       │
│  7. 更新基线或修复代码                                        │
└─────────────────────────────────────────────────────────────┘
```

### 6. 测试用例清单

#### 6.1 深色模式测试
| 测试ID | 测试场景 | 测试步骤 | 预期结果 |
|--------|---------|---------|---------|
| VR-001 | 用户名编辑对话框 | 深色模式下打开编辑用户名对话框 | 按钮文字清晰可见，非白色 |
| VR-002 | 简介编辑对话框 | 深色模式下打开编辑简介对话框 | 按钮文字清晰可见，非白色 |
| VR-003 | 头像对话框 | 深色模式下打开头像设置对话框 | 按钮文字清晰可见，非白色 |
| VR-004 | 导航栏显示 | 深色模式下查看底部导航栏 | 选中项高亮区域与背景一致 |

#### 6.2 浅色模式测试
| 测试ID | 测试场景 | 测试步骤 | 预期结果 |
|--------|---------|---------|---------|
| VR-005 | 用户名编辑对话框 | 浅色模式下打开编辑用户名对话框 | 按钮文字清晰可见 |
| VR-006 | 导航栏显示 | 浅色模式下查看底部导航栏 | 选中项高亮区域与背景一致 |

### 7. 集成到CI/CD

在 GitHub Actions 或 GitLab CI 中添加以下步骤：

```yaml
- name: Run Firebase Test Lab
  env:
    GCLOUD_SERVICE_KEY: ${{ secrets.GCLOUD_SERVICE_KEY }}
  run: |
    echo $GCLOUD_SERVICE_KEY > service-key.json
    gcloud auth activate-service-account --key-file=service-key.json
    gcloud firebase test android run \
      --type instrumentation \
      --app app/build/outputs/apk/debug/app-debug.apk \
      --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
      --device model=Pixel2,version=30,locale=zh-CN
```

### 8. 维护与更新

- **基线更新**: 当UI设计有意变更时，及时更新基线截图
- **设备矩阵更新**: 定期评估并更新测试设备列表
- **阈值调整**: 根据实际情况调整颜色差异容忍度

### 9. 注意事项

1. 确保测试设备的系统主题设置正确（跟随系统/深色/浅色）
2. 截图对比时考虑不同设备的屏幕分辨率差异
3. 对于动态内容（如时间、随机数据），使用mock数据确保测试稳定性