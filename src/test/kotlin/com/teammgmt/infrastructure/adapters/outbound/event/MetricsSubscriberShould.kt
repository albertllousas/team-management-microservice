package com.teammgmt.infrastructure.adapters.outbound.event

import com.teammgmt.domain.model.TeamCreated
import com.teammgmt.domain.model.TeamMember
import com.teammgmt.domain.model.TeamMemberJoined
import com.teammgmt.domain.model.TeamMemberLeft
import com.teammgmt.fixtures.buildPerson
import com.teammgmt.fixtures.buildTeam
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetricsSubscriberShould {

    val metrics = SimpleMeterRegistry()

    val metricsSubscriber = MetricsSubscriber(metrics)

    @Test
    fun `publish a metric when a team is created`() {
        val teamCreated = TeamCreated(buildTeam())

        metricsSubscriber.handle(teamCreated)

        assertThat(metrics.counter("domain.event", listOf(Tag.of("type", "TeamCreated"))).count())
            .isEqualTo(1.0)
    }

    @Test
    fun `publish a metric when a person joins a team`() {
        val person = buildPerson()
        val team = buildTeam(members = setOf(TeamMember(person.personId)))
        val teamMemberJoined = TeamMemberJoined(team = team, personId = person.personId)

        metricsSubscriber.handle(teamMemberJoined)

        assertThat(metrics.counter("domain.event", listOf(Tag.of("type", "TeamMemberJoined"))).count())
            .isEqualTo(1.0)
    }

    @Test
    fun `publish a metric when a person leave a team`() {
        val person = buildPerson()
        val team = buildTeam(members = setOf(TeamMember(person.personId)))
        val teamMemberLeft = TeamMemberLeft(team = team, personId = person.personId)

        metricsSubscriber.handle(teamMemberLeft)

        assertThat(metrics.counter("domain.event", listOf(Tag.of("type", "TeamMemberLeft"))).count())
            .isEqualTo(1.0)
    }
}
