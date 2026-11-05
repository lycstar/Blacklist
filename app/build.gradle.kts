import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// =============================================================================
// Fork 改包名:一般只需修改下面的 appId,并同步移动源码目录
// (src/main/java/cc/lycstar/admin → 新包路径,同时更新各 .kt 文件的 package 声明)。
// AndroidManifest 中 receiver 使用相对类名 ".MyAdmin",会自动跟随 namespace,无需改动。
// 详细步骤见 README.md 的「Fork 后修改包名指南」。
// =============================================================================
val appId = "cc.lycstar.admin"

// =============================================================================
// 签名配置:CI 通过环境变量注入(GitHub Actions Secrets);本地通过 local.properties
// (已被 .gitignore 忽略)配置。两者都没有时,release 构建不签名,不影响 debug 构建。
// local.properties 示例:
//   blacklist.storeFile=D:/path/to/blacklist.keystore
//   blacklist.storePassword=xxxxxx
//   blacklist.keyAlias=blacklist_key
//   blacklist.keyPassword=xxxxxx
// =============================================================================
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(envKey: String, propKey: String): String? =
    System.getenv(envKey) ?: localProps.getProperty(propKey)

val releaseStoreFile = signingValue("SIGNING_KEYSTORE_FILE", "blacklist.storeFile")
    ?.let { file(it) }?.takeIf { it.exists() }
val releaseStorePassword = signingValue("SIGNING_STORE_PASSWORD", "blacklist.storePassword")
val releaseKeyAlias = signingValue("SIGNING_KEY_ALIAS", "blacklist.keyAlias") ?: "blacklist_key"
val releaseKeyPassword = signingValue("SIGNING_KEY_PASSWORD", "blacklist.keyPassword")
    ?: releaseStorePassword

android {
    namespace = appId
    compileSdk = 34

    defaultConfig {
        applicationId = appId
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
