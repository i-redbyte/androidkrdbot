plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "su.redbyte"
version = "1.0"

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("su.redbyte.androidkrdbot.cli.ChatCommissarKt")
}

kotlin {
    jvmToolchain(20)
}