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
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.dom.localName
import nl.adaptivity.xmlutil.dom2.*

@XPathInternal
object NodeFunctions : AbstractFunctionObject() {

    val fnName: BuiltinFunctionImpl<XdmAtomic<XsdString>> = BuiltinFunctionImpl(
        "name",
        contextFunctionTypes(STRING, NODE.opt)
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdString(""))
        val name = when(val n = arg.node) {
            is Element -> n.nodeName
            is Attr -> n.nodeName
            is ProcessingInstruction -> n.target
            else -> ""
        }
        XdmAtomic(XsdString(name))
    }

    val fnLocalName: BuiltinFunctionImpl<XdmAtomic<XsdString>> = BuiltinFunctionImpl(
        "local-name",
        contextFunctionTypes(STRING, NODE.opt)
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdString(""))
        val name: String = when (val n = arg.node) {
            is Element -> n.getLocalName()!!
            is Attr -> n.localName ?: n.nodeName
            is ProcessingInstruction -> n.target
            else -> ""
        }
        XdmAtomic(XsdString(name))
    }

    val fnNamespaceUri: BuiltinFunctionImpl<XdmAtomic<XsdString>> = BuiltinFunctionImpl(
        "namespace-uri",
        contextFunctionTypes(STRING, NODE.opt)
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdString(""))
        val name = when(val n = arg.node) {
            is Element -> n.namespaceURI
            is Attr -> n.namespaceURI
            else -> ""
        }
        XdmAtomic(XsdString(name ?: ""))
    }

    val fnLang: BuiltinFunctionImpl<XdmAtomic<XsdBoolean>> = BuiltinFunctionImpl(
        "lang",
        contextFunctionTypes(BOOLEAN, NODE, STRING.opt)
    ) { args ->
        val testLang = when {
            args.isEmpty() -> throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT)
            else -> ((args[0] as? XdmAtomic<*>)?.value as? XsdString)?.xmlString
                ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected xs:string, found: ${args[0].staticType}")
        }

        val arg1 = (argOrContext(1, args) ?: throw EvaluationException(ErrorCodes.XPDY0002_ABSENT_DYNAMIC_CONTEXT))
        val node: Node = (arg1 as? XdmNode ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Expected node, found: ${arg1.staticType}")).node

        val effectiveLang = generateSequence<Node>(node) { it.parentNode as? Element }
            .filterIsInstance<Element>()
            .mapNotNull { it.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang") }
            .firstOrNull()

        val r = when {
            effectiveLang == null -> false
            effectiveLang == testLang -> true
            effectiveLang.startsWith("${testLang}-") -> true
            else -> false
        }

        XdmAtomic(XsdBoolean(r))

    }

    val fnRoot: BuiltinFunctionImpl<XdmValue> = BuiltinFunctionImpl("root",
        listOf(
            functionType(NODE.single),
            functionType(NODE.opt, NODE.opt)
        )
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY

        val r = generateSequence(arg.node) { it.getParentNode() }.last()
        XdmNode(r)
    }

    val fnPath: BuiltinFunctionImpl<XdmAtomicOrEmpty<XdmAtomic<XsdString>>> = BuiltinFunctionImpl(
        "path",
        contextFunctionTypes(STRING, NODE.opt)
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val n = arg.node

        val elemPath =
            generateSequence((n as? Element) ?: (n.parentNode as? Element)) { it.getParentNode() as? Element }
                .map { a ->
                    val position = generateSequence(a.previousSibling) { it.previousSibling }
                        .filterIsInstance<Element>()
                        .count { it.namespaceURI == a.namespaceURI && it.localName == a.localName } + 1
                    "Q{${a.namespaceURI}}${a.localName}[$position]"
                }
                .toList()
                .reversed()

        val path = buildString {
            elemPath.joinTo(this, "/") { it }
            when (n) {
                is Attr if (n.namespaceURI.isNullOrBlank()) -> append("/@").append(n.localName)
                is Attr -> append("/@Q{").append(n.namespaceURI).append("}").append(n.localName)

                is Text -> {
                    val position = generateSequence(n.previousSibling) { it.previousSibling }
                        .count { it is Text } + 1
                    append("/text()[").append(position).append(']')
                }

                is Comment -> {
                    val position = generateSequence(n.previousSibling) { it.previousSibling }
                        .count { it is Comment } + 1
                    append("/comment()[").append(position).append(']')
                }

                is ProcessingInstruction -> {
                    val position = generateSequence(n.previousSibling) { it.previousSibling }
                        .count { it is ProcessingInstruction && it.target == n.target } + 1
                    append("/processing-instruction(").append(n.target).append(")[").append(position).append(']')
                }
            }
        }

        XdmAtomic(XsdString(path))
    }

    val fnHasChildren: BuiltinFunctionImpl<XdmAtomic<XsdBoolean>> = BuiltinFunctionImpl(
        "has-children",
        contextFunctionTypes(BOOLEAN, NODE.opt)
    ) { args ->
        val arg = toSingleNode(args) ?: return@BuiltinFunctionImpl XdmAtomic(XsdBoolean.FALSE)
        XdmAtomic(XsdBoolean(arg.node.getChildNodes().getLength() > 0))
    }

    val fnInnermost: BuiltinFunctionImpl<XdmValue> = BuiltinFunctionImpl(
        "innermost",
        contextFunctionTypes(NODE.any, NODE.any)
    ) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY

        TODO("Not yet implemented")

    }

    val fnOutermost: BuiltinFunctionImpl<XdmValue> = BuiltinFunctionImpl(
        "outermost",
        contextFunctionTypes(NODE.any, NODE.any)
    ) { args ->
        val arg = toSingleArg(args) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY

        TODO("Not yet implemented")
    }

}
