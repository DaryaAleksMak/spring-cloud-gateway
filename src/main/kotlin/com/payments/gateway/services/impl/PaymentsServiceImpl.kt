package com.payments.gateway.services.impl

import com.payments.gateway.config.PaymentsServiceIntegrationConfig.Companion.PAYMENT_SERVICE_WEB_CLIENT
import com.payments.gateway.data.PaymentType
import com.payments.gateway.dto.InternalPaymentResponse
import com.payments.gateway.services.PaymentsService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class PaymentsServiceImpl(
    @Qualifier(PAYMENT_SERVICE_WEB_CLIENT)
    private val client: WebClient,
) : PaymentsService {
    private val log = KotlinLogging.logger { }

    override fun getPaymentTypeById(paymentId: String) =
        paymentInfo("payment_id" to paymentId)

    override fun getPaymentTypeByOperationId(operationId: String) =
        paymentInfo("operation_id" to operationId)

    private fun paymentInfo(searchCriteria: Pair<String, String>) =
        requestPaymentType(searchCriteria)

    private fun requestPaymentType(searchCriteria: Pair<String, String>) =
        client.get()
            .uri {
                it.path("/payments/routing_direction")
                    .queryParams(
                        LinkedMultiValueMap<String, String>().apply {
                            add(searchCriteria.first, searchCriteria.second)
                        },
                    )
                    .build()
            }
            .exchangeToMono {
                it.bodyToMono<InternalPaymentResponse>()
                    .doOnNext { log.info { "Get payment info. Search criteria $searchCriteria body: $it" } }
                    .mapNotNull<PaymentType> { body -> body.creditProvider?.toPaymentType() }
            }

    private fun String.toPaymentType() = when (uppercase()) {
        "UAPI" -> PaymentType.P2P
        "TRANZZO" -> PaymentType.P2P
        "TRANZZO_P2P" -> PaymentType.P2P
        "P2M_PROVIDER" -> PaymentType.P2M
        "MOBILE" -> PaymentType.P2M
        "IBAN" -> PaymentType.IBAN
        "TRANZZO_TOKENIZATION" -> PaymentType.TOKENIZATION
        else -> null
    }
}
