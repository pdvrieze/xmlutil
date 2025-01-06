package net.devrieze.serialization.examples.dynamictagnames

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicContainerSerializerTest {

    val xml = XML { indent = 2 }

    @Test
    fun testEncode() {
        val actual = xml.encodeToString(ktData)

        assertXmlEquals(XML_DATA, actual)
    }

    @Test
    fun testDecode() {
        val decoded = xml.decodeFromString<Container>(XML_DATA)
        assertEquals(ktData, decoded)
    }

    companion object {
        const val XML_DATA =
            "<Container><Test_123 attr=\"42\"><data>someData</data></Test_123><Test_456 attr=\"71\"><data>moreData</data></Test_456></Container>"

        val ktData = Container(
            listOf(
                TestElement(123, 42, "someData"),
                TestElement(456, 71, "moreData")
            )
        )

    }
}
