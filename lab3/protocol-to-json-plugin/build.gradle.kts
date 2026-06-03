plugins {
    id("java")
    id("maven-publish")
    id("java-gradle-plugin")
}

gradlePlugin {
    plugins {
        create("protocolToJson") {
            id = "frolenko.protocol-to-json-plugin"
            implementationClass = "ProtocolToJsonPlugin"
        }
    }
}

group = "frolenko"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}