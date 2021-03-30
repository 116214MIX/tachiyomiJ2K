object Versions {
    const val ACRA = "4.9.2"
    const val CHUCKER = "3.2.0"
    const val COIL = "0.11.0"
    const val COROUTINES = "1.4.2"
    const val FASTADAPTER = "5.0.0"
    const val HYPERION = "0.9.27"
    const val NUCLEUS = "3.0.0"
    const val OKHTTP = "4.8.1"
    const val OSS_LICENSE = "17.0.0"
    const val RETROFIT = "2.7.2"
    const val KOTLINSERIALIZATION = "1.0.1"
    const val ROBO_ELECTRIC = "3.1.4"
    const val RX_BINDING = "1.0.1"
    const val TIMBER = "4.7.1"
    const val WORKMANAGER = "2.5.0"
    const val aboutLibraries = "8.3.0"
    const val androidAnnotations = "1.1.0"
    const val androidAppCompat = "1.1.0"
    const val androidBiometrics = "1.0.1"
    const val androidBrowser = "1.2.0"
    const val androidCardView = "1.0.0"
    const val androidConstraintLayout = "1.1.3"
    const val androidCoreKtx = "1.3.1"
    const val androidGradlePlugin = "4.1.3"
    const val androidLifecycle = "2.2.0"
    const val androidMaterial = "1.1.0"
    const val androidMultiDex = "2.0.1"
    const val androidPalette = "1.0.0"
    const val androidPreferences = "1.1.1"
    const val androidRecyclerView = "1.1.0"
    const val androidSqlite = "2.1.0"
    const val androidWorkManager = "2.4.0"
    const val assertJ = "3.12.2"
    const val changelog = "2.1.0"
    const val chucker = "3.2.0"
    const val coil = "1.1.1"
    const val conductor = "2.1.5"
    const val directionalViewPager = "a844dbca0a"
    const val diskLruCache = "2.0.2"
    const val fastAdapter = "5.0.0"
    const val filePicker = "2.5.2"
    const val firebase = "17.5.0"
    const val firebaseCrashlytics = "17.2.1"
    const val flexibleAdapter = "5.1.0"
    const val flexibleAdapterUi = "1.0.0"
    const val flowPreferences = "1.3.2"
    const val googlePlayServices = "17.0.0"
    const val googleServices = "4.3.3"
    const val gradleVersions = "0.29.0"
    const val gson = "2.8.6"
    const val hyperion = "0.9.27"
    const val injekt = "65b0440"
    const val jsoup = "1.13.1"
    const val junit = "4.13"
    const val kotlin = "1.4.20"
    const val kotlinCoroutines = "1.3.9"
    const val kotlinSerialization = "1.0.1"
    const val kotson = "2.5.0"
    const val ktlint = "9.4.0"
    const val loadingButton = "2.2.0"
    const val materialDesignDimens = "1.4"
    const val materialDialogs = "3.1.1"
    const val mockito = "1.10.19"
    const val moshi = "1.9.3"
    const val nucleus = "3.0.0"
    const val numberSlidingPicker = "1.0.3"
    const val okhttp = "4.8.1"
    const val okio = "2.6.0"
    const val photoView = "2.3.0"
    const val reactiveNetwork = "0.13.0"
    const val requerySqlite = "3.31.0"
    const val retrofit = "2.7.2"
    const val retrofitKotlinSerialization = "0.7.0"
    const val roboElectric = "3.1.4"
    const val rxAndroid = "1.2.1"
    const val rxBinding = "1.0.1"
    const val rxJava = "1.3.8"
    const val rxPreferences = "1.0.2"
    const val rxRelay = "1.2.0"
    const val storioCommon = "8be19de@aar"
    const val storioSqlite = "8be19de@aar"
    const val stringSimilarity = "2.0.0"
    const val subsamplingImageScale = "93d74f0"
    const val systemUiHelper = "1.0.0"
    const val tagGroup = "1.6.0"
    const val tapTargetView = "1.13.0"
    const val tokenBucket = "1.7"
    const val unifile = "e9ee588"
    const val versionCompare = "1.3.4"
    const val viewStatePagerAdapter = "1.1.0"
    const val viewToolTip = "1.2.2"
    const val xlog = "1.6.1"
    const val kotlinter = "3.4.0"
}

object LegacyPluginClassPath {
    const val aboutLibraries = "com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:${Versions.aboutLibraries}"
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    const val googleServices = "com.google.gms:google-services:${Versions.googleServices}"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinSerializations = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val fireBaseCrashlytics = "com.google.firebase:firebase-crashlytics-gradle:2.3.0"
}

object AndroidVersions {
    const val BUILD_TOOL = "29.0.3"
    const val COMPILE_SDK = 29
    const val MIN_SDK = 23
    const val TARGET_SDK = 29
    const val VERSION_CODE = 68
    const val VERSION_NAME = "1.0.11"
    const val NDK = "22.0.7026061"
}

object Plugins {
    const val aboutLibraries = "com.mikepenz.aboutlibraries.plugin"
    const val androidApplication = "com.android.application"
    const val firebaseCrashlytics = "com.google.firebase.crashlytics"
    const val googleServices = "com.google.gms.google-services"
    const val kapt = "kapt"
    const val kotlinParcelize = "kotlin-parcelize"
    const val kotlinAndroid = "android"
    const val kotlinExtensions = "android.extensions"
    const val jetbrainsKotlin = "org.jetbrains.kotlin.android"
    const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
    val gradleVersions = PluginClass("com.github.ben-manes.versions", Versions.gradleVersions)
    val ktLint = PluginClass("org.jlleitschuh.gradle.ktlint", Versions.ktlint)
    val kotlinter = PluginClass("org.jmailen.kotlinter", Versions.kotlinter)
}

data class PluginClass(val name: String, val version: String)

object Configs {
    const val applicationId = "tachiyomi.mangadex"
    const val buildToolsVersion = "29.0.3"
    const val compileSdkVersion = 29
    const val minSdkVersion = 24
    const val targetSdkVersion = 29
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val versionCode = 110
    const val versionName = "2.2.2.2"
}

object BuildPluginsVersion {
    const val AGP = "com.android.tools.build:gradle:4.1.3"
    const val KOTLIN = "1.4.10"
    const val KOTLIN_GRADLE = "org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN"
    const val KOTLINTER = "3.3.0"
    const val GOOGLE_SERVICES = "com.google.gms:google-services:4.3.3"
    const val OSS_LICENSE = "com.google.android.gms:oss-licenses-plugin:0.10.2"
    const val VERSIONS_PLUGIN = "0.28.0"
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
