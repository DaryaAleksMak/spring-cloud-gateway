package com.payments.gateway.dto

data class CheckPaymentRequest(
    val creditDstType: String?,
) : BaseDto()
