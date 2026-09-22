package com.gexetr.capability.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CapabilityReport(
    val id: CapabilityId,
    val availability: CapabilityAvailability,
    val detail: String?,
)

interface CapabilityDiagnostics {
    val reports: StateFlow<List<CapabilityReport>>
}

class MutableCapabilityDiagnostics : CapabilityDiagnostics {
    private val mutableReports = MutableStateFlow<List<CapabilityReport>>(emptyList())

    override val reports: StateFlow<List<CapabilityReport>> = mutableReports.asStateFlow()

    fun put(report: CapabilityReport) {
        mutableReports.update { current ->
            val existingIndex = current.indexOfFirst { it.id == report.id }
            if (existingIndex == -1) {
                current + report
            } else {
                current.toMutableList().apply {
                    this[existingIndex] = report
                }
            }
        }
    }
}
