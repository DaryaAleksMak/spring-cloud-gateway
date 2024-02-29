package com.payments.gateway.services

import com.payments.gateway.data.PaymentType
import reactor.core.publisher.Mono

interface PaymentsService {
    fun getPaymentTypeById(paymentId: String): Mono<PaymentType>

    fun getPaymentTypeByOperationId(operationId: String): Mono<PaymentType>
}
