/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.benchmark.test

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import kotlinx.benchmark.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.benchmark.Serialization
import nl.adaptivity.xmlutil.benchmark.util.DummyBlackHole
import nl.adaptivity.xmlutil.benchmark.util.measure
import nl.adaptivity.xmlutil.serialization.XML
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test

@State(Scope.Benchmark)
@Measurement(iterations = 10)
@Warmup(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class SerializationTest : Serialization() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun testSerializeGenericSpeed() {
        measure("serialize to StringWriter",20) { testSerializeGenericSpeedImpl(DummyBlackHole) }
    }

    @Test
    fun testAttributePositionRegression() {
        val schemaName = "/xsts/ibmData/valid/S3_12/s3_12v03.xsd"
        val xml = XML.v1()
        val schemaText = String(javaClass.getResourceAsStream(schemaName)!!.readAllBytes())
        val schema = xml.decodeFromString<XSSchema>(schemaText)


        val serialized = xml.encodeToString(schema)
//        assertXmlEquals(schemaText, serialized)
    }

}
