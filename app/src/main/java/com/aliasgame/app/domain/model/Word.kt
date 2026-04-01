package com.aliasgame.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val text: String,
    val packId: String
)
