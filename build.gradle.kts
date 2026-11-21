plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.sasha"
version = "1.0.1"
description = "Collectibles and Mystery Boxes for Fractal Network's lobby: hats, suits, gadgets, cloaks, emotes, gestures, the Mystery Vault and Mystery Dust. Built by Sasha Wildfang."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // Compiled against the oldest supported API so the jar runs on Paper 1.21.11 and every newer release.
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // FractalLobby: menus, messages, holograms, trails, chat colours (the plugin depends on it).
    compileOnly("com.sasha:FractalLobby")
    testImplementation("com.sasha:FractalLobby")

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    // Paper 1.21.11 requires Java 21; newer servers run on newer JVMs, which load Java 21 bytecode fine.
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Xlint:removal"))
    }

    processResources {
        // plugin.yml quotes the description in single quotes, so an apostrophe (Network's) is doubled there.
        val props = mapOf("version" to project.version, "description" to project.description.toString().replace("'", "''"))
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("1.21.11")
    }
}
