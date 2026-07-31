# Toolbox 🛠️

一个多功能 Android 实用工具应用程序，包含定时、录音、统计、娱乐等功能。

![Android](https://img.shields.io/badge/API%2030-blue.svg) ![Java](https://img.shields.io/badge/语言-Java-orange.svg) ![版本](https://img.shields.io/badge/version-1.1.2-brightgreen.svg)

## 📱 简介

Toolbox 是一个功能全面的 Android 实用工具应用，将多种实用工具整合在一个应用中。应用采用现代化的 Material Design 设计，拥有导航抽屉和底部导航栏，方便用户快速访问不同功能模块。

## 🚀 功能特性

### 核心功能

| 功能 | 说明 |
|------|------|
| **定时返回** | 设置定时器，在指定时间后自动返回桌面（15-120分钟或自定义） |
| **统计** | 查看数据统计，支持 CSV/Excel 文件导入 |
| **录音机** | 音频录制功能 |
| **转盘抽奖** | 互动抽奖轮盘，用于随机选择 |
| **定时录屏** | 带定时功能的屏幕录制 |
| **更新日志** | 版本变更记录，带有未读通知角标 |
| **开发者入口** | 隐藏功能，通过点击开发者入口 5 次解锁测试区 |

### 附加模块

- **画廊** - 图片浏览
- **幻灯片放映** - 照片幻灯片播放效果
- **设置** - 用户资料管理

## 🧩 项目架构

```
app/
├── ui/
│   ├── home/          # HomeFragment, HomeViewModel
│   ├── gallery/       # GalleryFragment, GalleryViewModel
│   ├── slideshow/     # SlideshowFragment, SlideshowViewModel
│   ├── PanelGridAdapter # 功能面板网格适配器
│   └── settings/      # SettingsFragment, UserProfileManager
├── function/
│   ├── time/          # 定时器和锁屏功能
│   ├── lottery/       # 抽奖轮盘系统
│   ├── recorder_photo/# 屏幕和音频录制
│   ├── statistics/    # 数据统计模块
│   └── changelog/     # 更新日志管理器
└── MainActivity       # 主活动，含导航抽屉
```

### 关键技术

- **View Binding** - 类型安全的 UI 绑定
- **Navigation Component** - 安全且结构化的导航
- **ViewModel + LiveData** - 生命周期感知数据处理
- **Foreground Services** - 用于倒计时悬浮窗和屏幕录制
- **AlarmManager** - 用于精确计时和定时任务
- **SharedPreferences** - 本地状态存储

## 🛠️ 技术规格

| 项目 | 详情 |
|------|------|
| **最低 SDK** | API 30 (Android 11) |
| **目标 SDK** | API 34 (Android 14) |
| **语言** | Java 8 |
| **构建系统** | Gradle |
| **架构模式** | MVVM + Navigation Component |

## 🔐 所需权限

应用需要以下权限来支持各项功能：

- `SYSTEM_ALERT_WINDOW` - 悬浮窗权限，用于倒计时显示
- `FOREGROUND_SERVICE_*` - 前台服务权限（媒体投影、数据同步、麦克风）
- `POST_NOTIFICATIONS` - 通知权限（Android 13+）
- `SCHEDULE_EXACT_ALARM` - 精确闹钟调度
- `RECORD_AUDIO`, `CAMERA` - 录制功能所需权限
- `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE` - 文件管理权限

## 🧪 测试

- **单元测试**: `app/src/test/java/` (JUnit, Robolectric)
- **仪器测试**: `app/src/androidTest/java/` (Espresso, JUnit)

## 📦 构建与运行

1. 使用 Android Studio 打开项目
2. 同步 Gradle 依赖
3. 在 Android 设备或模拟器上构建并运行

## 🔄 版本历史

- **v1.1.2**（当前）：最新稳定版，已更新目标SDK并修复若干问题
- 详见应用内的 **更新日志** 功能获取详细发布记录

## 🤝 参与贡献

欢迎提交 Issue 或 Pull Request！

1. Fork 仓库
2. 创建您的特性分支 (`git checkout -b feature/新功能`)
3. 提交更改 (`git commit -m '添加新功能'`)
4. 推送到分支 (`git push origin feature/新功能`)
5. 提交 Pull Request

---

*使用 Android Jetpack 组件精心打造*
