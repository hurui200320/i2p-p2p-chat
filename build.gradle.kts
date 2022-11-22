import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"
    `maven-publish`
}

group = "info.skyblond"
version = "0.0.4"

repositories {
    mavenCentral()
}

dependencies {
    // i2p things, expose. (Need work with streaming client, etc.)
    api("net.i2p:i2p:1.9.1")
    api("net.i2p.client:streaming:1.9.1")
    // bencode for BitTorrent things
    implementation("com.dampcake:bencode:1.4")
    // kotlinx for json
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    // logger
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    testImplementation("ch.qos.logback:logback-classic:1.4.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val jarSources by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
}

val jarJavadoc by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("dist") {
            from(components["java"])
            artifact(jarSources)
            artifact(jarJavadoc)

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("${project.group}:${project.name}")
                description.set("A kotlin library for P2P chat over I2P network.")
                url.set("https://github.com/hurui200320/i2p-p2p-chat")
                licenses {
                    license {
                        name.set("GNU Affero General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/hurui200320/i2p-p2p-chat")
                    connection.set("scm:git:https://github.com/hurui200320/i2p-p2p-chat.git")
                    developerConnection.set("scm:git:ssh://git@github.com/hurui200320/i2p-p2p-chat.git")
                }
                developers {
                    developer {
                        id.set("hurui200320")
                        name.set("SkyBlond")
                        email.set("maven@skyblond.info")
                    }
                }
            }
        }
    }
}
