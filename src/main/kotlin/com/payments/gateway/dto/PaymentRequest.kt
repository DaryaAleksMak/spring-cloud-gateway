package com.payments.gateway.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okio.ByteString.Companion.decodeBase64

class PaymentRequest(
    val intent: String,
) : BaseDto() {

    class Intent(
        val creditDstType: String?,
    ) : BaseDto()

    fun retrieveCreditDstType(mapper: ObjectMapper) =
        mapper.readValue<Intent>(intent.decodeBase64()!!.utf8())
            .creditDstType
}
