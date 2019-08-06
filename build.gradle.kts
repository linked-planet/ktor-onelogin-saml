import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

println("Gradle Version: " + GradleVersion.current().toString())

group = "com.linktime.ktor"
version = "1.0.0-SNAPSHOT"

val kotlinVersion = "1.3.50-eap-54"
val ktorVersion = "1.1.3"
val jvmTarget = "1.8"

plugins {
    kotlin("jvm") version "1.3.50-eap-54"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("com.bmuschko.nexus") version "2.3.1"
    id("io.codearte.nexus-staging") version "0.21.0"
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

nexus {
}

nexusStaging {
}
