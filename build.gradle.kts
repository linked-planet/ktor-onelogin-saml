import groovy.lang.Closure
import nu.studer.gradle.credentials.domain.CredentialsContainer
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// print gradle version as we might run on different machines as well as in the cloud
println("Gradle Version: " + GradleVersion.current().toString())

group = "com.linked-planet.ktor"
version = "1.2.1-ktor-1.4.2"

val kotlinVersion = "1.4.0"
val ktorVersion = "1.4.2"
val jvmTarget = "1.8"

plugins {
    kotlin("jvm") version "1.3.72"
    id("org.jetbrains.dokka") version "0.10.1"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("com.bmuschko.nexus") version "2.3.1"
    id("io.codearte.nexus-staging") version "0.21.2"
    id("nu.studer.credentials") version "2.1"
    id("signing")
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
    api(group = "io.ktor", name = "ktor-locations", version = ktorVersion)
    api(group = "io.ktor", name = "ktor-html-builder", version = ktorVersion)
    api(group = "com.onelogin", name = "java-saml", version = "2.4.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jvmTarget
    kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
}

// ability to publish artifact to maven local
publishing {
    publications {
        register("mavenJava", MavenPublication::class.java) {
            from(components["kotlin"])
        }
    }
}

signing {
    useGpgCmd()
    sign(project.configurations[Dependency.ARCHIVES_CONFIGURATION])
    sign(publishing.publications["mavenJava"])
}

// obtain passwords from gradle credentials plugin
gradle.taskGraph.whenReady {
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
            "url"("https://github.com/linked-planet/ktor-onelogin-saml")
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
                    "organization"("linked-planet GmbH")
                    "organizationUrl"("https://linked-planet.com")
                }
            }
            "scm" {
                "url"("https://github.com/linked-planet/ktor-onelogin-saml.git")
                "connection"("scm:git:git://github.com/linked-planet/ktor-onelogin-saml.git")
                "developerConnection"("scm:git:git://github.com/linked-planet/ktor-onelogin-saml.git")
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

nexus {
    // disable nexus plugin signing, because we sign by ourselves, so we can get the private key password prompt
    // instead of having to store the private key via credentials plugin
    sign = false
}

nexusStaging {
    stagingProfileId = "21e9ad4c172f4"
    delayBetweenRetriesInMillis = 12000
    numberOfRetries = 30
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

tasks["closeRepository"].dependsOn("uploadArchives")
tasks["uploadArchives"].mustRunAfter("signMavenJavaPublication")
tasks["signMavenJavaPublication"].mustRunAfter("signArchives")
tasks["signArchives"].mustRunAfter("build")
