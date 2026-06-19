plugins {
    id("java")
    id("info.solidsoft.pitest") version "1.19.0"
}

group = "frolenko"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}

pitest {
    junit5PluginVersion.set("1.2.3")
    targetClasses.set(listOf("com.frolenko.auth.*"))
    targetTests.set(listOf("com.frolenko.auth.AuthServiceTestBroken"))
    outputFormats.set(listOf("HTML"))
    excludedMethods.set(listOf("generateSalt", "hash"))
}