plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.springBom))
    implementation(project(":application"))
    implementation("org.springframework.kafka:spring-kafka")
    implementation(libs.jacksonDatabind)
    implementation(libs.schemas)
    implementation(libs.kafkaAvroSerializer)
    implementation(libs.tradingCommon)




    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
}
