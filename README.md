# 应用黑名单 (Blacklist)

一个开源的 Android 应用隐藏工具。通过 Android **设备管理员 + 设备所有者 (Device Owner)** 机制,将任意应用从桌面和 launcher 中隐藏(图标消失、无法启动、数据保留),随时可一键恢复显示。

> 典型用途:给孩子设备隐藏不合适的 App、屏蔽广告/厂商预装的顽固应用、临时隐藏社交通讯软件避免打扰。

## 功能特性

- **应用列表主界面**:全部应用以「名称 + 包名」两行列出,支持搜索、筛选(全部 / 隐藏中 / 显示中),状态徽章实时标注
- **多选批量操作**:勾选多个应用后一键批量隐藏 / 批量恢复;对已卸载但预设了隐藏的包同样可管理
- **初始化向导(Shizuku 风格)**:首次打开自动进入分步授权引导——激活管理员 → 复制电脑端命令 → 自动检测授权结果,通过后自动进入主界面
- **实时状态查询**:每个应用实时显示隐藏状态(隐藏中 / 显示中),数据来自 `DevicePolicyManager` 实时校验
- **权限状态面板**:直观显示设备管理员与设备所有者的授权状态,并提供应用内授权 / 停用入口;应用回到前台时自动重新检测
- **Material 3 界面**:Jetpack Compose 构建,支持 Android 12+ 动态取色与深色模式

## 工作原理

本应用使用系统级 `DevicePolicyManager` API:

1. **设备管理员 (Device Admin)**:应用的基础授权,通过系统弹窗授予;
2. **设备所有者 (Device Owner)**:更高级别的设备管理身份,只能通过 adb 在"干净"的设备上一次性设置。只有成为设备所有者后,才能调用 `setApplicationHidden()` 隐藏应用;
3. 隐藏应用的本质是 `setApplicationHidden(adminComponent, packageName, true)`:
   - 桌面图标消失、应用无法启动、通知不显示;
   - **应用数据不会被删除**,恢复后一切如常;
   - 对未安装的包名预设置隐藏,应用日后安装时会自动进入隐藏状态;
4. 已隐藏应用记录保存在应用本地 (`SharedPreferences`),并每次打开时通过 `isApplicationHidden()` 实时校验。

## 环境要求

- Android 7.0+(API 24)及以上
- 设置设备所有者需要:电脑 + adb,且设备满足以下"干净"条件之一:
  - 恢复出厂后尚未登录任何账户的新设备(最可靠)
  - 或已移除所有账户的设备(设置 → 账户,逐一移除)

## 构建

### 方式一:Android Studio(推荐)

