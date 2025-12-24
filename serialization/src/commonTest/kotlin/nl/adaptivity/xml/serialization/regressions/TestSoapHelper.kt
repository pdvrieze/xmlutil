/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

@file:MustUseReturnValues

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer
import nl.adaptivity.xml.serialization.pedantic
import nl.adaptivity.xml.serialization.regressions.soap.Envelope
import nl.adaptivity.xml.serialization.regressions.soap.Fault
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.serialization.XML1_0
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.*


/**
 */
class TestSoapHelper {

    @Test
    fun testUnmarshalSoapResponse() {
        val env = Envelope.deserialize(xmlStreaming.newReader(SOAP_RESPONSE1))
        val bodyContent = env.body.child
        assertXmlEquals(SOAP_RESPONSE1_BODY, bodyContent.contentString)
    }

    @Test
    fun testRoundtripSoapResponse() {
        val xml = XML1_0.pedantic { setIndent(2) }
        val serializer = serializer<Envelope<CompactFragment>>()
        val env: Envelope<CompactFragment> = xml.decodeFromString(serializer, SOAP_RESPONSE1)
        assertXmlEquals(SOAP_RESPONSE1_BODY, env.body.child.contentString.trim())

        val serialized = xml.encodeToString(serializer, env)
        assertXmlEquals(SOAP_RESPONSE1, serialized)
    }

    @Test
    fun testRoundtripSoapResponse2() {
        val xml = XML1_0.pedantic { setIndent(2) }
        val env: Envelope<CompactFragment> = Envelope.deserialize(xmlStreaming.newReader(SOAP_RESPONSE2))
        val sw = StringWriter()
        xmlStreaming.newWriter(sw).use { out ->
            xml.encodeToWriter(out, env, null)
        }
        sw.flush()
        assertXmlEquals(SOAP_RESPONSE2, sw.toString())
    }

    @Test
    fun testUnmarshalSoapResponse2() {
        if (testTarget == Target.Node) return

        val dbf = xmlStreaming.genericDomImplementation
        //dbf.setNamespaceAware = true

        val doc = dbf.createDocument().also { d ->
            val i =xmlStreaming.newReader(SOAP_RESPONSE1)
            val o = xmlStreaming.newWriter(d)
            while (i.hasNext()) {
                i.next().writeEvent(o, i)
            }
        }

        val env = Envelope.deserialize(xmlStreaming.newReader(doc))
        val bodyContent = env.body.child
        assertXmlEquals(SOAP_RESPONSE1_BODY, bodyContent.contentString)
    }

    @Test
    fun testXmlReaderFromDom() {
        if (testTarget == Target.Node) return

        val input =
            "<foo xmlns=\"urn:bar\"><rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result></foo>"

        val dbf = xmlStreaming.genericDomImplementation
        //dbf.setNamespaceAware = true

        val doc = dbf.createDocument().also { d ->
            val i =xmlStreaming.newReader(input)
            val o = xmlStreaming.newWriter(d)
            while (i.hasNext()) { i.next().writeEvent(o, i) }
        }

        val reader = xmlStreaming.newReader(doc)
        assertFalse(reader.isStarted)

        if (reader.next() == EventType.START_DOCUMENT) { val _ = reader.next() }

        reader.require(EventType.START_ELEMENT, "urn:bar", "foo")
        reader.requireNext(EventType.START_ELEMENT, "http://www.w3.org/2003/05/soap-rpc", "result")
        val parseResult = reader.siblingsToFragment()

        assertEquals("<rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result>",parseResult.contentString)
        assertFalse(parseResult.namespaces.iterator().hasNext(), "Unexpected namespaces: ${parseResult.namespaces.joinToString()} - '${parseResult.contentString}'")
    }

