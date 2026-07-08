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

package nl.adaptivity.xmlutil.test.regressions

import dev.mokkery.MockMode
import dev.mokkery.answering.returnsBy
import dev.mokkery.every
import dev.mokkery.matcher.capture.Capture
import dev.mokkery.matcher.capture.capture
import dev.mokkery.matcher.capture.get
import dev.mokkery.mock
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget

abstract class TestDoubleCommon {
    lateinit var mockEncoder: Encoder
        private set

    protected fun normalizeExpected(expected: String): String = when(testTarget) {
        is Target.Js if 'N' !in expected -> expected.toDouble().toString()
        else -> expected
    }

    protected open fun init() {
        mockEncoder = mock(MockMode.autoUnit) {
            val doubleArg = Capture.slot<Double>()
            every { encodeDouble(capture(doubleArg)) }.returnsBy {
                mockEncoder.encodeString(doubleArg.get().toString())
            }
        }

    }
}
