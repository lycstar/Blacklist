# 应用黑名单 (Blacklist)

一个开源的 Android 应用隐藏工具。通过 Android **设备管理员 + 设备所有者 (Device Owner)** 机制,将任意应用从桌面和 launcher 中隐藏(图标消失、无法启动、数据保留),随时可一键恢复显示。

> 典型用途:给孩子设备隐藏不合适的 App、屏蔽广告/厂商预装的顽固应用、临时隐藏社交通讯软件避免打扰。

## 功能特性

- **初始化向导(Shizuku 风格)**:首次打开自动进入分步授权引导——激活管理员 → 导出/复制电脑端命令 → 自动检测授权结果,通过后自动进入主界面
- **一键授权脚本**:内置 Windows(.bat)与 macOS/Linux/Git Bash(.sh)脚本,应用内可直接导出;脚本自动检查 adb、设备连接、已装所有者,失败时用中文说明原因和解决办法
- **隐藏 / 恢复应用**:输入包名或从应用列表中选择,一键隐藏(禁用);已隐藏的应用一键恢复
- **实时状态查询**:显示目标应用的安装状态与隐藏状态(隐藏中 / 正常显示 / 未知)
- **应用选择器**:内置可搜索的已安装应用列表,无需手动输入包名;用户应用在前,系统应用在后
- **已隐藏应用列表**:集中管理所有已隐藏的应用,逐个恢复,并实时验证实际隐藏状态
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

设备连接电脑并开启 USB 调试后,任选一种方式:

**方式一(推荐):一键脚本** —— 在应用初始化向导中点击「导出脚本」,保存后运行:

- Windows:双击 `setup-device-owner.bat`
- macOS / Linux / Git Bash:`chmod +x setup-device-owner.sh && ./setup-device-owner.sh`

脚本会自动检查 adb、设备连接、应用安装情况,失败时用中文提示原因与解决办法;也可以从仓库直接获取脚本:`app/src/main/assets/scripts/`。

**方式二:手动执行命令**:

```bash
adb shell dpm set-device-owner cc.lycstar.admin/.MyAdmin
```

输出 `Success: Device owner written to ...` 即成功。回到应用,向导会自动检测到授权完成并进入主界面。

常见失败原因:

| 报错 | 原因与解决 |
|---|---|
| `Not allowed to set the device owner because there are already some accounts on the device` | 设备上存在账户,先移除全部账户(MIUI 还需删除锁屏密码);实在不行恢复出厂后再试 |
| `Not allowed to set the device owner because there are already multiple users on the device` | 移除多用户/访客/手机分身/应用双开产生的用户后再试 |
| `Trying to set device owner but device already has a device owner` | 已有其他设备所有者,需先解除或恢复出厂 |
| 模拟器/MIUI 等定制系统失败 | 见下方「机型兼容性说明」,MIUI 有额外前置步骤 |

### 机型兼容性说明(MIUI / 双开 / 分身等)

**⚠️ 应用双开、手机分身/系统分身与设备所有者冲突(重要弊端)**

- MIUI 的「应用双开」在底层是一个受管理的影子用户(userId 999),「手机分身/系统分身」则是第二个系统用户;而 `set-device-owner` 要求设备处于"干净"的单用户、无账户状态。
- 因此:**设置前请先关闭应用双开、手机分身、访客模式等多用户功能**;设置设备所有者后,这些功能在部分机型上会无法使用。介意此限制的话请勿设置设备所有者。
- 一台设备同时只能有一个设备所有者/激活的设备管理员:如果你已经设置过其他同类设备管理工具,需要先移除它们的管理员才能设置本应用。

**小米 / MIUI 额外前置步骤**(逐项确认,任何一项没做都可能失败):

1. 开发者选项中开启「**USB 调试(安全设置)**」
2. 退出小米账号,以及「设置 → 账户」中的全部账户
3. 删除锁屏密码
4. 关闭「应用双开」「手机分身」
5. 开发者选项中关闭「**MIUI 优化**」——注意:关闭 MIUI 优化本身会带来一些副作用(如分享面板选不到双开应用),**设置成功后可以重新打开 MIUI 优化**,不影响已生效的设备所有者

