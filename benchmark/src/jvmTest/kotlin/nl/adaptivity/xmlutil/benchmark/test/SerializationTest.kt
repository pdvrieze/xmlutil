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

package nl.adaptivity.xmlutil.benchmark.test

import kotlinx.benchmark.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.benchmark.Deserialization
import nl.adaptivity.xmlutil.benchmark.Serialization
import nl.adaptivity.xmlutil.benchmark.util.*
import nl.adaptivity.xmlutil.serialization.structure.*
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
        measure("serialize to StringWriter",5) { testSerializeGenericSpeedImpl(DummyBlackHole) }
    }

}
