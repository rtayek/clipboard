plugins {
    java
    application
    eclipse
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

javafx {
    version = "25.0.2"
    modules("javafx.controls")
}

application {
    mainClass.set("clipboard.Launcher")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources {
            setSrcDirs(listOf("src"))
            exclude("**/*.java")
        }
    }
    test {
        java.setSrcDirs(listOf("tst"))
        resources {
            setSrcDirs(listOf("tst"))
            exclude("**/*.java")
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