**其他机型**:部分机型(如索尼)需要移除 SIM 卡后才能设置成功。

参考资料:[dpm 命令失败原因(CSDN)](https://blog.csdn.net/2401_84538391/article/details/138406843) · [MIUI 双开 userId 999 原理](https://bbs.oneplus.com/thread/5817716)

### 3. 隐藏应用

- 在「目标应用」卡片中输入包名,或点击「选择应用」从列表挑选;
- 界面会实时显示该应用的安装与隐藏状态;
- 点击「禁用(隐藏)」即可从桌面隐藏该应用;点击「恢复(取消隐藏)」还原;
- 所有已隐藏应用集中显示在下方列表,可逐个恢复。

> ⚠️ **不要隐藏本应用自身**(应用已内置拦截),否则你将无法再打开它来恢复其他应用。若不小心操作导致无法恢复:重新安装本应用不会丢失记录,重新设置 device owner 后即可继续;最后手段是恢复出厂。

### 卸载说明

正常顺序:先在应用内「停用管理员权限」→ 再卸载。若已成为设备所有者,可导出或直接运行内置的 `remove-admin.sh` / `remove-admin.bat` 脚本,其内容等价于:

```bash
adb shell dpm remove-active-admin cc.lycstar.admin/.MyAdmin
```

部分系统要求设备所有者应用标记 `testOnly` 才能被 adb 移除,此时需恢复出厂设置。请谨慎决定是否成为设备所有者。

## Fork 后修改包名指南

fork 后改成自己的包名只需四步(配置已集中到一处):

1. **改 `app/build.gradle.kts`** 顶部的:

   ```kotlin
   val appId = "cc.lycstar.admin"   // ← 改成你的包名,例如 com.yourname.blacklist
   ```

   `namespace` 与 `applicationId` 都引用了这个变量,无需改第二处。

2. **移动源码目录**:`app/src/main/java/cc/lycstar/admin/` 整体移动到与你的包名对应的路径(例如 `com/yourname/blacklist/`),并把每个 `.kt` 文件的 `package cc.lycstar.admin` 改成新包名。

3. **改内置脚本中的包名**:`app/src/main/assets/scripts/` 下四个脚本的 `PKG` 变量(`.sh` 和 `.bat` 都要改),初始化向导导出的脚本就来自这里。

4. **改 adb 命令中的组件名**:上文所有 `cc.lycstar.admin/.MyAdmin` 相应替换为新包名(`.MyAdmin` 部分不变)。

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
├── assets/scripts/                  # 电脑端一键脚本(应用内可导出)
│   ├── setup-device-owner.sh        #   设备所有者授权 (macOS/Linux/Git Bash)
│   ├── setup-device-owner.bat       #   设备所有者授权 (Windows)
│   ├── remove-admin.sh              #   移除管理员 (macOS/Linux/Git Bash)
│   └── remove-admin.bat             #   移除管理员 (Windows)
├── java/cc/lycstar/admin/
│   ├── MainActivity.kt              # Compose 入口 + 管理员授权结果处理
│   ├── BlacklistViewModel.kt        # 全部业务逻辑(DPM 调用、状态管理、本地记录、脚本导出)
│   ├── AppInfo.kt                   # 应用条目数据类
│   ├── MyAdmin.kt                   # DeviceAdminReceiver(设备管理员回调)
│   └── ui/
│       ├── BlacklistTheme.kt        # Material 3 主题(动态取色)
│       ├── AppRoot.kt               # 根路由(初始化向导/主界面切换、回到前台自动检测)
│       ├── SetupScreen.kt           # 初始化向导(分步授权、复制命令、导出脚本)
│       └── BlacklistScreen.kt       # 主界面(权限卡片/目标应用/隐藏列表/选择对话框)
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
