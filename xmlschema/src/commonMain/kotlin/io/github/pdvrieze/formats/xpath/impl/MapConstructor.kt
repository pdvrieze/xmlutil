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

import io.github.pdvrieze.formats.xpath.XPath3_1

@XPathInternal
internal class MapConstructor @XPath3_1 constructor(val entries: List<Entry>): ExprSingle() {

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.append("map{")
        builder.joinHelper(entries) {
            it.key.appendToString(builder)
            append(" : ")
            it.value.appendToString(builder)
        }
    }

    class Entry(val key: ExprSingle, val value: ExprSingle)
}
