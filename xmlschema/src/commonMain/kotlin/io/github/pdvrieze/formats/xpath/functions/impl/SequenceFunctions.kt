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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.data.XdmType
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdInteger

@XPathInternal
object SequenceFunctions : AbstractFunctionObject() {

    val fnEmpty = BuiltinFunctionImpl("empty", functionType(XdmType.BOOLEAN, XdmType.ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdInteger(0))
        XdmAtomic(XsdBoolean(arg.size==0))
    }

    val fnExists = BuiltinFunctionImpl("exists", functionType(XdmType.BOOLEAN, XdmType.ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdInteger(0))
        XdmAtomic(XsdBoolean(arg.size>0))
    }

    val fnCount = BuiltinFunctionImpl("count", functionType(XdmType.INTEGER, XdmType.ITEM.any)) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdInteger(0))
        XdmAtomic(XsdInteger(arg.size))
    }

}
