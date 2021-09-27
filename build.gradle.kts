import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val verKotlin = "1.4.31"
    val verKotlinLinter = "3.3.0"
    val verSpring = "2.4.5"
    val verSpringManagement = "1.0.11.RELEASE"

    kotlin("jvm") version verKotlin
    application
    kotlin("plugin.spring") version verKotlin
    id("org.springframework.boot") version verSpring
    id("io.spring.dependency-management") version verSpringManagement
    id("org.jmailen.kotlinter") version verKotlinLinter
}

group = "com.alo.teammgmt"
version = "1.0.0"

object Versions {
    const val SPRINGDOC = "1.4.8"
    const val JUNIT = "5.7.0"
    const val MOCKK = "1.12.0"
    const val SPRING_MOCKK = "3.0.1"
    const val ASSERTJ = "3.20.2"
    const val SPRING_KAFKA = "2.7.0"
    const val TESTCONTAINERS = "1.16.0"
    const val ARROW = "0.12.1"
    const val POSTGRES = "42.2.23"
    const val FLYWAY = "7.11.2"
    const val RESILIENCE4J = "1.5.0"
    const val OKHTTP_LOGGING_INTERCEPTOR = "4.8.0"
    const val RETROFIT = "2.9.0"
    const val WIREMOCK = "2.27.2"
    const val COROUTINES = "1.3.8"
    const val REST_ASSURED = "4.2.0"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.arrow-kt:arrow-core:${Versions.ARROW}")
    implementation("io.arrow-kt:arrow-fx:${Versions.ARROW}")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.kafka:spring-kafka:${Versions.SPRING_KAFKA}")
    implementation("org.springdoc:springdoc-openapi-ui:${Versions.SPRINGDOC}")
    implementation("org.springdoc:springdoc-openapi-kotlin:${Versions.SPRINGDOC}")
    implementation("org.postgresql:postgresql:${Versions.POSTGRES}")
    implementation("org.flywaydb:flyway-core:${Versions.FLYWAY}")
    implementation("org.flywaydb:flyway-gradle-plugin:${Versions.FLYWAY}")
    implementation(group = "io.github.resilience4j", name = "resilience4j-spring-boot2", version = Versions.RESILIENCE4J)
    implementation(group = "io.github.resilience4j", name = "resilience4j-micrometer", version = Versions.RESILIENCE4J)
    implementation(group = "io.github.resilience4j", name = "resilience4j-retrofit", version = Versions.RESILIENCE4J)
    implementation(group = "io.github.resilience4j", name = "resilience4j-circuitbreaker", version = Versions.RESILIENCE4J)
    implementation(group = "com.squareup.okhttp3", name = "logging-interceptor", version = Versions.OKHTTP_LOGGING_INTERCEPTOR)
    implementation(group = "com.squareup.retrofit2", name = "retrofit", version = Versions.RETROFIT)
    implementation(group = "com.squareup.retrofit2", name = "converter-jackson", version = Versions.RETROFIT)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = Versions.COROUTINES)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-reactor", version = Versions.COROUTINES)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(group = "com.ninja-squad", name = "springmockk", version = Versions.SPRING_MOCKK)
    testImplementation(group = "io.mockk", name = "mockk", version = Versions.MOCKK)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = Versions.JUNIT)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = Versions.JUNIT)
    testImplementation(group = "org.assertj", name = "assertj-core", version = Versions.ASSERTJ)
    testImplementation(group =  "org.testcontainers", name = "testcontainers", version = Versions.TESTCONTAINERS)
    testImplementation(group =  "org.testcontainers", name = "kafka", version = Versions.TESTCONTAINERS)
    testImplementation("org.testcontainers:postgresql:${Versions.TESTCONTAINERS}")
    testImplementation("com.github.javafaker:javafaker:1.0.2")
    testImplementation("com.github.tomakehurst:wiremock:${Versions.WIREMOCK}")
    testImplementation(group = "io.rest-assured", name = "rest-assured-all", version = Versions.REST_ASSURED)
    testImplementation(group = "io.rest-assured", name = "rest-assured", version = Versions.REST_ASSURED)

}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.teammgmt.App")
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xinline-classes")
        }
    }

    task<Test>("unitTest") {
        description = "Runs unit tests."
        useJUnitPlatform {
            excludeTags("integration")
            excludeTags("acceptance")
        }
        shouldRunAfter(test)
    }

    task<Test>("integrationTest") {
        description = "Runs integration tests."
        useJUnitPlatform {
            includeTags("integration")
        }
        shouldRunAfter(test)
    }

    task<Test>("acceptanceTest") {
        description = "Runs acceptance tests."
        useJUnitPlatform {
            includeTags("acceptance")
        }
        shouldRunAfter(test)
    }
}
