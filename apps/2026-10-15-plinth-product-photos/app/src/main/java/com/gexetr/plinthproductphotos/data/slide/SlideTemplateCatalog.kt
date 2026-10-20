package com.gexetr.plinthproductphotos.data.slide

import android.content.res.AssetManager
import com.gexetr.plinthproductphotos.domain.slide.SlideTemplate
import com.gexetr.plinthproductphotos.domain.slide.SlideTextSlot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SlideTemplateDefinition(
    val template: SlideTemplate,
    val layout: String,
)

object SlideTemplateCatalog {
    fun load(assets: AssetManager): List<SlideTemplateDefinition> {
        val source = assets.open("slide/slide-templates.json").bufferedReader().use { it.readText() }
        val root = Json.parseToJsonElement(source).jsonObject
        require(root.getValue("schemaVersion").jsonPrimitive.content.toInt() == 1) {
            "Unsupported slide-template asset schema"
        }
        val templates = root.getValue("templates").jsonArray.map { element ->
            val value = element.jsonObject
            val id = value.getValue("id").jsonPrimitive.content
            val template = SlideTemplate.entries.firstOrNull { it.wireValue == id }
                ?: error("Unknown slide template '$id'")
            val slots = value.getValue("slots").jsonArray.map { slot ->
                SlideTextSlot.entries.firstOrNull { it.wireValue == slot.jsonPrimitive.content }
                    ?: error("Unknown text slot '${slot.jsonPrimitive.content}' in template '$id'")
            }.toSet()
            require(slots == template.slots) { "Slide template '$id' has slots that disagree with the layout engine" }
            val layout = value.getValue("layout").jsonPrimitive.content
            require(layout.isNotBlank()) { "Slide template '$id' needs a layout id" }
            SlideTemplateDefinition(
                template = template,
                layout = layout,
            )
        }
        require(
            templates.size == SlideTemplate.entries.size &&
                templates.map { it.template }.toSet() == SlideTemplate.entries.toSet(),
        ) {
            "Slide-template asset must declare every supported template exactly once"
        }
        return templates
    }
}
