import java.util.Properties

plugins {
    id("java")
    id("frolenko.protocol-to-json-plugin") version "1.0-SNAPSHOT"
}

group = "frolenko"
version = findProperty("projectVersion") ?: "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val setVersionTask = tasks.register("setVersion") {
    group = "versioning"

    doLast {
        val newVersion = project.findProperty("projectVersion") as String?
            ?: throw GradleException("Pass version via -PprojectVersion=...")

        val propsFile = file("gradle.properties")

        if (!propsFile.exists()) {
            propsFile.createNewFile()
        }

        val props = Properties().apply {
            if (propsFile.length() > 0) {
                propsFile.inputStream().use { load(it) }
            }
        }

        props.setProperty("projectVersion", newVersion)

        propsFile.outputStream().use {
            props.store(it, null)
        }

        println("Version updated to: $newVersion")
    }
}

if (file("protocol-json").exists()) {
    setVersionTask.configure {
        finalizedBy("generateProtocolJson")
    }
}