1. 安装 [Android Studio](https://developer.android.com/studio)(任意近期版本)
2. `File → Open` 打开本项目根目录
3. 等待 Gradle Sync 完成,点击 `Run`(或 `Build → Build APK(s)`)

> 若 Sync 报 Java 版本错误,在 `Settings → Build Tools → Gradle` 中把 Gradle JDK 设为 17(Studio 内置的多版本 JDK 列表里有)。

### 方式二:命令行

需要 **JDK 17**(不要使用新版 Android Studio 自带的 JBR,其 Java 25 与 Gradle 8.2 / AGP 8.2 不兼容)。

```bash
# Windows(Git Bash)示例:
export JAVA_HOME="D:/application/jdk17/jdk-17.0.20.1+1"   # 指向你的 JDK 17 安装目录
./gradlew assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

APK 输出位置:`app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### CI 自动构建与发布

仓库配置了 GitHub Actions(`.github/workflows/build.yml`):

- **push 到 main / PR / 手动触发**:自动构建 debug APK,在 Actions 运行页面的 Artifacts 中下载;
- **推送 `v*` 标签**:自动构建并创建 **GitHub Release**,标题带版本号与构建日期(如 `v1.2 · 2026-11-05`),附 `Blacklist-v版本-日期.apk`(统一密钥正式签名,可直接安装)。签名密钥通过 Actions Secrets 注入 CI,不进仓库;本地构建通过 `local.properties` 读取(见 `app/build.gradle.kts` 顶部注释)。

发版步骤:

```bash
# 1. 修改 app/build.gradle.kts 中的 versionName / versionCode 并提交
git tag v1.3
git push origin v1.3
```

## 使用步骤

### 1. 激活设备管理员

首次打开应用会自动进入**初始化向导**,按界面提示点击「授予管理员权限」即可;也可以在应用内随时「停用管理员权限」。

### 2. 设置设备所有者(隐藏功能的前提)

设备连接电脑并开启 USB 调试后,在初始化向导第 2 步**点击「复制命令」**,粘贴到电脑终端执行(命令中的包名会随 fork 自动变化):

```bash
adb shell dpm set-device-owner cc.lycstar.admin/.MyAdmin
```

输出 `Success: Device owner written to ...` 即成功。回到应用,向导会自动检测到授权完成并进入主界面。

常见失败原因:

| 报错 | 原因与解决 |
|---|---|
| `Not allowed to set the device owner because there are already some accounts on the device` / `Calling identity is not authorized` | 设备上存在账户(实测小米 14:退出小米账号 + 移除「账号与同步」中所有账户即可);实在不行恢复出厂后再试 |
| `Not allowed to set the device owner because there are already multiple users on the device` | 移除多用户/访客/手机分身/应用双开产生的用户后再试 |
| `Trying to set device owner but device already has a device owner` | 已有其他设备所有者,需先解除或恢复出厂 |
| 模拟器/MIUI 等定制系统失败 | 见下方「机型兼容性说明」,MIUI 有额外前置步骤 |

### 机型兼容性说明(实测:小米 14)

**小米 14 实测**:设置设备所有者前只需两步——

1. **退出登录小米账号**
2. 进入「设置 → 账号与同步」,**移除其中所有账户**

完成后直接执行 `adb shell dpm set-device-owner cc.lycstar.admin/.MyAdmin` 即可成功。无需删除锁屏密码、无需关闭 MIUI 优化、无需其他操作(社区流传的这些步骤在小米 14 上实测均不需要)。

**⚠️ 应用双开、手机分身 / 系统分身与设备所有者冲突(实测)**

设置设备所有者后,「应用双开」「手机分身 / 系统分身」等多用户功能将无法使用,介意者请勿设置;设置前若已开启双开 / 分身,请先关闭。

**通用限制**:一台设备同时只能有一个设备所有者,如已激活其他同类设备管理工具,需先移除。

> 其他机型 / 系统版本的实测差异,欢迎提交 issue 补充。

### 3. 隐藏应用

- 主界面即应用列表,每行显示「应用名称 + 包名」;
- 通过搜索框或筛选 Chips(全部 / 隐藏中 / 显示中)快速定位目标;
- **单个操作**:点击行左侧勾选框选中 → 底部操作栏点「禁用(隐藏)」或「恢复(取消隐藏)」;
- **批量操作**:连续勾选多个应用,底部操作栏一次批量处理;
- 右上角「⋮」菜单可手动输入包名(用于隐藏尚未安装的包,隐藏状态会在安装后自动生效)。

> ⚠️ **不要隐藏本应用自身**(应用已内置拦截),否则你将无法再打开它来恢复其他应用。若不小心操作导致无法恢复:重新安装本应用不会丢失记录,重新设置 device owner 后即可继续;最后手段是恢复出厂。

### 卸载说明

正常顺序:先在应用内「停用管理员权限」(右上角 ⋮ 菜单)→ 再卸载。若已成为设备所有者,需要先执行:

```bash
adb shell dpm remove-active-admin cc.lycstar.admin/.MyAdmin
```

部分系统要求设备所有者应用标记 `testOnly` 才能被 adb 移除,此时需恢复出厂设置。请谨慎决定是否成为设备所有者。

## Fork 后修改包名指南

fork 后改成自己的包名只需三步(配置已集中到一处):

1. **改 `app/build.gradle.kts`** 顶部的:

   ```kotlin
   val appId = "cc.lycstar.admin"   // ← 改成你的包名,例如 com.yourname.blacklist
   ```

   `namespace` 与 `applicationId` 都引用了这个变量,无需改第二处;初始化向导中的 adb 命令也会自动跟随新包名。

2. **移动源码目录**:`app/src/main/java/cc/lycstar/admin/` 整体移动到与你的包名对应的路径(例如 `com/yourname/blacklist/`),并把每个 `.kt` 文件的 `package cc.lycstar.admin` 改成新包名。

3. **改 adb 命令中的组件名**:上文所有 `cc.lycstar.admin/.MyAdmin` 相应替换为新包名(`.MyAdmin` 部分不变)。

> AndroidManifest 中 receiver 写的是相对类名 `.MyAdmin`,会自动跟随新的 `namespace`,不需要改 Manifest。
>
> 签名:Release APK 需要 `blacklist.keystore`(仓库不含密钥)。fork 者请自行生成(`keytool -genkeypair -alias blacklist_key -validity 10950 ...`)并配置到仓库 Secrets 或本地 `local.properties`,详见 `app/build.gradle.kts` 顶部注释;没有密钥时 CI 的 release 任务会失败,但 debug 构建不受影响。

## 技术栈

- Kotlin 2.0 + Jetpack Compose(BOM 2024.06)+ Material 3
- 单模块 Android 工程,无额外 SDK 依赖
- `DevicePolicyManager` 系统 API,无任何第三方权限库

## 项目结构

```
app/src/main/
├── AndroidManifest.xml              # 应用声明、QUERY_ALL_PACKAGES 权限、设备管理员 receiver
├── java/cc/lycstar/admin/
│   ├── MainActivity.kt              # Compose 入口 + 管理员授权结果处理
│   ├── BlacklistViewModel.kt        # 全部业务逻辑(DPM 调用、应用列表、多选批量、本地记录)
│   ├── AppInfo.kt                   # 应用条目数据类
│   ├── MyAdmin.kt                   # DeviceAdminReceiver(设备管理员回调)
│   └── ui/
│       ├── BlacklistTheme.kt        # Material 3 主题(动态取色)
│       ├── AppRoot.kt               # 根路由(初始化向导/主界面切换、回到前台自动检测)
│       ├── SetupScreen.kt           # 初始化向导(分步授权、复制命令、兼容性提示)
│       └── BlacklistScreen.kt       # 主界面(应用列表/搜索筛选/多选批量/手动输入)
└── res/
    ├── values/strings.xml           # 所有文案
    └── xml/device_admin_receiver.xml # 设备管理员策略声明
```

## 已知限制

- **与应用双开 / 手机分身 / 访客模式等多用户功能冲突**:设置设备所有者前需关闭它们,设置后在部分机型上这些功能不可用(详见「机型兼容性说明」);
- **一台设备只能有一个设备所有者**:与其他同类设备管理工具互斥,需先移除对方;
- 隐藏列表是应用本地记录:如果隐藏状态在系统层面被第三方改动,列表中的状态标签会显示「未验证」或随实时校验更新;
- 部分厂商 ROM 可能对 device owner 行为有自定义限制,兼容性以实际设备为准;
- 应用自身不可被隐藏(内置拦截),防止失去管理入口。

## 免责声明

本项目仅供学习研究和个人设备管理使用。隐藏系统关键应用或误操作 device owner 可能影响设备正常使用,使用前请了解相关风险。作者不对任何因使用本工具造成的损失负责。

## License

MIT
