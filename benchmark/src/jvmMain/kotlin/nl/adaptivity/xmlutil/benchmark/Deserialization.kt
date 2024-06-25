/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.benchmark

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import kotlinx.benchmark.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.benchmark.util.testXmlSchemaUrls
import nl.adaptivity.xmlutil.jdk.StAXStreamingFactory
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.structure.*
import java.net.URL
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Measurement(iterations = 10)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class Deserialization {
    lateinit var retainedXml: XML

    val suites: List<Pair<URL, URL>> = testXmlSchemaUrls(XML { recommended_0_87_0() })

    lateinit var readers: Lazy<List<XmlBufferReader>>

    @Param("true")
    var unchecked: Boolean = false

    @Setup
    fun setup() {
        println("Setup called (unchecked: $unchecked)")
        retainedXml = XML {
            isUnchecked = unchecked
            defaultPolicy {
                autoPolymorphic = true
                throwOnRepeatedElement = true
                verifyElementOrder = true
                isStrictAttributeNames = true
            }
        }
        readers = lazy {
            suites.map { (_, u) ->
                u.openStream().use { input ->
                    xmlStreaming.newReader(input).use { reader ->
                        val events = buildList<XmlEvent> {
                            while (reader.hasNext()) add(reader.run { next(); toEvent() })
                        }
                        XmlBufferReader(events)
                    }
                }
            }
        }
    }

    @Benchmark
    fun testDeserializeGenericSpeed(bh : Blackhole) {
        val xml = XML {
            isUnchecked = unchecked
            defaultPolicy {
                autoPolymorphic = true
                throwOnRepeatedElement = true
            }
        }
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testDeserializeAndParseSpeed(bh, xml)
        xmlStreaming.setFactory(null)
    }

    @Benchmark
    fun testDeserializeGenericSpeedRetainedXml(bh : Blackhole) {
        check(retainedXml.config.isUnchecked == unchecked)
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testDeserializeAndParseSpeed(bh, retainedXml)
        xmlStreaming.setFactory(null)
    }

    @Benchmark
    fun testDeserializeNoparseRetained(bh : Blackhole) {
        check(retainedXml.config.isUnchecked == unchecked)
        testDeserializeOnlySpeed(bh, retainedXml)
    }

    @Benchmark
    fun testDeserializeStaxSpeed(bh : Blackhole) {
        val xml = XML {
            defaultPolicy {
                autoPolymorphic = true
                throwOnRepeatedElement = true
            }
        }
        xmlStreaming.setFactory(StAXStreamingFactory())
        testDeserializeAndParseSpeed(bh, xml)
        xmlStreaming.setFactory(null)
    }

    private fun testDeserializeAndParseSpeed(bh : Blackhole, xml: XML) {
        for ((_, uri) in suites) {
            try {
                uri.openStream().use { inStream ->
                    xmlStreaming.newReader(inStream).use { reader ->
                        val schema = xml.decodeFromReader<XSSchema>(reader)
                        bh.consume(schema)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failure to read schema: $uri \n${e.message?.prependIndent("        ")}")
            }
        }
    }

    private fun testDeserializeOnlySpeed(bh : Blackhole, xml: XML) {
        for (reader in readers.value) {
            try {
                val schema = xml.decodeFromReader<XSSchema>(reader)
                bh.consume(schema)
            } catch (e: Exception) {
                System.err.println("Failure to read schema:\n${e.message?.prependIndent("        ")}")
            }
        }
    }


}
