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

import io.github.pdvrieze.formats.xpath.data.XdmSchemaType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants

@XPathInternal
sealed class ArrayTypeTest: ItemTypeTest {
    @NeedsXPath3_1
    object ANY: ArrayTypeTest() {
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("array(*)")
        }

        @OptIn(NeedsXPath3_0::class)
        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            // 2.5.6.2 #33
            if (baseType == FunctionTypeTest.ANY) return true

            // 2.5.6.2 #34
            if (baseType is FunctionTypeTest.Typed) {
                val paramType: SequenceType = (baseType.paramTypes.singleOrNull()) ?: return false
                val evalType = paramType.eval() as? XdmSchemaType ?: return false
                return evalType.schemaType.name?.isEquivalent(QName(XMLConstants.XSD_NS_URI, "integer")) == true
            }

            return baseType == ANY
        }
    }

    class Typed @NeedsXPath3_1 constructor(val elemType: SequenceType): ArrayTypeTest() {

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            TODO("not implemented")
        }

        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("array(")
            elemType.appendToString(builder)
            builder.append(")")
        }
    }
}
