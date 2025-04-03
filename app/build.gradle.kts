plugins {
    id("com.android.application")
}

android {
    namespace = "com.ims.bpcluat"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ims.bpcluat"
        minSdk = 23
        targetSdk = 34
        versionCode = 20
        versionName = "1.0.20"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Define the flavor dimensions using the flavorDimensions property
    flavorDimensions.add("environment")

    productFlavors {
        create("uat") {
            dimension = "environment"
            applicationId = "com.ims.bpcluat"
           // versionNameSuffix = "-UAT"
            buildConfigField("String", "ENVIRONMENT", "\"UAT\"")
            resValue("string", "app_name", "BPCL UAT")
        }
        create("production") {
            dimension = "environment"
            applicationId = "com.ims.bpcl"
          //  versionNameSuffix = "-PROD"
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
            resValue("string", "app_name", "BPCL")
        }
    }

    buildFeatures{
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.camera:camera-camera2:1.1.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha04")
    implementation("androidx.camera:camera-view:1.0.0-alpha24")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.navigation:navigation-fragment:2.6.0")
    implementation("androidx.navigation:navigation-ui:2.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation(files("./libs/java-json.jar"))
    implementation(files("./libs/json-smart-2.3.jar"))
    implementation(files("./libs/nimbus-jose-jwt-8.9.jar"))
    implementation(files("./libs/InterApp_IMS_PAX_v10.0.8-20240730.aar"))
    implementation(files("./libs/PaxNeptuneLiteApi_V2.01.00_20171025.jar"))
    implementation(files("./libs/BpclAlp_Sdk_1.0.2_202041028.aar"))
    implementation(files("./libs/GLPage_V1.03.00_20181030.jar"))
    implementation(files("./libs/gson-2.6.1.jar"))
    implementation ("com.google.android.material:material:1.2.0-alpha02")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.journeyapps:zxing-android-embedded:3.6.0")
}