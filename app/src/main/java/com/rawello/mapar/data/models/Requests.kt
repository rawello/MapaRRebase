package com.rawello.mapar.data.models

data class LoginRequest(
    val username: String,
    val password: String
)

data class SignUpRequest(
    val username: String,
    val password: String
)

data class JwtAuthenticationResponse(
    val token: String
)