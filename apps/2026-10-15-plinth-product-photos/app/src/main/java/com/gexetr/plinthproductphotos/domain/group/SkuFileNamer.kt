package com.gexetr.plinthproductphotos.domain.group

import com.gexetr.plinthproductphotos.domain.model.SlotRole

data class GroupOutputName(
    val id: String,
    val role: SlotRole,
    val sequence: Int,
    val extension: String,
)

object SkuFileNamer {
    fun assign(groupSku: String, outputs: List<GroupOutputName>): Map<String, String> {
        require(groupSku.isNotBlank())
        require(outputs.map(GroupOutputName::id).distinct().size == outputs.size) { "Output ids must be unique" }
        val safeSku = groupSku
            .replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('.', '-', '_')
        require(safeSku.isNotEmpty()) { "SKU must contain a filename-safe character" }
        val used = mutableSetOf<String>()
        return outputs.asSequence()
            .filter { it.role != SlotRole.REFERENCE }
            .associate { output ->
                require(output.sequence > 0)
                val extension = output.extension.removePrefix(".").lowercase()
                require(extension.isNotBlank() && extension.all { it.isLetterOrDigit() }) {
                    "Output extension must contain only letters and numbers"
                }
                val base = "${safeSku}_${output.role.name.lowercase()}_${output.sequence}"
                var filename = "$base.$extension"
                var suffix = 2
                while (!used.add(filename.lowercase())) {
                    filename = "$base-$suffix.$extension"
                    suffix++
                }
                output.id to filename
            }
    }
}
