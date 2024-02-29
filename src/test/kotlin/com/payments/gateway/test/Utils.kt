package com.payments.gateway.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jeasy.random.EasyRandom

private val mapper = jacksonObjectMapper()

fun Any.toJson(): String = mapper.writeValueAsString(this)
