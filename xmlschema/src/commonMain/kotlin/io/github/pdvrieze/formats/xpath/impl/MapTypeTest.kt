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

package io.github.pdvrieze.formats.xpath.impl

import nl.adaptivity.xmlutil.XmlWriter

@XPathInternal
sealed class MapTypeTest: ItemTypeTest {
    object ANY: MapTypeTest() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append("map(*)")
        }
    }

    class Typed(val inputType: AtomicOrUnionTypeTest, val outputType: SequenceTypeTest): MapTypeTest() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append("map(")
            builder.appendQName(inputType, output)
            builder.append(", ")
            outputType.appendToString(builder, output)
            builder.append(")")
        }
    }
}

