package com.teammgmt.infrastructure.adapters.outbound.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.Team
import com.teammgmt.domain.model.TeamId
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamName
import com.teammgmt.domain.model.TeamNameAlreadyTaken
import com.teammgmt.domain.model.TeamNotFound
import com.teammgmt.domain.model.TeamRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import java.sql.ResultSet
import java.util.UUID
import kotlin.streams.toList

class PostgresTeamRepository(private val jdbcTemplate: JdbcTemplate) : TeamRepository {

    override fun find(teamId: TeamId): Either<TeamNotFound, Team> = try {
        jdbcTemplate.queryForObject(""" SELECT id, name, members FROM team WHERE id = ? FOR UPDATE """, teamId.value) { rs, _ -> rs.asTeam() }.right()
    } catch (exception: EmptyResultDataAccessException) {
        TeamNotFound.left()
    }

    override fun save(team: Team): Either<TeamNameAlreadyTaken, Team> = try {
        val members: Array<String> = team.members.map { it.personId.value.toString() }.toTypedArray()
        jdbcTemplate.update(
            """
            INSERT INTO team (id, name, members) VALUES (?,?,?)
            ON CONFLICT (id) DO UPDATE SET members = ?, name = ?
            """,
            team.teamId.value,
            team.teamName.value,
            members,
            members,
            team.teamName.value,
        ).let { team.right() }
    } catch (exception: DuplicateKeyException) {
        TeamNameAlreadyTaken.left()
    }

    fun findAllTeamsFor(personId: PersonId): List<Team> =
        jdbcTemplate.queryForStream(
            """
                SELECT id, members, name
                FROM team
                WHERE '${personId.value}' =  ANY (members)
              """
        ) { rs, _ -> rs.asTeam() }.toList()

    private fun ResultSet.asTeam() = Team(
        teamId = TeamId(UUID.fromString(getString("id"))),
        teamName = TeamName.reconstitute(getString("name")),
        members = (getArray("members").array as Array<String>).map {
            TeamMember(PersonId(UUID.fromString(it)))
        }.toSet()
    )
}
