plugins {
    id("java")
}

group = "frolenko"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("frolenko:annotations:1.0-SNAPSHOT")
    annotationProcessor("frolenko:processor:1.0-SNAPSHOT")
    implementation("frolenko:runtime:1.0-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}