package com.teammgmt.infrastructure.adapters.inbound.http

import arrow.core.left
import arrow.core.right
import com.ninjasquad.springmockk.MockkBean
import com.teammgmt.application.service.AddTeamMemberRequest
import com.teammgmt.application.service.AddTeamMemberService
import com.teammgmt.application.service.CreateTeamRequest
import com.teammgmt.application.service.CreateTeamResponse
import com.teammgmt.application.service.CreateTeamService
import com.teammgmt.application.service.RemoveTeamMemberRequest
import com.teammgmt.application.service.RemoveTeamMemberService
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.domain.model.TeamValidationError.TooLongName
import io.mockk.every
import io.undertow.util.StatusCodes.CREATED
import io.undertow.util.StatusCodes.NO_CONTENT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.util.UUID.randomUUID

@Tag("integration")
@WebFluxTest(TeamsHttpController::class)
class TeamsHttpControllerShould(@Autowired val webTestClient: WebTestClient) {

    @MockkBean
    private lateinit var createTeam: CreateTeamService

    @MockkBean
    private lateinit var addTeamMember: AddTeamMemberService

    @MockkBean
    private lateinit var removeTeamMember: RemoveTeamMemberService

    @Nested
    inner class Create {

        @Test
        fun `should create a team`() {
            val teamId = randomUUID()
            every { createTeam(CreateTeamRequest("Hungry Hippos")) } returns CreateTeamResponse(teamId).right()

            val response = webTestClient
                .post()
                .uri("/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("""{ "name": "Hungry Hippos" }"""))
                .exchange()

            response
                .expectStatus().isCreated
                .expectBody().json("""{ "id": "$teamId" }""")
        }

        @Test
        fun `should fail when there is an error creating the team`() {
            every { createTeam(CreateTeamRequest("Hungry Hippos")) } returns TooLongName.left()

            val response = webTestClient
                .post()
                .uri("/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("""{ "name": "Hungry Hippos" }"""))
                .exchange()

            response.expectStatus().value {
                assertThat(it).isNotEqualTo(CREATED)
            }
        }
    }

    @Nested
    inner class AddTeamMember {

        @Test
        fun `should add a new team member to a team`() {
            val teamId = randomUUID()
            val personId = randomUUID()
            every { addTeamMember(AddTeamMemberRequest(teamId, personId)) } returns Unit.right()

            val response = webTestClient
                .put()
                .uri("/teams/$teamId/person/$personId")
                .exchange()

            response.expectStatus().isNoContent
        }

        @Test
        fun `should fail when there is an error creating the adding a team member`() {
            val teamId = randomUUID()
            val personId = randomUUID()
            every { addTeamMember(AddTeamMemberRequest(teamId, personId)) } returns PersonNotFound.left()

            val response = webTestClient
                .put()
                .uri("/teams/$teamId/person/$personId")
                .exchange()

            response.expectStatus().value {
                assertThat(it).isNotEqualTo(NO_CONTENT)
            }
        }
    }

    @Nested
    inner class RemoveTeamMember {

        @Test
        fun `should remove a team member from a team`() {
            val teamId = randomUUID()
            val personId = randomUUID()
            every { removeTeamMember(RemoveTeamMemberRequest(teamId, personId)) } returns Unit.right()

            val response = webTestClient
                .delete()
                .uri("/teams/$teamId/person/$personId")
                .exchange()

            response.expectStatus().isNoContent
        }

        @Test
        fun `should fail when there is an error creating the adding a team member`() {
            val teamId = randomUUID()
            val personId = randomUUID()
            every { removeTeamMember(RemoveTeamMemberRequest(teamId, personId)) } returns PersonNotFound.left()

            val response = webTestClient
                .delete()
                .uri("/teams/$teamId/person/$personId")
                .exchange()

            response.expectStatus().value {
                assertThat(it).isNotEqualTo(NO_CONTENT)
            }
        }
    }
}
