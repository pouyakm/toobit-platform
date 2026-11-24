package com.toobit.platform.domain

import com.toobit.platform.data.AppJson
import kotlinx.serialization.json.*

/** Mirrors backend OrderState enum order (Received=0 ... Unknown=9). */
object OrderStates {
    val names = listOf("Received", "Validated", "RiskChecked", "Submitted", "Acked", "PartiallyFilled", "Filled", "Canceled", "Rejected", "Unknown")
    fun name(i: Int) = names.getOrElse(i) { "?" }
    fun isTerminal(i: Int) = i in 6..8          // Filled, Canceled, Rejected. Unknown (9) is NOT terminal.
    fun isUnknown(i: Int) = i == 9
}

object KillSwitchPhrase {
    /** Same typed confirmation as the web dashboard. */
    fun required(turnOn: Boolean) = if (turnOn) "HALT" else "RESUME"
    fun matches(turnOn: Boolean, typed: String) = typed.trim() == required(turnOn)
}

object Explain {
    /** Top-n (name, value) pairs by absolute value from a defensive parse of the server's explain/features JSON. */
    fun topFeatures(json: String?, n: Int = 5): List<Pair<String, Double>> {
        if (json.isNullOrBlank()) return emptyList()
        val root = try { AppJson.parseToJsonElement(json) } catch (e: Exception) { return emptyList() }
        val pairs = mutableListOf<Pair<String, Double>>()
        fun collect(el: JsonElement) {
            when (el) {
                is JsonObject -> el.forEach { (k, v) ->
                    val d = (v as? JsonPrimitive)?.doubleOrNull
                    if (d != null) pairs += k to d else if (v is JsonObject || v is JsonArray) collect(v)
                }
                is JsonArray -> el.forEach { item ->
                    val o = item as? JsonObject ?: return@forEach
                    val name = (o["name"] ?: o["feature"])?.jsonPrimitive?.contentOrNull
                    val v = (o["value"] ?: o["contribution"] ?: o["importance"])?.jsonPrimitive?.doubleOrNull
                    if (name != null && v != null) pairs += name to v
                }
                else -> {}
            }
        }
        collect(root)
        return pairs.sortedByDescending { kotlin.math.abs(it.second) }.take(n)
    }
}
