package com.teammgmt.application.service

import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.AlreadyPartOfTheTeam
import com.teammgmt.domain.model.DomainEventPublisher
import com.teammgmt.domain.model.PeopleFinder
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamNotFound
import com.teammgmt.domain.model.TeamRepository
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class AddTeamMemberServiceShould {

    private val teamRepository = mockk<TeamRepository>()

    private val peopleFinder = mockk<PeopleFinder>()

    private val domainEventPublisher = mockk<DomainEventPublisher>(relaxed = true)

    private val addTeamMember = AddTeamMemberService(teamRepository, peopleFinder, domainEventPublisher)

    @Test
    fun `add a new member to a team`() {
        val team = buildTeam()
        val person = buildPerson()
        every { teamRepository.find(team.teamId) } returns team.right()
        every { peopleFinder.find(person.personId) } returns person.right()
        every { teamRepository.save(any()) } returns team.right()

        val result = addTeamMember(AddTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(Unit.right())
        verify { domainEventPublisher.publish(TeamMemberJoined(team, person.personId)) }
    }

    @Test
    fun `fail when team does not exists`() {
        val team = buildTeam()
        val person = buildPerson()
        every { peopleFinder.find(person.personId) } returns person.right()
        every { teamRepository.find(team.teamId) } returns TeamNotFound.left()

        val result = addTeamMember(AddTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(TeamNotFound.left())
    }

    @Test
    fun `fail when person does not exists`() {
        val team = buildTeam()
        val person = buildPerson()
        every { teamRepository.find(team.teamId) } returns team.right()
        every { peopleFinder.find(person.personId) } returns PersonNotFound.left()

        val result = addTeamMember(AddTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(PersonNotFound.left())
    }

    @Test
    fun `fail when person is already part of the team`() {
        val person = buildPerson()
        val team = buildTeam(members = setOf(TeamMember(person.personId)))
        every { teamRepository.find(team.teamId) } returns team.right()
        every { peopleFinder.find(person.personId) } returns person.right()

        val result = addTeamMember(AddTeamMemberRequest(team.teamId.value, person.personId.value))

        assertThat(result).isEqualTo(AlreadyPartOfTheTeam.left())
    }
}
