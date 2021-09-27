package com.teammgmt.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.springframework.http.HttpStatus
import java.util.UUID

fun WireMockServer.stubHttpEnpointForFindPersonNonSucceeded(
    personId: UUID = DEFAULT_PERSON_ID,
    responseCode: Int = 400,
    responseErrorBody: String = """{"status":400,"detail":"Some problem"}"""
) =
    this.stubFor(
        get(urlEqualTo("/people/$personId"))
            .willReturn(status(responseCode).withBody(responseErrorBody))
    )

fun WireMockServer.stubHttpEnpointForFindPersonNotFound(personId: UUID = DEFAULT_PERSON_ID) =
    this.stubHttpEnpointForFindPersonNonSucceeded(
        personId, 404, """ {"status":404,"detail":"Account not found: $personId"} """
    )

fun WireMockServer.stubHttpEnpointForFindPersonSucceeded(personId: UUID = DEFAULT_PERSON_ID) =
    this.stubFor(
        get(urlEqualTo("/people/$personId"))
            .willReturn(
                status(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                              "id": "$personId",
                              "firstName": "Jane",
                              "lastName": "Doe"
                            }
                        """
                    )
            )
    )
