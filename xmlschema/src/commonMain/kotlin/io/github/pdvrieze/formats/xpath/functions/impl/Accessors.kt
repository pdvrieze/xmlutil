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
import nl.adaptivity.xmlutil.dom2.textContent

@XPathInternal
object Accessors : AbstractFunctionObject() {
    val fnNodeName: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmQName>> = BuiltinFunctionImpl(
        "node-name",
        contextFunctionTypes(t(QNameType.Instance).opt, NODE.opt)
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdQName(arg.node.nodeName))
    }

    val fnNilled: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmBoolean>> = BuiltinFunctionImpl(
        "nilled", contextFunctionTypes(BOOLEAN.opt, NODE.opt)
    ) { args ->
        val arg = (toSingleNode(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY)
            .node
        XdmAtomic(
            XsdBoolean(
                arg is Element &&
                        arg.getAttributeNS(XMLConstants.XSI_NS_URI, "nil").let { v ->
                            v != null && XsdBoolean(v).value
                        })
        )

    }

    val fnString: BuiltinFunctionImpl<XdmAtomic<XsdString>> = BuiltinFunctionImpl(
        "string",
        contextFunctionTypes(STRING.opt, ITEM.opt)
    ) { args ->
        val arg = toSingleArg(args) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        if (arg.size == 0) return@BuiltinFunctionImpl XdmAtomic(XsdString(""))

        val s = buildString {
            for (i in 0 until arg.size) {
                when (val a = arg[i]) {
                    is XdmNode -> append(a.node.textContent?:"")
                    is XdmAtomic<*> -> append(a.value.xmlString)
                    is XdmFunction<*> -> throw EvaluationException(ErrorCodes.FOTY0014_FN_IN_TOSTRING, "Type has no text content: ${a.staticType}")
                }
            }
        }

        XdmAtomic(XsdString(s))
    }

    val fnData: BuiltinFunctionImpl<XdmAtomicOrSequence<XdmAtomic<*>>> = BuiltinFunctionImpl(
        "data",
        contextFunctionTypes(ATOMIC.any, ITEM.any)
    ) { args ->
        (toSingleArg(args) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT))
            .atomize()
    }

    val fnBaseUri: BuiltinFunctionImpl<XdmAtomic<XsdAnyURI>> = BuiltinFunctionImpl(
        "base-uri",
        contextFunctionTypes(t(AnyURIType.Instance).opt, NODE.opt)
    ) { args ->
        val node = toSingleNode(args) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        TODO("Needs XdmNode to properly implement DOM and not do delegation")
    }

    val fnDocumentUri: BuiltinFunctionImpl<XdmAtomic<XsdAnyURI>> = BuiltinFunctionImpl("document-uri",
        contextFunctionTypes(AnyURIType.Instance.opt, NODE.opt)
    ) { args ->
        val node = toSingleNode(args) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT)
        TODO("Needs XdmNode to properly implement DOM and not do delegation")
    }

}
