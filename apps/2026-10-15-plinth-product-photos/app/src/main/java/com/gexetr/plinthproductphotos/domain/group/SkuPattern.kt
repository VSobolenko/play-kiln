package com.gexetr.plinthproductphotos.domain.group

data class ParsedSku(val base: String, val number: Int)

class SkuPattern(val template: String) {
    private val matcher: Regex

    init {
        require(Regex("\\{base}").findAll(template).count() == 1) {
            "SKU pattern must contain {base} exactly once"
        }
        require(Regex("\\{n}").findAll(template).count() == 1) {
            "SKU pattern must contain {n} exactly once"
        }
        val pattern = StringBuilder("^")
        var previousEnd = 0
        Regex("\\{base}|\\{n}").findAll(template).forEach { match ->
            pattern.append(Regex.escape(template.substring(previousEnd, match.range.first)))
            pattern.append(if (match.value == "{base}") "(.+?)" else "(\\d+)")
            previousEnd = match.range.last + 1
        }
        pattern.append(Regex.escape(template.substring(previousEnd))).append('$')
        matcher = Regex(pattern.toString(), RegexOption.IGNORE_CASE)
    }

    fun format(base: String, number: Int): String {
        require(base.isNotBlank())
        require(number > 0)
        return template.replace("{base}", base).replace("{n}", number.toString())
    }

    fun parse(sku: String): ParsedSku? {
        val match = matcher.matchEntire(sku) ?: return null
        val placeholders = Regex("\\{base}|\\{n}").findAll(template).map { it.value }.toList()
        val captures = match.groupValues.drop(1)
        val base = captures[placeholders.indexOf("{base}")]
        val number = captures[placeholders.indexOf("{n}")].toIntOrNull() ?: return null
        return ParsedSku(base, number)
    }

    fun assignUnique(base: String, itemIds: List<String>, existingSkus: Set<String> = emptySet()): List<String> {
        require(itemIds.distinct().size == itemIds.size) { "Item ids must be unique" }
        val used = existingSkus.mapTo(mutableSetOf()) { it.lowercase() }
        return itemIds.mapIndexed { index, _ ->
            val candidate = format(base, index + 1)
            var unique = candidate
            var suffix = 2
            while (!used.add(unique.lowercase())) {
                unique = "$candidate-$suffix"
                suffix++
            }
            unique
        }
    }
}
