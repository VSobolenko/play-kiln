package com.gexetr.plinthproductphotos.domain.provenance

import com.gexetr.plinthproductphotos.domain.export.XmpWriter
import com.gexetr.plinthproductphotos.domain.model.Provenance

enum class ProvenanceOperation {
    LOCAL_ORIGINAL,
    MANUAL_ADJUSTMENT,
    GENERATED_SCENE,
    EXPAND_CANVAS,
    RELIGHT,
    RECOLOUR,
    UPSCALE,
    ERASE_REGION,
    GHOST_MANNEQUIN,
    VIRTUAL_MODEL,
}

data class ProvenanceDeclaration internal constructor(
    val provenance: Provenance,
    val digitalSourceType: String?,
) {
    init {
        when (provenance) {
            Provenance.ORIGINAL_PIXELS -> require(digitalSourceType == null)
            Provenance.MANUALLY_ADJUSTED -> require(digitalSourceType == IPTC_ALGORITHMICALLY_ENHANCED)
            Provenance.COMPOSITE_GENERATED_BACKGROUND ->
                require(digitalSourceType == IPTC_COMPOSITE_TRAINED_ALGORITHMIC_MEDIA)
            Provenance.ENHANCED -> require(digitalSourceType == IPTC_TRAINED_ALGORITHMIC_MEDIA)
        }
    }

    fun xmpPacket(appName: String, appVersion: String): String {
        require(appName.isNotBlank())
        require(appVersion.isNotBlank())
        return XmpWriter.packet("$appName $appVersion", digitalSourceType)
    }

    companion object {
        const val IPTC_TRAINED_ALGORITHMIC_MEDIA =
            "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"
        const val IPTC_COMPOSITE_TRAINED_ALGORITHMIC_MEDIA =
            "http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia"
        const val IPTC_ALGORITHMICALLY_ENHANCED =
            "http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced"
    }
}

fun declarationFor(operation: ProvenanceOperation): ProvenanceDeclaration = when (operation) {
    ProvenanceOperation.LOCAL_ORIGINAL -> ProvenanceDeclaration(Provenance.ORIGINAL_PIXELS, null)
    ProvenanceOperation.MANUAL_ADJUSTMENT -> ProvenanceDeclaration(
        Provenance.MANUALLY_ADJUSTED,
        ProvenanceDeclaration.IPTC_ALGORITHMICALLY_ENHANCED,
    )
    ProvenanceOperation.GENERATED_SCENE,
    ProvenanceOperation.EXPAND_CANVAS -> ProvenanceDeclaration(
        Provenance.COMPOSITE_GENERATED_BACKGROUND,
        ProvenanceDeclaration.IPTC_COMPOSITE_TRAINED_ALGORITHMIC_MEDIA,
    )
    ProvenanceOperation.RELIGHT,
    ProvenanceOperation.RECOLOUR,
    ProvenanceOperation.UPSCALE,
    ProvenanceOperation.ERASE_REGION,
    ProvenanceOperation.GHOST_MANNEQUIN,
    ProvenanceOperation.VIRTUAL_MODEL -> ProvenanceDeclaration(
        Provenance.ENHANCED,
        ProvenanceDeclaration.IPTC_TRAINED_ALGORITHMIC_MEDIA,
    )
}
