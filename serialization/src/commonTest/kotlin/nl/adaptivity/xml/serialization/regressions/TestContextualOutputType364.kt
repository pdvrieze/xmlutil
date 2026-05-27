/*
 * Copyright (c) 2026.
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

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TestContextualOutputType364 {
    @OptIn(ExperimentalTime::class)
    val xml = XML.v1(SerializersModule {
        contextual(Instant::class, Instant.serializer())
    }) {
        defaultToGenericParser = true
    }

    @Test
    fun bug() {
        val actual = xml.encodeToString(
            Bug.serializer(),
            Bug(BugEnum.ABC, Instant.fromEpochSeconds(0))
        )

        assertXmlEquals("<Bug date=\"1970-01-01T00:00:00Z\"><BugEnum>ABC</BugEnum></Bug>", actual)
    }

    @Serializable
    data class Bug(
        val enum: BugEnum,
        @Contextual val date: Instant,
    )

    enum class BugEnum {
        ABC,
    }
}
