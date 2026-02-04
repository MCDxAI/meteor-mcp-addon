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
    include(modImplementation(libs.mcp.get())!!)
    include(modImplementation(libs.mcpCore.get())!!)
    include(modImplementation(libs.mcpJson.get())!!)
    include(modImplementation(libs.mcpJsonJackson2.get())!!)

    include(modImplementation(libs.reactiveStreams.get())!!)
    include(modImplementation(libs.reactorCore.get())!!)
    include(modImplementation(libs.jsonSchemaValidator.get())!!)

    // Gemini AI
    include(modImplementation(libs.gemini.get())!!)
    include(modImplementation(libs.okhttp.get())!!)
    include(modImplementation(libs.okio.get())!!)
    include(modImplementation(libs.kotlinStdlib.get())!!)
    include(modImplementation(libs.kotlinStdlibJdk8.get())!!)
    include(modImplementation(libs.kotlinStdlibJdk7.get())!!)
    include(modImplementation(libs.jacksonAnnotations.get())!!)
    include(modImplementation(libs.jacksonCore.get())!!)
    include(modImplementation(libs.jacksonDatabind.get())!!)
    include(modImplementation(libs.jacksonDatatypeJdk8.get())!!)
    include(modImplementation(libs.jacksonDatatypeJsr310.get())!!)

    // Testing
    testImplementation(libs.junitApi)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
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
