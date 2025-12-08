/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.isXmlWhitespace
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlIgnoreWhitespace
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import kotlin.test.*

class ElementChildren237 {
    private val xml get() = XML { recommended_0_91_0 { pedantic = true } }

    @Test
    fun testDeserializedElement() {
        if (testTarget == Target.Node) return

        val extensions = xml.decodeFromString<Extensions>(XMLSTRING)

        val extension = assertNotNull(extensions.extension)

        assertEquals(1, extension.size)
        val extValue = extension.single()

        assertEquals("AdVerifications", extValue.type)

        val elems = extValue.value
        assertEquals(1, elems.size)

        val elem = elems.single()
        val adVerChildren = elem.childNodes
        assertEquals(3, adVerChildren.getLength())

        adVerChildren[0].assertWS()
        adVerChildren[2].assertWS()
        val verification = assertIs<Element>(adVerChildren[1])

        assertEquals("Verification", verification.localName)
        assertEquals(1, verification.attributes.size)
        assertEquals("Something", verification.getAttribute("vendor"))

        val jsResource = assertIs<Element>(
            assertIs<Text>(verification.firstChild).assertWS()
                .nextSibling
        )
        assertEquals("JavaScriptResource", jsResource.localName)
        assertEquals(2, jsResource.attributes.size)
        assertEquals("omid", jsResource.getAttribute("apiFramework"))
        assertEquals("true", jsResource.getAttribute("browserOptional"))

        val cdata = assertIs<Text>(
            jsResource.firstChild.assertWS().nextSibling
        )
        assertEquals("https://google.com/video.js", cdata.data)
        assertNull(cdata.nextSibling.assertWS().nextSibling)


        val verParams = assertIs<Element>(
            jsResource.nextSibling.assertWS().nextSibling
        )
        assertEquals("VerificationParameters", verParams.localName)
        assertEquals(0, verParams.attributes.size)

        val keys = assertIs<Text>(
            verParams.firstChild.assertWS().nextSibling
        )
        assertEquals("""{"key":"21649"}""", keys.data)
        assertNull(keys.nextSibling.assertWS().nextSibling)
    }

    @Test
    fun testDeserializedElementIgnoreWhitespace() {
        if (testTarget == Target.Node) return

        val extensions = xml.decodeFromString<ExtensionsIgnoreWS>(XMLSTRING)

        val extension = assertNotNull(extensions.extension)

        assertEquals(1, extension.size)
        val extValue = extension.single()

        assertEquals("AdVerifications", extValue.type)

        val elems = extValue.value
        assertEquals(1, elems.size)

        val elem = elems.single()
        val adVerChildren = elem.childNodes
        assertEquals(3, adVerChildren.getLength())

        adVerChildren[0].assertWS()
        adVerChildren[2].assertWS()
        val verification = assertIs<Element>(adVerChildren[1])

        assertEquals("Verification", verification.localName)
        assertEquals(1, verification.attributes.size)
        assertEquals("Something", verification.getAttribute("vendor"))

        val jsResource = assertIs<Element>(
            assertIs<Text>(verification.firstChild).assertWS()
                .nextSibling
        )
        assertEquals("JavaScriptResource", jsResource.localName)
        assertEquals(2, jsResource.attributes.size)
        assertEquals("omid", jsResource.getAttribute("apiFramework"))
        assertEquals("true", jsResource.getAttribute("browserOptional"))

        val cdata = assertIs<Text>(
            jsResource.firstChild.assertWS().nextSibling
        )
        assertEquals("https://google.com/video.js", cdata.data)
        assertNull(cdata.nextSibling.assertWS().nextSibling)


        val verParams = assertIs<Element>(
            jsResource.nextSibling.assertWS().nextSibling
        )
        assertEquals("VerificationParameters", verParams.localName)
        assertEquals(0, verParams.attributes.size)

        val keys = assertIs<Text>(
            verParams.firstChild.assertWS().nextSibling
        )
        assertEquals("""{"key":"21649"}""", keys.data)
        assertNull(keys.nextSibling.assertWS().nextSibling)
    }


    @Serializable
    class Extensions {
        var extension: List<Extension>? = null

        @Serializable
        class Extension {
            var type: String? = null

            @XmlValue
            val value: List<Element> = emptyList()
        }
    }

    @Serializable
    class ExtensionsIgnoreWS {
        var extension: List<Extension>? = null

        @Serializable
        @XmlIgnoreWhitespace
        class Extension {
            var type: String? = null

            @XmlValue
            val value: List<Element> = emptyList()
        }
    }


    companion object {

        @IgnorableReturnValue
        fun <T : Node> T?.assertWS(): T = also { assertIs<Text>(it).assertWS() }!!

        @IgnorableReturnValue
        fun <T : Text> T.assertWS(): T = also { assertTrue(isXmlWhitespace(it.data)) }

        internal val XMLSTRING = """|<Extensions>
            |    <Extension type="AdVerifications">
            |        <AdVerifications>
            |            <Verification vendor="Something">
            |                <JavaScriptResource apiFramework="omid" browserOptional="true">
            |                    <![CDATA[https://google.com/video.js]]>
            |                </JavaScriptResource>
            |                <VerificationParameters>
            |                    <![CDATA[{"key":"21649"}]]>
            |                </VerificationParameters>
            |            </Verification>
            |        </AdVerifications>
            |    </Extension>
            |</Extensions>""".trimMargin()
    }
}
