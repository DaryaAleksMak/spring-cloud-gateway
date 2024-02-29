package com.payments.gateway.data

enum class PaymentType(
    val creditDestinationTypes: List<String?>,
) {
    P2P(listOf("pan", "panId", "external_card_id", "card_token")),
    P2M(listOf("mobile")),
    IBAN(listOf("iban")),
    TOKENIZATION(listOf(null)),
}
