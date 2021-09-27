package com.teammgmt.infrastructure.adapters.inbound.http

import com.teammgmt.domain.model.AlreadyPartOfTheTeam
import com.teammgmt.domain.model.DomainError
import com.teammgmt.domain.model.PersonNotFound
import com.teammgmt.domain.model.TeamMemberNotFound
import com.teammgmt.domain.model.TeamNameAlreadyTaken
import com.teammgmt.domain.model.TeamNotFound
import com.teammgmt.domain.model.TeamValidationError.TooLongName
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity

val conflict = ResponseEntity<Unit>(CONFLICT)
val notFound = ResponseEntity<Unit>(NOT_FOUND)
val unprocessableEntity = ResponseEntity<Unit>(UNPROCESSABLE_ENTITY)

// TODO: Improve, map errors better, add meaningful error payloads
fun DomainError.asHttpResponse(): ResponseEntity<Unit> = when (this) {
    AlreadyPartOfTheTeam -> conflict
    TeamNameAlreadyTaken, TooLongName -> unprocessableEntity
    PersonNotFound, TeamMemberNotFound, TeamNotFound -> notFound
}
