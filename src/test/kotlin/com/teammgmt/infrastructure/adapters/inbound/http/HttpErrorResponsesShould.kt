package com.teammgmt.infrastructure.adapters.inbound.http

import com.teammgmt.domain.model.AlreadyPartOfTheTeam
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.domain.model.TeamMemberNotFound
import com.teammgmt.domain.model.TeamNameAlreadyTaken
import com.teammgmt.domain.model.TeamNotFound
import com.teammgmt.domain.model.TeamValidationError.TooLongName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity

class HttpErrorResponsesShould {

    private val conflict = ResponseEntity<Unit>(CONFLICT)

    private val unprocessableEntity = ResponseEntity<Unit>(UNPROCESSABLE_ENTITY)

    private val notFound = ResponseEntity<Unit>(NOT_FOUND)

    @TestFactory
    fun `build http answers for domain errors`(): List<DynamicTest> =
        listOf(
            AlreadyPartOfTheTeam to conflict,
            TooLongName to unprocessableEntity,
            TeamNameAlreadyTaken to unprocessableEntity,
            TeamNotFound to notFound,
            TeamMemberNotFound to notFound,
            PersonNotFound to notFound
        ).map { (error, expectedHttpResponse) ->
            dynamicTest("$error is mapped to ${expectedHttpResponse.statusCode}") {
                assertThat(error.asHttpResponse()).isEqualTo(expectedHttpResponse)
            }
        }
}
