package com.teammgmt.acceptance

import com.fasterxml.jackson.module.kotlin.readValue
import com.teammgmt.fixtures.consumeAndAssertMultiple
import com.teammgmt.infrastructure.adapters.outbound.event.IntegrationEvent
import io.restassured.RestAssured.given
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@Tag("acceptance")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AddTeamMemberToTeamShould : BaseAcceptanceTest() {

    @Test
    fun `add a team member to a team successfully`() {
        runBlocking {
            val teamId = givenATeamExists()
            val personId = givenAPersonExists()

            val response = given()
                .port(servicePort)
                .`when`()
                .put("/teams/$teamId/person/$personId")
                .then()

            assertThat(response.extract().statusCode()).isEqualTo(204)
            kafkaConsumer.consumeAndAssertMultiple(stream = "public.team.v1", numberOfMessages = 2) { records ->
                assertThat(records[1].key()).isEqualTo(teamId.toString())
                assertDoesNotThrow { mapper.readValue<IntegrationEvent.TeamMemberJoined>(records[1].value()) }
            }
        }
    }
}
