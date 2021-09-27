package com.teammgmt.domain.model

import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.TeamValidationError.TooLongName
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class TeamShould {

    @Nested
    inner class CreateTeam {

        @Test
        fun `create a team`() {
            val teamId = UUID.randomUUID()
            val teamName = "Teletubbies"

            val result = Team.create(teamId, teamName)

            assertThat(result).isEqualTo(buildTeam(teamId, teamName).right())
        }

        @Test
        fun `fail creating a team`() {
            val teamName = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut 
                labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris 
                nisi ut aliquip ex ea commodo consequat.
                """

            val result = Team.create(UUID.randomUUID(), teamName)

            assertThat(result).isEqualTo(TooLongName.left())
        }
    }

    @Nested
    inner class JoinTeam {

        @Test
        fun `allow a person to join the team`() {
            val team = buildTeam()
            val person = buildPerson()

            val result = team.join(person)

            assertThat(result).isEqualTo(team.copy(members = setOf(TeamMember(person.personId))).right())
        }

        @Test
        fun `fail adding a person that was already part of the team`() {
            val person = buildPerson()
            val team = buildTeam(members = setOf(TeamMember(person.personId)))

            val result = team.join(person)

            assertThat(result).isEqualTo(AlreadyPartOfTheTeam.left())
        }
    }

    @Nested
    inner class LeaveTeam {

        @Test
        fun `allow a person to leave the team`() {
            val person = buildPerson()
            val team = buildTeam(members = setOf(TeamMember(person.personId)))

            val result = team.leave(person.personId)

            assertThat(result).isEqualTo(team.copy(members = emptySet()).right())
        }

        @Test
        fun `fail removing person that was not part of the team`() {
            val person = buildPerson()
            val team = buildTeam()

            val result = team.leave(person.personId)

            assertThat(result).isEqualTo(TeamMemberNotFound.left())
        }
    }
}
