import groovy.json.JsonSlurper
import groovy.util.logging.Log

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.rp_white_label"
    compileSdk = 36

    // Get the accent color from the config.json file
    fun getAccentColor(): String {
        val configFile = project.file("config.json")
        if (configFile.exists()) {
            val config = JsonSlurper().parseText(configFile.readText()) as Map<*, *>
            logger.lifecycle("Accent color: ${config["accentColor"]}")
            return config["accentColor"] as String
        }
        return "#000000"
    }

    // Task to generate colors.xml
    tasks.register("generateColorsFile") {
        doLast {
            val colorValue = getAccentColor()
            val resDir = project.layout.buildDirectory.dir("generated/res/white-label")
            logger.lifecycle("Res dir: ${resDir.get()}")
            val valuesDir = file("${resDir.get()}/values")
            valuesDir.mkdirs()
            val colorsFile = file("${valuesDir}/colors.xml")
            colorsFile.writeText("""
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <color name="accentColor">${colorValue}</color>
                </resources>
            """.trimIndent())
        }
    }

    // Add the generate task to preBuild
    tasks.named("preBuild") {
        dependsOn("generateColorsFile")
    }

    sourceSets {
        getByName("main") {
            res.srcDir(project.layout.buildDirectory.dir("generated/res/white-label"))
        }
    }

    defaultConfig {
        applicationId = "com.example.rp_white_label"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media)
    implementation(libs.androidx.preference)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}