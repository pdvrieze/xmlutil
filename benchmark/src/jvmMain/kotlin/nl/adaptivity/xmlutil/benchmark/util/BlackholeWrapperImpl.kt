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

package nl.adaptivity.xmlutil.benchmark.util

import kotlinx.benchmark.Blackhole

class BlackholeWrapperImpl(val delegate: Blackhole) : BlackholeWrapper {
    override fun consume(value: Any?) {
        delegate.consume(value)
    }
}

object DummyBlackHole: BlackholeWrapper {
    private var x: Any? = null
    override fun consume(value: Any?) {
        x = value
    }
}
