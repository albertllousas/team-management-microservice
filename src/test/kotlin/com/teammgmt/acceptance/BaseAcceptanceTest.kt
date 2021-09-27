package com.teammgmt.acceptance

import com.github.javafaker.Faker
import com.github.tomakehurst.wiremock.WireMockServer
import com.teammgmt.App
import com.teammgmt.Kafka
import com.teammgmt.Postgres
import com.teammgmt.acceptance.BaseAcceptanceTest.Initializer
import com.teammgmt.fixtures.buildKafkaConsumer
import com.teammgmt.fixtures.buildKafkaProducer
import com.teammgmt.infrastructure.adapters.inbound.stream.PersonJoinedEvent
import com.teammgmt.infrastructure.configuration.InfrastructureConfiguration
import io.restassured.RestAssured
import io.restassured.http.ContentType
import kotlinx.coroutines.delay
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [Initializer::class], classes = [App::class])
abstract class BaseAcceptanceTest {

    private val faker = Faker()

    init {
        RestAssured.defaultParser = io.restassured.parsing.Parser.JSON
    }

    @LocalServerPort
    protected val servicePort: Int = 0

    protected val mapper = InfrastructureConfiguration().defaultObjectMapper()

    protected val kafkaConsumer = buildKafkaConsumer(kafka.container.bootstrapServers)

    protected val kafkaProducer = buildKafkaProducer(kafka.container.bootstrapServers)

    protected val wireMockServer = WireMockServer()

    companion object {

        val postgres = Postgres()
        val kafka = Kafka()
    }

    @BeforeEach
    fun setUp() {
        wireMockServer.resetAll()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.container.jdbcUrl,
                "spring.datasource.password=" + postgres.container.password,
                "spring.datasource.username=" + postgres.container.username,
                "spring.kafka.bootstrap-servers=" + kafka.container.bootstrapServers,
                "spring.flyway.url=" + postgres.container.jdbcUrl,
                "spring.flyway.password=" + postgres.container.password,
                "spring.flyway.username=" + postgres.container.username,
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    protected suspend fun givenAPersonExists(): UUID {
        val joinedEvent = PersonJoinedEvent(UUID.randomUUID(), "John", "Doe", LocalDateTime.now())
        kafkaProducer
            .send(
                ProducerRecord(
                    "public.person.v1",
                    joinedEvent.personId.toString(),
                    mapper.writeValueAsBytes(joinedEvent)
                )
            )
        delay(2000) // could be improved
        return joinedEvent.personId
    }

    protected fun givenATeamExists(): UUID {
        val id: String = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""{ "name": "${faker.team().name()}" }""")
            .port(servicePort)
            .`when`()
            .post("/teams")
            .then()
            .extract()
            .path("id")
        return UUID.fromString(id)
    }

    protected fun givenAPersonPartOfTheTeam(teamId: UUID, personId: UUID) {
        RestAssured.given()
            .port(servicePort)
            .`when`()
            .put("/teams/$teamId/person/$personId")
            .then()
    }
}
