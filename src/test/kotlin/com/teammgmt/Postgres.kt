package com.teammgmt

import com.teammgmt.domain.model.Team
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

class Postgres {

    val container: KtPostgreSQLContainer = KtPostgreSQLContainer()
        .withNetwork(Network.newNetwork())
        .withNetworkAliases("localhost")
        .withUsername("teammgmt")
        .withPassword("teammgmt")
        .withDatabaseName("teammgmt")
        .also {
            it.start()
        }

    val datasource: DataSource = HikariDataSource().apply {
        driverClassName = org.postgresql.Driver::class.qualifiedName
        jdbcUrl = container.jdbcUrl
        username = container.username
        password = container.password
    }.also { Flyway(FluentConfiguration().dataSource(it)).migrate() }

    private val jdbcTemplate = JdbcTemplate(datasource)

    fun addTeam(team: Team) =
        jdbcTemplate.update(
            """ INSERT INTO team (id, name, members) VALUES (?,?,?) """,
            team.teamId.value,
            team.teamName.value,
            team.members.map { it.personId.value.toString() }.toTypedArray()
        )
}

class KtPostgreSQLContainer : PostgreSQLContainer<KtPostgreSQLContainer>("postgres:13.4")
