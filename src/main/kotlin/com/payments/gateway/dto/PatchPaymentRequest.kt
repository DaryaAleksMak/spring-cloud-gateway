package com.payments.gateway.dto

data class PatchPaymentRequest(
    val paymentId: String,
) : BaseDto()
