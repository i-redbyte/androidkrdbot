plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "su.redbyte"
version = "1.0"

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
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