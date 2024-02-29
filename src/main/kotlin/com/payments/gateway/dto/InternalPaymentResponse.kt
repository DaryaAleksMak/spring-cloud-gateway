package com.payments.gateway.dto

data class InternalPaymentResponse(
    val creditProvider: String?,
) : BaseDto()
