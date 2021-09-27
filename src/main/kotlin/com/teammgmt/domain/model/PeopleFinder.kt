package com.teammgmt.domain.model

import arrow.core.Either
import java.util.UUID

interface PeopleFinder {
    fun find(personId: PersonId): Either<PersonNotFound, Person>
}

data class PersonId(val value: UUID)

data class Person(val personId: PersonId, val fullName: FullName)
