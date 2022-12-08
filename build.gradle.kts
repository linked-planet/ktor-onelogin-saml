import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// print gradle version as we might run on different machines as well as in the cloud
println("Gradle Version: " + GradleVersion.current().toString())

val libraryVersion = scmVersion.version
group = "com.linked-planet"
version = libraryVersion

val kotlinVersion = "1.7.21"
val ktorVersion = "2.1.3"
val jvmTarget = "1.8"

plugins {
    kotlin("jvm") version "1.7.21"
    id("org.jetbrains.dokka") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("nu.studer.credentials") version "2.1"
    id("pl.allegro.tech.build.axion-release") version "1.14.2"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("signing")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    api(group = "io.ktor", name = "ktor-server-jetty", version = ktorVersion)
    api(group = "io.ktor", name = "ktor-server-locations", version = ktorVersion)
    api(group = "io.ktor", name = "ktor-server-html-builder", version = ktorVersion)
    api(group = "com.onelogin", name = "java-saml", version = "2.9.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jvmTarget
    kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
}

signing {
    useInMemoryPgpKeys(
        findProperty("signingKey").toString(),
        findProperty("signingPassword").toString()
    )

    sign(publishing.publications)
}

publishing {
    publications {
        create<MavenPublication>("ktor-onelogin-saml") {
            from(components["kotlin"])
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

tasks {
    register("dokkaJavadoc", DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
        configuration.reportUndocumented = false
    }
    register("javadocJar", Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/javadoc")
    }
    register("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from("src/main/kotlin")
    }
}
