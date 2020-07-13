import groovy.lang.Closure
import nu.studer.gradle.credentials.domain.CredentialsContainer
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// print gradle version as we might run on different machines as well as in the cloud
println("Gradle Version: " + GradleVersion.current().toString())

group = "com.link-time.ktor"
version = "1.2.0-SNAPSHOT"

val kotlinVersion = "1.3.72"
val ktorVersion = "1.2.3"
val jvmTarget = "1.8"

plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.dokka") version "0.9.18"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("com.bmuschko.nexus") version "2.3.1"
    id("io.codearte.nexus-staging") version "0.21.0"
    id("nu.studer.credentials") version "1.0.7"
    `maven-publish`
}

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("http://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("http://dl.bintray.com/kotlin/kotlin-eap") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    api(group = "io.ktor", name = "ktor-server-jetty", version = ktorVersion)
    api(group = "com.onelogin", name = "java-saml", version = "2.4.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jvmTarget
    kotlinOptions.freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
}

// ability to publish artifact to maven local
publishing {
    publications {
        register("mavenJava", MavenPublication::class.java) {
            from(components["kotlin"])
        }
    }
}

// obtain passwords from gradle credentials plugin
gradle.taskGraph.whenReady {
    if (allTasks.any { it is Sign }) {
        val credentials: CredentialsContainer by ext
        allprojects {
            extra["signing.password"] = credentials.getProperty("signingPassword")
        }
    }
    if (allTasks.any {
                it.name in setOf("uploadArchives", "closeRepository", "releaseRepository", "closeAndReleaseRepository")
            }) {
        val credentials: CredentialsContainer by ext
        allprojects {
            extra["nexusPassword"] = credentials.getProperty("nexusToken")
        }
    }
}

// add pom metadata needed for publishing to maven central
val modifyPom: Closure<MavenPom> by ext
modifyPom(closureOf<MavenPom> {
    project {
        withGroovyBuilder {
            "name"("Ktor OneLogin SAML Integration")
            "description"("Integrates Ktor with OneLogin java-saml library.")
            "url"("https://github.com/link-time/ktor-onelogin-saml")
            "inceptionYear"("2019")
            "licenses" {
                "license" {
                    "name"("The Apache Software License, Version 2.0")
                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    "distribution"("repo")
                }
            }
            "developers" {
                "developer" {
                    "name"("Alexander Weickmann")
                    "email"("alexander.weickmann@gmail.com")
                    "url"("https://github.com/weickmanna")
                    "organization"("link-time GmbH")
                    "organizationUrl"("https://link-time.com")
                }
            }
            "scm" {
                "url"("https://github.com/link-time/ktor-onelogin-saml.git")
                "connection"("scm:git:git://github.com/link-time/ktor-onelogin-saml.git")
                "developerConnection"("scm:git:git://github.com/link-time/ktor-onelogin-saml.git")
            }
        }
    }
})

// disable archives set by nexus plugin, since we use Kotlin we have to create our own ones
extraArchive {
    sources = false
    tests = false
    javadoc = false
}

tasks {
    register("dokkaJavadoc", DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
        reportUndocumented = false
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
