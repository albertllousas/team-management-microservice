package com.teammgmt.infrastructure.adapters.outbound.client

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.teammgmt.domain.model.FullName
import com.teammgmt.domain.model.Person
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.fixtures.stubHttpEnpointForFindPersonNonSucceeded
import com.teammgmt.fixtures.stubHttpEnpointForFindPersonNotFound
import com.teammgmt.fixtures.stubHttpEnpointForFindPersonSucceeded
import com.teammgmt.infrastructure.configuration.InfrastructureConfiguration
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

@Tag("integration")
class PeopleServiceHttpClientShould {

    private val peopleService = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }

    private val api = InfrastructureConfiguration().let {
        it.peopleServiceApi(
            defaultObjectMapper = it.defaultObjectMapper(),
            circuitBreaker = CircuitBreaker.ofDefaults(""),
            meterRegistry = SimpleMeterRegistry(),
            url = peopleService.baseUrl(),
            connectTimeout = 2000,
            readTimeout = 2000
        )
    }

    private val client = PeopleServiceHttpClient(api)

    @Test
    fun `find an person`() {
        val personId = UUID.randomUUID()
        peopleService.stubHttpEnpointForFindPersonSucceeded(personId = personId)

        val result = client.find(PersonId(personId))

        assertThat(result).isEqualTo(
            Person(personId = PersonId(personId), fullName = FullName.reconstitute("Jane Doe")).right()
        )
    }

    @Test
    fun `fail when person does not exists`() {
        val personId = UUID.randomUUID()
        peopleService.stubHttpEnpointForFindPersonNotFound(personId)

        val result = client.find(PersonId(personId))

        assertThat(result).isEqualTo(PersonNotFound.left())
    }

    @Test
    fun `crash when there is a non successful http response`() {
        val personId = UUID.randomUUID()
        peopleService.stubHttpEnpointForFindPersonNonSucceeded(personId)

        assertThatThrownBy { client.find(PersonId(personId)) }
            .isExactlyInstanceOf(HttpCallNonSucceededException::class.java)
            .hasMessage(
                """Http call with 'PeopleServiceHttpClient' failed with status '400' and body '{"status":400,"detail":"Some problem"}' """
            )
    }
}
