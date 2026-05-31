plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    java
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure:event-adapter"))
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterActuator)
    implementation(libs.springBootStarterValidation)
    implementation("org.springframework.kafka:spring-kafka")

    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)

    testImplementation(libs.springBootStarterTest)

    testRuntimeOnly(libs.junitPlatformLauncher)
}