package com.toobit.platform

import com.toobit.platform.data.UrlRules
import com.toobit.platform.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DomainTest {
    @Test fun orderStateNamesMatchBackendEnum() {
        assertEquals("Received", OrderStates.name(0)); assertEquals("Acked", OrderStates.name(4)); assertEquals("Unknown", OrderStates.name(9)); assertEquals("?", OrderStates.name(42))
    }
    @Test fun unknownIsNotTerminal() {
        assertFalse(OrderStates.isTerminal(9)); assertTrue(OrderStates.isUnknown(9))
        listOf(6, 7, 8).forEach { assertTrue(OrderStates.isTerminal(it)) }
        listOf(0, 1, 2, 3, 4, 5).forEach { assertFalse(OrderStates.isTerminal(it)) }
    }
    @Test fun killSwitchPhrase() {
        assertTrue(KillSwitchPhrase.matches(true, " HALT ")); assertFalse(KillSwitchPhrase.matches(true, "halt")); assertFalse(KillSwitchPhrase.matches(true, "RESUME"))
        assertTrue(KillSwitchPhrase.matches(false, "RESUME"))
    }
    @Test fun explainObjectSortedByMagnitude() {
        val r = Explain.topFeatures("""{"a":0.1,"b":-0.9,"c":0.5}""", 2)
        assertEquals(listOf("b", "c"), r.map { it.first })
    }
    @Test fun explainArrayAndNestedAndGarbage() {
        assertEquals("x", Explain.topFeatures("""[{"name":"x","value":2.0},{"feature":"y","contribution":1}]""").first().first)
        assertEquals("k", Explain.topFeatures("""{"top":{"k":3}}""").first().first)
        assertTrue(Explain.topFeatures("not json").isEmpty()); assertTrue(Explain.topFeatures(null).isEmpty())
    }
    @Test fun urlRules() {
        assertEquals("https://example.com/", UrlRules.normalize(" https://example.com ", false))
        assertNull(UrlRules.normalize("http://example.com", false))
        assertEquals("http://10.0.2.2:8080/", UrlRules.normalize("http://10.0.2.2:8080", true))
        assertNull(UrlRules.normalize("not a url", true))
        assertEquals("https://h/api/", UrlRules.normalize("https://h/api?x=1#f", false))
    }

    private fun strings(path: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent }
    }
    @Test fun enFaStringKeysAndPlaceholdersMatch() {
        val en = strings("src/main/res/values/strings.xml"); val fa = strings("src/main/res/values-fa/strings.xml")
        assertEquals(en.keys, fa.keys)
        val ph = Regex("%\\d\\$[sd]")
        en.forEach { (k, v) -> assertEquals(k, ph.findAll(v).map { it.value }.sorted().toList(), ph.findAll(fa.getValue(k)).map { it.value }.sorted().toList()) }
    }
}
