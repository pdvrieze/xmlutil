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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.benchmark

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import kotlinx.benchmark.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.benchmark.util.BlackholeWrapper
import nl.adaptivity.xmlutil.benchmark.util.BlackholeWrapperImpl
import nl.adaptivity.xmlutil.benchmark.util.testXmlSchemaUrls
import nl.adaptivity.xmlutil.jdk.StAXStreamingFactory
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XML1_0
import java.net.URL
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Measurement(iterations = 10)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class Deserialization {
    lateinit var retainedXml: XML

    val suites: List<Pair<URL, URL>> = testXmlSchemaUrls(XML.recommended_1_0())

    val readers: List<Pair<URL, XmlBufferReader>> by lazy(LazyThreadSafetyMode.NONE) {
        suites
            .map { (_, u) ->
            u.openStream().use { input ->
                xmlStreaming.newReader(input).use { reader ->
                    val events = buildList<XmlEvent> {
                        while (reader.hasNext()) {
                            add(reader.run { next(); toEvent() })
                        }
                    }
                    u to XmlBufferReader(events)
                }
            }
        }
    }

    @Param("true", "false")
    var fast: Boolean = false

    @Setup
    fun setup() {
        println("Setup called (fast: $fast)")
        retainedXml = when {
            fast -> XML1_0.fast()
            else -> XML1_0.recommended()
        }

    }

    @Benchmark
    fun testDeserializeGenericSpeed(bh : Blackhole) = testDeserializeGenericSpeedImpl(BlackholeWrapperImpl(bh))

    fun testDeserializeGenericSpeedImpl(bh: BlackholeWrapper) {
        val xml =
            when {
                fast -> XML1_0.fast()
                else -> XML1_0.recommended {
                    policy { throwOnRepeatedElement = true }
                }
            }

        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testDeserializeAndParseSpeed(bh, xml)
        xmlStreaming.setFactory(null)
    }

    @Benchmark
    fun testDeserializeGenericSpeedRetainedXml(bh : Blackhole) =
        testDeserializeGenericSpeedImpl(BlackholeWrapperImpl(bh))

    fun testDeserializeGenericSpeedRetainedXml(bh : BlackholeWrapper) {
        xmlStreaming.setFactory(xmlStreaming.genericFactory)
        testDeserializeAndParseSpeed(bh, retainedXml)
        xmlStreaming.setFactory(null)
    }

    @Benchmark
    fun testDeserializeNoparseRetained(bh : Blackhole) = testDeserializeNoparseRetained(BlackholeWrapperImpl(bh))

    fun testDeserializeNoparseRetained(bh : BlackholeWrapper) {
        testDeserializeOnlySpeed(bh, retainedXml)
    }

    @Benchmark
    fun testDeserializeStaxSpeed(bh : Blackhole) = testDeserializeStaxSpeed(BlackholeWrapperImpl(bh))

    fun testDeserializeStaxSpeed(bh : BlackholeWrapper) {
        val xml =
            when {
                fast -> XML1_0.fast()
                else -> XML1_0.recommended {
                    policy { throwOnRepeatedElement = true }
                }
            }

        xmlStreaming.setFactory(StAXStreamingFactory())
        testDeserializeAndParseSpeed(bh, xml)
        xmlStreaming.setFactory(null)
    }

    private fun testDeserializeAndParseSpeed(bh : BlackholeWrapper, xml: XML) {
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

    private fun testDeserializeOnlySpeed(bh : BlackholeWrapper, xml: XML) {
        var count = 0
        for ((u, reader) in readers) {
            try {
                reader.reset()
                ++count
                bh.consume(xml.decodeFromReader<XSSchema>(reader))
            } catch (e: Exception) {
                throw AssertionError("Failure to read $u:\n${e.message?.prependIndent("        ")}", e)
            }
        }
    }


}
