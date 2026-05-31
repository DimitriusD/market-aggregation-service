plugins {
    `java-library`
}

dependencies {
    api(libs.slf4jApi)
    implementation(libs.tradingCommon)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)

    testImplementation(libs.junitJupiter)
    testImplementation("org.assertj:assertj-core:3.26.3")

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testRuntimeOnly(libs.junitPlatformLauncher)
}