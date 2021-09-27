package com.teammgmt.infrastructure.adapters.outbound.db

import arrow.core.left
import arrow.core.right
import com.teammgmt.Postgres
import com.teammgmt.domain.model.Team
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamName
import com.teammgmt.domain.model.TeamNameAlreadyTaken
import com.teammgmt.domain.model.TeamNotFound
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID.randomUUID

@Tag("integration")
class PostgresTeamRepositoryShould {

    private val database = Postgres()

    private val jdbcTemplate = JdbcTemplate(database.datasource)

    private val teamRepository = PostgresTeamRepository(jdbcTemplate)

    @AfterEach
    fun `tear down`() {
        database.container.stop()
    }

    @Nested
    inner class Find {

        @Test
        fun `find a team`() {
            val team = buildTeam().also(teamRepository::save)

            val result = teamRepository.find(team.teamId)

            assertThat(result).isEqualTo(team.right())
        }

        @Test
        fun `not find a team when it does not exists`() {
            val team = buildTeam()

            val result = teamRepository.find(team.teamId)

            assertThat(result).isEqualTo(TeamNotFound.left())
        }
    }

    @Nested
    inner class Save {

        @Test
        fun `save a team`() {
            val team = buildTeam()

            val result = teamRepository.save(team)

            assertThat(result).isEqualTo(team.right())
            jdbcTemplate.queryForObject(
                """SELECT * FROM team WHERE id = '${team.teamId.value}'"""
            ) { it, _ ->
                assertThat(it.getString("id")).isEqualTo(team.teamId.value.toString())
                assertThat(it.getString("name")).isEqualTo(team.teamName.value)
                assertThat(it.getString("members")).isEqualTo("")
            }
        }

        @Test
        fun `update the team when it already exists`() {
            val team = buildTeam()
            teamRepository.save(team)
            val updatedTeam = team.copy(teamName = TeamName.reconstitute("teletubbies"))

            val result = teamRepository.save(updatedTeam)

            assertThat(result).isEqualTo(updatedTeam.right())
            jdbcTemplate.queryForObject(
                """SELECT * FROM team WHERE id = '${team.teamId.value}'"""
            ) { it, _ ->
                assertThat(it.getString("id")).isEqualTo(team.teamId.value.toString())
                assertThat(it.getString("name")).isEqualTo(updatedTeam.teamName.value)
                assertThat(it.getString("members")).isEqualTo("")
            }
        }

        @Test
        fun `fail saving a team when the name is already taken by another one`() {
            val team = buildTeam(teamName = "Teletubbies")
            val teamWithAlreadyTakenName = buildTeam(teamId = randomUUID(), teamName = "Teletubbies")
            teamRepository.save(team)

            val result = teamRepository.save(teamWithAlreadyTakenName)

            assertThat(result).isEqualTo(TeamNameAlreadyTaken.left())
        }
    }

    @Nested
    inner class FindTeamsForAPerson {

        @Test
        fun `find all teams for a person`() {
            val john = buildPerson(fullName = "John Doe")
            val jane = buildPerson(personId = randomUUID(), fullName = "Jane Doe")
            val firstJohnTeam =
                buildTeam(teamName = "first", teamId = randomUUID(), members = setOf(TeamMember(john.personId)))
                    .also(teamRepository::save)
            val secondJohnTeam =
                buildTeam(teamName = "second", teamId = randomUUID(), members = setOf(TeamMember(john.personId)))
                    .also(teamRepository::save)
            val janeTeam =
                buildTeam(teamName = "third", teamId = randomUUID(), members = setOf(TeamMember(jane.personId)))
                    .also(teamRepository::save)

            val result = teamRepository.findAllTeamsFor(john.personId)

            assertThat(result).isEqualTo(listOf(firstJohnTeam, secondJohnTeam))
        }

        @Test
        fun `not find teams for a person when they are not belonging to any`() {
            val person = buildPerson()

            val result = teamRepository.findAllTeamsFor(person.personId)

            assertThat(result).isEqualTo(emptyList<Team>())
        }
    }
}
