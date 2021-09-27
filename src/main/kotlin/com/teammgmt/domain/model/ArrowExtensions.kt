package com.teammgmt.domain.model

import arrow.core.Either

fun <A, B> Either<A, B>.peek(consume: (B) -> Unit) = when (this) {
    is Either.Left -> this
    is Either.Right -> consume(this.value).let { this }
}
