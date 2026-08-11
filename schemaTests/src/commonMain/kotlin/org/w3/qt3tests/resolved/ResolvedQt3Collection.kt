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

package org.w3.qt3tests.resolved

import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI

@OptIn(XPathInternal::class)
class ResolvedQt3Collection(
    val elements: List<Element>,
    val uri: XsdAnyURI?
) {

    private var values: XdmValue<*>? = null

//    context(ctx: ResolutionContext)
    fun getValues(): XdmValue<*> {
        return values ?: run {
            XdmSequence.fromList(elements.flatMap { it.asXdmValue() })
        }.also { values = it }

    }

    sealed interface Element {
        fun asXdmValue(): XdmValue<*>
    }
}
