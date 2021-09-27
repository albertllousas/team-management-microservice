package com.teammgmt.domain.model

sealed class DomainError
sealed class TeamValidationError : DomainError() {
    object TooLongName : TeamValidationError()
}
object TeamNameAlreadyTaken : DomainError()
object TeamNotFound : DomainError()
object TeamMemberNotFound : DomainError()
object AlreadyPartOfTheTeam : DomainError()
object PersonNotFound : DomainError()
