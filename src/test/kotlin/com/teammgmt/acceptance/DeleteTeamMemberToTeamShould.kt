package com.teammgmt.acceptance

import com.fasterxml.jackson.module.kotlin.readValue
import com.teammgmt.fixtures.buildKafkaProducer
import com.teammgmt.fixtures.consumeAndAssertMultiple
import com.teammgmt.infrastructure.adapters.inbound.stream.PersonLeftEvent
import com.teammgmt.infrastructure.adapters.outbound.event.IntegrationEvent
import io.restassured.RestAssured.given
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import java.time.LocalDateTime.now

@Tag("acceptance")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class DeleteTeamMemberToTeamShould : BaseAcceptanceTest() {

    @Test
    fun `delete a team member to a team successfully`() {
        runBlocking {
            val teamId = givenATeamExists()
            val personId = givenAPersonExists()
            givenAPersonPartOfTheTeam(teamId, personId)

            val response = given()
                .port(servicePort)
                .`when`()
                .delete("/teams/$teamId/person/$personId")
                .then()

            assertThat(response.extract().statusCode()).isEqualTo(204)
            kafkaConsumer.consumeAndAssertMultiple(stream = "public.team.v1", numberOfMessages = 3) { records ->
                assertThat(records[2].key()).isEqualTo(teamId.toString())
                assertDoesNotThrow { mapper.readValue<IntegrationEvent.TeamMemberLeft>(records[2].value()) }
            }
        }
    }

    @Test
    fun `delete a team member to a team successfully when a person leave`() {
        runBlocking {
            val teamId = givenATeamExists()
            val personId = givenAPersonExists()
            givenAPersonPartOfTheTeam(teamId, personId)

            val leftEvent = PersonLeftEvent(personId, now())
            buildKafkaProducer(kafka.container.bootstrapServers)
                .send(
                    ProducerRecord(
                        "public.person.v1",
                        leftEvent.personId.toString(),
                        mapper.writeValueAsBytes(leftEvent)
                    )
                )

            kafkaConsumer.consumeAndAssertMultiple(stream = "public.team.v1", numberOfMessages = 3) { records ->
                assertThat(records[2].key()).isEqualTo(teamId.toString())
                assertDoesNotThrow { mapper.readValue<IntegrationEvent.TeamMemberLeft>(records[2].value()) }
            }
        }
    }
}
