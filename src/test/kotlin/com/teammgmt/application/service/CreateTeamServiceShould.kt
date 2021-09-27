package com.teammgmt.application.service

import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamNameAlreadyTaken
import com.teammgmt.domain.model.TeamRepository
import com.teammgmt.fixtures.buildTeam
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateTeamServiceShould {

    private val teamRepository = mockk<TeamRepository>()

    private val generateId = mockk<() -> UUID>()

    private val domainEventPublisher = mockk<DomainEventPublisher>(relaxed = true)

    private val createTeam = CreateTeamService(teamRepository, domainEventPublisher, generateId)

    @Test
    fun `create a team successfully`() {
        val team = buildTeam()
        every { generateId() } returns team.teamId.value
        every { teamRepository.save(team) } returns team.right()

        val result = createTeam(CreateTeamRequest(team.teamName.value))

        assertThat(result).isEqualTo(CreateTeamResponse(team.teamId.value).right())
        verify { domainEventPublisher.publish(TeamCreated(team)) }
    }

    @Test
    fun `fail when saving fails`() {
        val team = buildTeam()
        every { generateId() } returns team.teamId.value
        every { teamRepository.save(team) } returns TeamNameAlreadyTaken.left()

        val result = createTeam(CreateTeamRequest(team.teamName.value))

        assertThat(result).isEqualTo(TeamNameAlreadyTaken.left())
    }
}
