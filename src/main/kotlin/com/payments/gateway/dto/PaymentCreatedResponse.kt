package com.payments.gateway.dto

data class PaymentCreatedResponse(
    val id: String,
    val operationId: String,
) : BaseDto()
