package com.teammgmt.infrastructure.adapters.outbound.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.FullName
import com.teammgmt.domain.model.PeopleFinder
import com.teammgmt.domain.model.Person
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.infrastructure.adapters.outbound.db.PersonStatus.DELETED
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import java.time.LocalDateTime
import java.util.UUID

class PostgresPeopleReplicationRepository(private val jdbcTemplate: JdbcTemplate) : PeopleFinder {

    override fun find(personId: PersonId): Either<PersonNotFound, Person> = try {
        jdbcTemplate.queryForObject(
            """ 
            SELECT id, status, first_name, last_name, joined_at FROM people_replication WHERE id = ? AND status = 'ACTIVE' 
            """,
            personId.value
        ) { rs, _ ->
            Person(
                personId = PersonId(UUID.fromString(rs.getString("id"))),
                fullName = FullName.create(rs.getString("first_name"), rs.getString("last_name"))
            )
        }.right()
    } catch (exception: EmptyResultDataAccessException) {
        PersonNotFound.left()
    }

    fun save(person: PersonReplicationInfo) {
        jdbcTemplate.update(
            """
            INSERT INTO people_replication (id, status, first_name, last_name, joined_at) VALUES (?,?,?,?,?)
            ON CONFLICT (id) DO UPDATE SET status = ?, first_name = ?, last_name = ?, joined_at = ?
            """,
            person.personId,
            person.status.name,
            person.firstName,
            person.lastname,
            person.joinedAt,
            person.status.name,
            person.firstName,
            person.lastname,
            person.joinedAt
        )
    }

    fun delete(personId: UUID) {
        jdbcTemplate.update("UPDATE people_replication SET status = ? where id = ?", DELETED.name, personId)
    }
}

data class PersonReplicationInfo(
    val personId: UUID,
    val firstName: String,
    val lastname: String,
    val joinedAt: LocalDateTime,
    val status: PersonStatus
)

enum class PersonStatus {
    ACTIVE, DELETED
}
