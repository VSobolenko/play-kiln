package com.gexetr.capability.core

enum class CapabilityAvailability {
    UNINITIALIZED,
    UNSUPPORTED,
    NEEDS_CONSENT,
    READY,
    FAILED,
}

fun CapabilityAvailability.isUsable(): Boolean = this == CapabilityAvailability.READY

@JvmInline
value class CapabilityId(val value: String) {
    companion object {
        val ADS = CapabilityId("ads")
        val BILLING = CapabilityId("billing")
        val TELEMETRY = CapabilityId("telemetry")
    }
}
