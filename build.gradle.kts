plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")

    // Meteor - using local JAR for consistent version
    modImplementation(files("libs/meteor-client-${properties["meteor_version"] as String}.jar"))

    // StarScript (separate dependency, not bundled in Meteor)
    implementation("org.meteordev:starscript:0.2.3")

    val mcpVersion = properties["mcp_version"] as String
    modImplementation("io.modelcontextprotocol.sdk:mcp:${mcpVersion}")!!.let { include(it) }
    modImplementation("io.modelcontextprotocol.sdk:mcp-core:${mcpVersion}")!!.let { include(it) }
    modImplementation("io.modelcontextprotocol.sdk:mcp-json:${mcpVersion}")!!.let { include(it) }
    modImplementation("io.modelcontextprotocol.sdk:mcp-json-jackson2:${mcpVersion}")!!.let { include(it) }

    modImplementation("org.reactivestreams:reactive-streams:1.0.4")!!.let { include(it) }
    modImplementation("io.projectreactor:reactor-core:3.6.5")!!.let { include(it) }
    modImplementation("com.networknt:json-schema-validator:1.5.7")!!.let { include(it) }

    val geminiVersion = properties["gemini_version"] as String
    modImplementation("com.google.genai:google-genai:${geminiVersion}")!!.let { include(it) }
    modImplementation("com.squareup.okhttp3:okhttp:4.12.0")!!.let { include(it) }
    modImplementation("com.squareup.okio:okio-jvm:3.6.0")!!.let { include(it) }
    modImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")!!.let { include(it) }
    modImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")!!.let { include(it) }
    modImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10")!!.let { include(it) }
    modImplementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")!!.let { include(it) }
    modImplementation("com.fasterxml.jackson.core:jackson-core:2.18.3")!!.let { include(it) }
    modImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")!!.let { include(it) }
    modImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.3")!!.let { include(it) }
    modImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")!!.let { include(it) }

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    test {
        useJUnitPlatform()
    }
}
