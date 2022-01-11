/*
 * Copyright (c) 2021.
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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.Serializable

@OptIn(ExperimentalUnsignedTypes::class)
class InlineCounterTest: PlatformTestBase<InlineCounterTest.Counter>(
    Counter(239.toUByte(), "tries"),
    Counter.serializer()
                                                            ) {
    override val expectedXML: String
        get() = "<Counter counted=\"239\" description=\"tries\"/>"
    override val expectedJson: String
        get() = "{\"counted\":239,\"description\":\"tries\"}"


    @Serializable
    data class Counter(val counted: UByte, val description:String) {
//    constructor(counted: UByte, description:String): this(counted)
    }

}
