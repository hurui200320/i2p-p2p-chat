import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
}

group = "info.skyblond.i2p"
version = "0.0.1"

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    // i2p things
    implementation("net.i2p:i2p:1.9.1")
    implementation("net.i2p.client:streaming:1.9.1")
    // bencode for BitTorrent things
    implementation("com.dampcake:bencode:1.4")
    // kotlinx for json
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    // logger
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("ch.qos.logback:logback-classic:1.4.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
