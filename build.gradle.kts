plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
//    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.github.ajalt.clikt:clikt:5.0.3")
//    implementation "org.jetbrains.kotlin:kotlin-stdlib"

//    testCompile 'org.jetbrains.kotlin:kotlin-test'
//    testCompile 'junit:junit:4.12'
}

//application {
//    mainClassName = 'jmj.deskumm.DeskummKt'
//}