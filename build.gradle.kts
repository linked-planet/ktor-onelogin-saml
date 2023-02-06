println("Gradle Version: " + GradleVersion.current().toString())

plugins {
    kotlin("jvm") version "1.8.10"

    // derive gradle version from git tag
    id("pl.allegro.tech.build.axion-release") version "1.14.3"

    // publishing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("signing")
    `maven-publish`

    // provide & configure dependencyUpdates
    id("com.github.ben-manes.versions") version "0.45.0"
    id("se.ascp.gradle.gradle-versions-filter") version "0.1.16"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = "com.linked-planet"
version = scmVersion.version

repositories {
    mavenCentral()
}

val kotlinVersion = "1.8.10"
val ktorVersion = "2.2.3"
val oneloginVersion = "2.9.0"
dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    api("io.ktor", "ktor-server-jetty", ktorVersion)
    api("io.ktor", "ktor-server-locations", ktorVersion)
    api("io.ktor", "ktor-server-html-builder", ktorVersion)
    api("com.onelogin", "java-saml", oneloginVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
}

signing {
    useInMemoryPgpKeys(
        findProperty("signingKey").toString(),
        findProperty("signingPassword").toString()
    )

    sign(publishing.publications)
}


tasks {
    register("javadocJar", Jar::class) {
        archiveClassifier.set("javadoc")
        from("$buildDir/javadoc")
    }
    register("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from("src/main/kotlin")
    }
}

publishing {
    publications {
        create<MavenPublication>("ktor-onelogin-saml") {
            from(components["kotlin"])

            version = scmVersion.version

            artifact(tasks.getByName<Zip>("javadocJar"))
            artifact(tasks.getByName<Zip>("sourcesJar"))

            pom {
                name.set("Ktor OneLogin SAML Integration")
                description.set("Integrates Ktor with OneLogin java-saml library.")
                url.set("https://github.com/linked-planet/ktor-onelogin-saml")
                inceptionYear.set("2019")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("Alexander Weickmann")
                        email.set("alexander.weickmann@gmail.com")
                        url.set("https://github.com/weickmanna")
                        organization.set("linked-planet GmbH")
                        organizationUrl.set("https://linked-planet.com")
                    }
                }
                scm {
                    url.set("https://github.com/linked-planet/ktor-onelogin-saml.git")
                    connection.set("scm:git:git://github.com/linked-planet/ktor-onelogin-saml.git")
                    developerConnection.set("scm:git:git://github.com/linked-planet/ktor-onelogin-saml.git")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

// do not generate extra load on Nexus with new staging repository if signing fails
val initializeSonatypeStagingRepository by tasks.existing
initializeSonatypeStagingRepository {
    shouldRunAfter(tasks.withType<Sign>())
}
