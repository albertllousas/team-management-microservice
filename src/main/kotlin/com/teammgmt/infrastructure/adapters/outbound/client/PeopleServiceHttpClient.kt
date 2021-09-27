package com.teammgmt.infrastructure.adapters.outbound.client

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.teammgmt.domain.model.FullName
import com.teammgmt.domain.model.PeopleFinder
import com.teammgmt.domain.model.Person
import com.teammgmt.domain.model.PersonId
import com.teammgmt.domain.model.PersonNotFound
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.UUID

@Deprecated(message = "Now using replication by events instead")
class PeopleServiceHttpClient(private val peopleServiceApi: PeopleServiceApi) : PeopleFinder {

    override fun find(personId: PersonId): Either<PersonNotFound, Person> =
        peopleServiceApi.find(personId.value)
            .execute()
            .let(::extractBody)
            .map(::mapToDomain)

    private fun extractBody(response: Response<PersonApiResponse>): Either<PersonNotFound, PersonApiResponse> =
        when {
            response.isSuccessful -> response.body()!!.right()
            response.code() == 404 -> PersonNotFound.left()
            else -> throw HttpCallNonSucceededException(
                httpClient = this@PeopleServiceHttpClient::class.simpleName!!,
                errorBody = response.errorBody()?.charStream()?.readText()?.trimIndent(),
                httpStatus = response.code()
            )
        }

    private fun mapToDomain(response: PersonApiResponse) = Person(
        personId = PersonId(response.id),
        fullName = FullName.create(response.firstName, response.lastName)
    )
}

interface PeopleServiceApi {

    @GET("/people/{id}")
    fun find(@Path("id") accountId: UUID): Call<PersonApiResponse>
}

data class PersonApiResponse(val id: UUID, val firstName: String, val lastName: String)
