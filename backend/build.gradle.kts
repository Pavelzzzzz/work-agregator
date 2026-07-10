plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("org.liquibase.gradle") version "2.2.2"
    id("jacoco")
    id("com.diffplug.spotless") version "6.25.0"
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
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0-M2")
    }
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    // Reactive DB
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    // Liquibase
    implementation("org.liquibase:liquibase-core")
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    // AI (LM Studio on localhost)
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")
    // PDF / DOCX
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    // Validation / Actuator
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:r2dbc:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.wiremock:wiremock:3.3.1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

liquibase {
    activities.register("main") {
        arguments = mapOf(
            "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.xml",
            "url" to "jdbc:postgresql://localhost:5432/vacancy_scout",
            "username" to "vacancy_scout",
            "password" to "vacancy_scout"
        )
    }
}

// Jacoco for test coverage
tasks.test {
    useJUnitPlatform()
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
    toolVersion = "10.17.0"
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
    toolVersion = "7.1.0"
    rulesMinimumPriority = 5
    ruleSetFiles = files(file("config/pmd/ruleset.xml"))
    isIgnoreFailures = false
}
tasks.named("pmdMain") {
    dependsOn(tasks.named("checkstyleMain"))
}

// Make check depend on all lint tasks
tasks.named("check") {
    dependsOn(tasks.named("spotlessCheck"), tasks.named("checkstyleMain"), tasks.named("pmdMain"))
}
