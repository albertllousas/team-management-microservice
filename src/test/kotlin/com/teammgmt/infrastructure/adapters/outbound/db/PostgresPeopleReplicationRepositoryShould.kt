package com.teammgmt.infrastructure.adapters.outbound.db

import arrow.core.left
import arrow.core.right
import com.teammgmt.Postgres
import com.teammgmt.domain.model.FullName
import com.teammgmt.domain.model.Person
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.fixtures.buildPersonReplicationInfo
import com.teammgmt.infrastructure.adapters.outbound.db.PersonStatus.DELETED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@Tag("integration")
class PostgresPeopleReplicationRepositoryShould {

    private val database = Postgres()

    private val jdbcTemplate = JdbcTemplate(database.datasource)

    private val peopleReplicationRepository = PostgresPeopleReplicationRepository(jdbcTemplate)

    @AfterEach
    fun `tear down`() {
        database.container.stop()
    }

    @Nested
    inner class Find {

        @Test
        fun `find a person`() {
            val info = buildPersonReplicationInfo().also(peopleReplicationRepository::save)

            val result = peopleReplicationRepository.find(PersonId(info.personId))

            assertThat(result).isEqualTo(
                Person(
                    personId = PersonId(info.personId),
                    fullName = FullName.reconstitute("${info.firstName} ${info.lastname}")
                ).right()
            )
        }

        @Test
        fun `not find a person when it does not exists`() {
            val result = peopleReplicationRepository.find(PersonId(UUID.randomUUID()))

            assertThat(result).isEqualTo(PersonNotFound.left())
        }

        @Test
        fun `not find a person when it has been deleted`() {
            val person = buildPersonReplicationInfo(status = DELETED).also(peopleReplicationRepository::save)

            val result = peopleReplicationRepository.find(PersonId(person.personId))

            assertThat(result).isEqualTo(PersonNotFound.left())
        }
    }

    @Nested
    inner class Save {

        @Test
        fun `save a person replication info`() {
            val person = buildPersonReplicationInfo()

            val result = peopleReplicationRepository.save(person)

            assertThat(result).isEqualTo(Unit)
            val storedPerson = jdbcTemplate.queryForMap(
                "SELECT * FROM people_replication WHERE id = '${person.personId}'"
            )
            assertThat(storedPerson).containsAllEntriesOf(
                mapOf(
                    "id" to person.personId,
                    "status" to person.status.name,
                    "first_name" to person.firstName,
                    "last_name" to person.lastname
                )
            )
        }

        @Test
        fun `update the person replication info when it already exists`() {
            val person = buildPersonReplicationInfo().also(peopleReplicationRepository::save)
            val personWithNewName = person.copy(firstName = "James")

            val result = peopleReplicationRepository.save(personWithNewName)

            assertThat(result).isEqualTo(Unit)
            val storedPerson = jdbcTemplate.queryForMap(
                "SELECT * FROM people_replication WHERE id = '${person.personId}'"
            )
            assertThat(storedPerson).containsAllEntriesOf(
                mapOf("first_name" to personWithNewName.firstName)
            )
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `delete a person`() {
            val person = buildPersonReplicationInfo().also(peopleReplicationRepository::save)

            peopleReplicationRepository.delete(person.personId)

            val storedPerson = jdbcTemplate.queryForMap(
                "SELECT * FROM people_replication WHERE id = '${person.personId}'"
            )
            assertThat(storedPerson).containsAllEntriesOf(mapOf("status" to DELETED.name))
        }
    }
}
