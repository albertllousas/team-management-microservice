package com.teammgmt.infrastructure.adapters.inbound.http

import com.teammgmt.application.service.AddTeamMemberRequest
import com.teammgmt.application.service.AddTeamMemberService
import com.teammgmt.application.service.CreateTeamRequest
import com.teammgmt.application.service.CreateTeamService
import com.teammgmt.application.service.RemoveTeamMemberRequest
import com.teammgmt.application.service.RemoveTeamMemberService
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val createdWith = { body: CreateTeamHttpResponse -> ResponseEntity(body, CREATED) }

private val noContent = ResponseEntity<Any>(NO_CONTENT)

@RestController
class TeamsHttpController(
    private val createTeam: CreateTeamService,
    private val addTeamMember: AddTeamMemberService,
    private val removeTeamMember: RemoveTeamMemberService
) {

    @PostMapping("/teams")
    fun createInternalPayment(@RequestBody httpRequest: CreateTeamHttpRequest) =
        createTeam(CreateTeamRequest(teamName = httpRequest.name))
            .fold(ifLeft = { it.asHttpResponse() }, ifRight = { createdWith(CreateTeamHttpResponse(it.newTeamId)) })

    @PutMapping("/teams/{teamId}/person/{personId}")
    fun joinTeam(@PathVariable("teamId") teamId: UUID, @PathVariable("personId") personId: UUID) =
        addTeamMember(AddTeamMemberRequest(teamId, personId))
            .fold(ifLeft = { it.asHttpResponse() }, ifRight = { noContent })

    @DeleteMapping("/teams/{teamId}/person/{personId}")
    fun leaveTeam(@PathVariable("teamId") teamId: UUID, @PathVariable("personId") personId: UUID) =
        removeTeamMember(RemoveTeamMemberRequest(teamId, personId))
            .fold(ifLeft = { it.asHttpResponse() }, ifRight = { noContent })
}

data class CreateTeamHttpRequest(val name: String)

data class CreateTeamHttpResponse(val id: UUID)