    @Test
    fun testResponse3_234() {
        if (testTarget == Target.Node) return

        val xml = XML1_0.pedantic()
        val soap = xml.decodeFromString<Envelope<Fault>>(SOAP_RESPONSE3)
        val fault = soap.body.child
        assertNull(fault.role)
        assertNull(fault.node)

        assertEquals("Client", fault.code.value.localPart)
        assertEquals(Envelope.NAMESPACE, fault.code.value.namespaceURI)

        val reasonText = assertIs<Fault.Text>(fault.reason.texts.singleOrNull())
        assertEquals("en", reasonText.lang)
        assertEquals("UPnPError", reasonText.value)

        val detail = assertIs<Element>(fault.detail?.content?.singleOrNull { (it !is Text) || !isXmlWhitespace(it.data) })
        assertEquals("UPnPError", detail.localName)
        assertEquals("urn:schemas-upnp-org:control-1-0", detail.namespaceURI)

        val errorChildren = detail.childNodes.filterNot { it is Text && isXmlWhitespace(it.data) }
        assertEquals(2, errorChildren.size)
    }

    companion object {

        private val SOAP_RESPONSE1_BODY = """|<getProcessNodeInstanceSoapResponse>
            |  <rpc:result xmlns:rpc="http://www.w3.org/2003/05/soap-rpc">result</rpc:result>
            |  <result>
            |    <pe:nodeInstance xmlns:pe="http://adaptivity.nl/ProcessEngine/" handle="18" nodeid="ac2" processinstance="5" state="Acknowledged">
            |      <pe:predecessor>16</pe:predecessor>
            |      <pe:body>
            |        <env:Envelope xmlns="http://www.w3.org/2003/05/soap-envelope" xmlns:env="http://www.w3.org/2003/05/soap-envelope" xmlns:umh="http://adaptivity.nl/userMessageHandler" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" encodingStyle="http://www.w3.org/2003/05/soap-encoding">
            |          <Body>
            |            <umh:postTask xmlns="http://adaptivity.nl/userMessageHandler">
            |              <repliesParam>
            |                <jbi:endpointDescriptor xmlns:jbi="http://adaptivity.nl/jbi" endpointLocation="http://localhost:8080/ProcessEngine" endpointName="soap" serviceLocalName="ProcessEngine" serviceNS="http://adaptivity.nl/ProcessEngine/"/>
            |              </repliesParam>
            |              <taskParam>
            |                <task instancehandle="5" owner="pdvrieze" remotehandle="18" summary="Task Bar">
            |                  <item type="label" value="Hi . Welcome!"/>
            |                </task>
            |              </taskParam>
            |            </umh:postTask>
            |          </Body>
            |        </env:Envelope>
            |      </pe:body>
            |    </pe:nodeInstance>
            |  </result>
            |</getProcessNodeInstanceSoapResponse>""".trimMargin()

        private val SOAP_RESPONSE1 =
            """|<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
            |  <soap:Body soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding">$SOAP_RESPONSE1_BODY  </soap:Body>
            |</soap:Envelope>""".trimMargin()

        private val SOAP_RESPONSE2 =
            """|<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
                |  <soap:Body soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding"><getProcessNodeInstanceSoapResponse>
                |    </getProcessNodeInstanceSoapResponse>
                |    </soap:Body>
                |</soap:Envelope>
                """.trimMargin()

        private val SOAP_RESPONSE3 =
            """|<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" s:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
                |    <s:Body>
                |        <s:Fault>
                |            <s:Detail>
                |                <UPnPError xmlns="urn:schemas-upnp-org:control-1-0">
                |                    <errorCode>401</errorCode>
                |                    <errorDescription>Invalid Action</errorDescription>
                |                </UPnPError>
                |            </s:Detail>
                |            <s:Code><s:Value>s:Client</s:Value></s:Code>
                |            <s:Reason>
                |                <s:Text xml:lang="en">UPnPError</s:Text>
                |            </s:Reason>
                |        </s:Fault>
                |    </s:Body>
                |</s:Envelope>""".trimMargin()
    }

}
