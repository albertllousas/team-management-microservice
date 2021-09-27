package com.teammgmt.application.service

import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamMemberLeft
import com.teammgmt.domain.model.TeamMemberNotFound
import com.teammgmt.domain.model.TeamNotFound
import com.teammgmt.domain.model.TeamRepository
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RemoveTeamMemberServiceShould {

    private val teamRepository = mockk<TeamRepository>()

    private val domainEventPublisher = mockk<DomainEventPublisher>(relaxed = true)

    private val removeTeamMember = RemoveTeamMemberService(teamRepository, domainEventPublisher)

    @Test
    fun `remove a member from a team`() {
        val person = buildPerson()
        val team = buildTeam()
        val teamWithPerson = team.copy(members = setOf(TeamMember(person.personId)))
        every { teamRepository.find(team.teamId) } returns teamWithPerson.right()
        every { teamRepository.save(team) } returns team.right()

        val result = removeTeamMember(RemoveTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(Unit.right())
        verify { domainEventPublisher.publish(TeamMemberLeft(team, person.personId)) }
    }

    @Test
    fun `fail when team does not exists`() {
        val team = buildTeam()
        val person = buildPerson()
        every { teamRepository.find(team.teamId) } returns TeamNotFound.left()

        val result = removeTeamMember(RemoveTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(TeamNotFound.left())
    }

    @Test
    fun `fail when person is not part of this team`() {
        val person = buildPerson()
        val team = buildTeam()
        every { teamRepository.find(team.teamId) } returns team.right()

        val result = removeTeamMember(RemoveTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(TeamMemberNotFound.left())
    }
}
