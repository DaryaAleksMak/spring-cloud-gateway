package com.payments.gateway.properties

data class Route(
    val url: String,
    val check: String?,
    val payment: String?,
    val status: String?,
    val cancel: String?,
    val repeat: String?,
)
