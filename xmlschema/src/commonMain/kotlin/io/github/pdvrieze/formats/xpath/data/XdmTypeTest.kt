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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.ItemTypeTest
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0
import io.github.pdvrieze.formats.xpath.impl.XPathInternal

@OptIn(XPathInternal::class, NeedsXPath3_0::class)
class XdmTypeTest(val itemType: ItemTypeTest) : XdmSingleType() {
    context(ctx: ExprEvalContext)
    override fun isSubtypeOf(other: XdmSingleType): Boolean {
        TODO("not implemented")
    }

    override fun toString(): String = "$itemType"

    context(ctx: ExprEvalContext)
    override fun fromString(value: String): XdmValue {
        throw UnsupportedOperationException("Type tests cannot be created from strings")
    }
}
