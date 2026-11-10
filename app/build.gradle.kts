// 通过 -Pled.fullResources=true 可以把原 APK 的全部资源（含未使用的 Material/AppCompat 布局）一并打包。
// 默认只打包本工程用到的资源，APK 体积更小。
val fullResources = (findProperty("led.fullResources") as String?)?.toBoolean() ?: false

plugins {
    id("com.android.application")
}

android {
    namespace = "com.ledapp.scroller"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ledapp.scroller"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 使用 debug 签名，便于直接安装到设备
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // 资源全部沿用原 APK；视频/字体等已压缩格式不再压缩，安装后可直接映射播放
        noCompress += listOf("ttf", "otf", "mp4", "webp", "json", "png")
    }

    lint {
        // 原 APK 的对齐/国际化等提示不影响功能，发布构建时不因此中断
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// ---------------------------------------------------------------------------
// 可选：打包原 APK 的完整资源集合。
// 原 APK 从 resources.arsc 直接解包，其中包含 Material/AppCompat 等库的
// 混淆资源（二进制 XML），正常情况下依赖库会自带同名资源，无需重复打包。
// 如需完整保留（例如以后想直接使用原 APK 的某个布局），加上
//   -Pled.fullResources=true
// 即可把 src/full_res 作为额外资源目录参与合并。
// ---------------------------------------------------------------------------
if (fullResources) {
    android.sourceSets.getByName("main").res.srcDir("src/full_res")
}