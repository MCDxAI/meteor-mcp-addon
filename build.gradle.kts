plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = libs.versions.mod.version.get()
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
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Meteor
    modImplementation(libs.meteor.client)

    // StarScript (separate dependency, not bundled in Meteor)
    implementation(libs.starscript)

    // MCP SDK
    modImplementation(libs.mcp)
    include(libs.mcp)
    modImplementation(libs.mcpCore)
    include(libs.mcpCore)
    modImplementation(libs.mcpJson)
    include(libs.mcpJson)
    modImplementation(libs.mcpJsonJackson2)
    include(libs.mcpJsonJackson2)

    modImplementation(libs.reactiveStreams)
    include(libs.reactiveStreams)
    modImplementation(libs.reactorCore)
    include(libs.reactorCore)
    modImplementation(libs.jsonSchemaValidator)
    include(libs.jsonSchemaValidator)

    // Gemini AI
    modImplementation(libs.gemini)
    include(libs.gemini)
    modImplementation(libs.okhttp)
    include(libs.okhttp)
    modImplementation(libs.okio)
    include(libs.okio)
    modImplementation(libs.kotlinStdlib)
    include(libs.kotlinStdlib)
    modImplementation(libs.kotlinStdlibJdk8)
    include(libs.kotlinStdlibJdk8)
    modImplementation(libs.kotlinStdlibJdk7)
    include(libs.kotlinStdlibJdk7)
    modImplementation(libs.jacksonAnnotations)
    include(libs.jacksonAnnotations)
    modImplementation(libs.jacksonCore)
    include(libs.jacksonCore)
    modImplementation(libs.jacksonDatabind)
    include(libs.jacksonDatabind)
    modImplementation(libs.jacksonDatatypeJdk8)
    include(libs.jacksonDatatypeJdk8)
    modImplementation(libs.jacksonDatatypeJsr310)
    include(libs.jacksonDatatypeJsr310)

    // Testing
    testImplementation(libs.junitApi)
    testRuntimeOnly(libs.junitEngine)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get()
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
