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

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.*
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyURIType
import io.github.pdvrieze.xml.schematypes.types.QNameType
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.nodeName

@XPathInternal
object Accessors : AbstractFunctionObject() {
    val fnNodeName: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmQName>> = BuiltinFunctionImpl.Fn(
        "node-name",
        contextFunctionTypes(t(QNameType.Instance).opt, NODE.opt)
    ) Fn@{ args ->
        val arg = args.toSingleNode() ?: return@Fn XdmSequence.EMPTY
        atomic(XsdQName(arg.nodeName))
    }

    val fnNilled: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmBoolean>> = BuiltinFunctionImpl.Fn(
        "nilled", contextFunctionTypes(BOOLEAN.opt, NODE.opt)
    ) Fn@{ args ->
        val arg = (args.toSingleNode() ?: return@Fn XdmSequence.EMPTY)
        atomic(
            arg is Element &&
                    arg.getAttributeNS(XMLConstants.XSI_NS_URI, "nil")
                .let { v -> v != null && XsdBoolean(v).value })

    }

    val fnString: BuiltinFunctionImpl<XdmAtomic<XsdString>> = BuiltinFunctionImpl.Fn(
        "string",
        contextFunctionTypes(STRING.opt, ITEM.opt)
    ) Fn@{ args ->
        val arg = args.argOrContext() ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        if (arg.size == 0) return@Fn XdmAtomic(XsdString(""))
        if (arg.size > 1) error("Can only convert a sequence of 1 item to a string")

        @Suppress("UNCHECKED_CAST")
        when (val a = arg[0]) {
            is XdmNodeBase<*> -> atomic(a.asT().getTextContent() ?: "")
            is XdmAtomic<*> -> when (val v = a.value) {
                is XsdString -> a as XdmAtomic<XsdString>
                else -> atomic(v.xmlString)
            }
            is XdmFunction<*> -> throw EvaluationException(ErrorCodes.FOTY0014_FN_IN_TOSTRING, "Type has no text content: ${a.staticType}")
        }
    }

    val fnData: BuiltinFunctionImpl<XdmAtomicOrSequence<XdmAtomic<*>>> = BuiltinFunctionImpl.Fn(
        "data",
        contextFunctionTypes(ATOMIC.any, ITEM.any)
    ) Fn@{ args ->
        (args.argOrContext() ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT))
            .atomize()
    }

    val fnBaseUri = BuiltinFunctionImpl.Fn(
        "base-uri",
        contextFunctionTypes(t(AnyURIType.Instance).opt, NODE.opt)
    ) Fn@{ args ->
        val node = args.toSingleNode(true)
            ?: return@Fn XdmSequence.EMPTY

        atomicOrNull(node.getBaseURI()?.let { XsdAnyURI(it) })
//        TODO("Needs XdmNode to properly implement DOM and not do delegation")
    }

    val fnDocumentUri: BuiltinFunctionImpl<XdmAtomic<XsdAnyURI>> = BuiltinFunctionImpl.Fn("document-uri",
        contextFunctionTypes(AnyURIType.Instance.opt, NODE.opt)
    ) Fn@{ args ->
        val node = args.toSingleNode(true) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        TODO("Needs XdmNode to properly implement DOM and not do delegation")
    }

}
