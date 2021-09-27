package com.teammgmt.acceptance

import com.fasterxml.jackson.module.kotlin.readValue
import com.teammgmt.fixtures.consumeAndAssert
import com.teammgmt.infrastructure.adapters.outbound.event.IntegrationEvent.TeamCreatedEvent
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@Tag("acceptance")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CreateTeamShould : BaseAcceptanceTest() {

    @Test
    fun `create a team successfully`() {
        val response = given()
            .contentType(ContentType.JSON)
            .body("""{ "name": "Hungry Hippos" }""")
            .port(servicePort)
            .`when`()
            .post("/teams")
            .then()

        assertThat(response.extract().statusCode()).isEqualTo(201)
        val userId: String = response.extract().path("id")
        kafkaConsumer.consumeAndAssert(stream = "public.team.v1") { record ->
            assertThat(record.key()).isEqualTo(userId)
            assertDoesNotThrow { mapper.readValue<TeamCreatedEvent>(record.value()) }
        }
    }
}
