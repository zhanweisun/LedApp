pluginManagement {
    repositories {
        maven{setUrl("https://maven.aliyun.com/repository/central")}
        maven{setUrl("https://maven.aliyun.com/repository/jcenter")}
        maven{setUrl("https://maven.aliyun.com/repository/google")}
        maven{setUrl("https://maven.aliyun.com/repository/gradle-plugin")}
        maven{setUrl("https://maven.aliyun.com/repository/public")}
        maven{setUrl("https://jitpack.io")}
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven{setUrl("https://maven.aliyun.com/repository/central")}
        maven{setUrl("https://maven.aliyun.com/repository/jcenter")}
        maven{setUrl("https://maven.aliyun.com/repository/google")}
        maven{setUrl("https://maven.aliyun.com/repository/gradle-plugin")}
        maven{setUrl("https://maven.aliyun.com/repository/public")}
        maven{setUrl("https://jitpack.io")}
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "LedScroller"
include(":app")
