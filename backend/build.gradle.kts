buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.liquibase:liquibase-core:5.0.4")
    }
}

plugins {
    java
    id("org.springframework.boot") version "4.2.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.liquibase.gradle") version "3.1.1"
    id("jacoco")
    id("com.diffplug.spotless") version "8.11.0"
    checkstyle
    pmd
}

group = "com.vacancyscout"
version = "0.1.0"
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    // Reactive DB
    implementation("org.springframework.boot:spring-boot-starter-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    // Liquibase
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    // PDF / DOCX
    implementation("org.apache.pdfbox:pdfbox:3.0.8") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("org.apache.poi:poi-ooxml:6.0.0")
    // Validation / Actuator
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Liquibase runtime for the liquibase gradle plugin tasks (validate/update/…)
    liquibaseRuntime("org.liquibase:liquibase-core")
    liquibaseRuntime("org.postgresql:postgresql")
    liquibaseRuntime("info.picocli:picocli:4.7.7") // required for Liquibase 4.4+ CLI parser

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson-test")
    testImplementation("org.springframework.boot:spring-boot-starter-r2dbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-r2dbc")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.wiremock:wiremock:3.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

liquibase {
    activities.register("main") {
        arguments = mapOf(
            "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.xml",
            "url" to (findProperty("liquibaseUrl") ?: "jdbc:postgresql://localhost:5432/vacancy_scout").toString(),
            "username" to (findProperty("liquibaseUser") ?: "vacancy_scout").toString(),
            "password" to (findProperty("liquibasePassword") ?: "vacancy_scout").toString(),
            "driver" to "org.postgresql.Driver"
        )
    }
}

// Custom task to validate Liquibase changelogs
tasks.register<JavaExec>("validateChangelogs") {
    group = "liquibase"
    description = "Validate Liquibase changelog files"
    classpath = configurations.getByName("liquibaseRuntime")
    mainClass.set("liquibase.integration.commandline.Main")
    args("--changeLogFile", "src/main/resources/db/changelog/db.changelog-master.xml")
    args("--url", findProperty("liquibaseUrl") ?: "jdbc:postgresql://localhost:5432/vacancy_scout")
    args("--username", findProperty("liquibaseUser") ?: "vacancy_scout")
    args("--password", findProperty("liquibasePassword") ?: "vacancy_scout")
    args("validate")
}

// Jacoco for test coverage
tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.test)
    jvmArgs("-Dapi.version=1.40")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}

tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Spotless — code formatting (Google Java Style)
spotless {
    java {
        googleJavaFormat()
        target("src/**/*.java")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Checkstyle — style checks
checkstyle {
    toolVersion = "14.4.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxErrors = 0
    maxWarnings = 0
}
tasks.named("checkstyleMain") {
    dependsOn(tasks.named("spotlessCheck"))
}

// PMD — static analysis
pmd {
    toolVersion = "7.24.0"
    rulesMinimumPriority = 5
    ruleSetFiles = files(file("config/pmd/ruleset.xml"))
    isIgnoreFailures = false
}
tasks.named("pmdMain") {
    dependsOn(tasks.named("checkstyleMain"))
}
tasks.named("pmdTest") {
    enabled = false
}

// Make check depend on all lint tasks
tasks.named("check") {
    dependsOn(tasks.named("spotlessCheck"), tasks.named("checkstyleMain"), tasks.named("pmdMain"))
    dependsOn(tasks.named("integrationTest"))
}
