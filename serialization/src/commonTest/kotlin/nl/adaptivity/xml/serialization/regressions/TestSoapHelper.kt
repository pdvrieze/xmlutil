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

package nl.adaptivity.xml.serialization.regressions

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import nl.adaptivity.xml.serialization.regressions.soap.Envelope
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.CompactFragmentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse


/**
 * Created by pdvrieze on 03/12/15.
 */
class TestSoapHelper {

    @Test
    fun testUnmarshalSoapResponse() {
        val env = Envelope.deserialize(xmlStreaming.newReader(SOAP_RESPONSE1))
        val bodyContent = env.body.child as CompactFragment
        assertXmlEquals(SOAP_RESPONSE1_BODY, bodyContent.contentString)
    }

    @Test
    fun testRoundtripSoapResponse() {
        val xml = XML { indent = 2; autoPolymorphic = true }
        val serializer = Envelope.Serializer(CompactFragmentSerializer)
        val env: Envelope<CompactFragment> = xml.decodeFromString(serializer, SOAP_RESPONSE1)
        assertXmlEquals(SOAP_RESPONSE1_BODY, env.body.child.contentString.trim())

        val serialized = XML.encodeToString(serializer, env)
        assertXmlEquals(SOAP_RESPONSE1, serialized)
    }

    @Test
    fun testRoundtripSoapResponse2() {
        val xml: XML = XML { indent = 2; autoPolymorphic = true }
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
        val dbf = xmlStreaming.genericDomImplementation
        //dbf.setNamespaceAware = true

        val doc = dbf.createDocument().also { d ->
            val i =xmlStreaming.newReader(SOAP_RESPONSE1)
            val o = xmlStreaming.newWriter(d)
            while (i.hasNext()) { i.next(); i.writeCurrent(o) }
        }

        val env = Envelope.Companion.deserialize(xmlStreaming.newReader(doc))
        val bodyContent = env.body.child as CompactFragment
        assertXmlEquals(SOAP_RESPONSE1_BODY, bodyContent.contentString)
    }

    @Test
    fun testXmlReaderFromDom() {
        val input =
            "<foo xmlns=\"urn:bar\"><rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result></foo>"

        val dbf = xmlStreaming.genericDomImplementation
        //dbf.setNamespaceAware = true

        val doc = dbf.createDocument().also { d ->
            val i =xmlStreaming.newReader(input)
            val o = xmlStreaming.newWriter(d)
            while (i.hasNext()) { i.next(); i.writeCurrent(o) }
        }

        val reader = xmlStreaming.newReader(doc)
        assertFalse(reader.isStarted)

        if (reader.next() == EventType.START_DOCUMENT) reader.next()

        reader.require(EventType.START_ELEMENT, "urn:bar", "foo")
        reader.next()
        reader.require(EventType.START_ELEMENT, "http://www.w3.org/2003/05/soap-rpc", "result")
        val parseResult = reader.siblingsToFragment()
        assertEquals("<rpc:result xmlns:rpc=\"http://www.w3.org/2003/05/soap-rpc\">result</rpc:result>", parseResult.contentString)
        assertFalse(parseResult.namespaces.iterator().hasNext(), "Unexpected namespaces: ${parseResult.namespaces.joinToString()} - '${parseResult.contentString}'")
    }

    companion object {

        private val SOAP_RESPONSE1_BODY = """<getProcessNodeInstanceSoapResponse>
      <rpc:result xmlns:rpc="http://www.w3.org/2003/05/soap-rpc">result</rpc:result>
      <result>
        <pe:nodeInstance xmlns:pe="http://adaptivity.nl/ProcessEngine/" handle="18" nodeid="ac2" processinstance="5" state="Acknowledged">
          <pe:predecessor>16</pe:predecessor>
          <pe:body>
            <env:Envelope xmlns="http://www.w3.org/2003/05/soap-envelope" xmlns:env="http://www.w3.org/2003/05/soap-envelope" xmlns:umh="http://adaptivity.nl/userMessageHandler" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" encodingStyle="http://www.w3.org/2003/05/soap-encoding">
              <Body>
                <umh:postTask xmlns="http://adaptivity.nl/userMessageHandler">
                  <repliesParam>
                    <jbi:endpointDescriptor xmlns:jbi="http://adaptivity.nl/jbi" endpointLocation="http://localhost:8080/ProcessEngine" endpointName="soap" serviceLocalName="ProcessEngine" serviceNS="http://adaptivity.nl/ProcessEngine/"/>
                  </repliesParam>
                  <taskParam>
                    <task instancehandle="5" owner="pdvrieze" remotehandle="18" summary="Task Bar">
                      <item type="label" value="Hi . Welcome!"/>
                    </task>
                  </taskParam>
                </umh:postTask>
              </Body>
            </env:Envelope>
          </pe:body>
        </pe:nodeInstance>
      </result>
    </getProcessNodeInstanceSoapResponse>
  """
        private val SOAP_RESPONSE1 =
            """<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding">$SOAP_RESPONSE1_BODY  </soap:Body>
</soap:Envelope>
"""

        private val SOAP_RESPONSE2 =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                    "  <soap:Body soap:encodingStyle=\"http://www.w3.org/2003/05/soap-encoding\">" + ("<getProcessNodeInstanceSoapResponse>\n" +
                    "    </getProcessNodeInstanceSoapResponse>\n" +
                    "  ") +
                    "  </soap:Body>\n" +
                    "</soap:Envelope>\n"
    }

}
