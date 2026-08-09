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

package org.w3.qt3tests.resolved.assertions

import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmValue
import io.github.pdvrieze.formats.xpath.eval.data.dom.*
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.core.internal.QNameMap
import nl.adaptivity.xmlutil.dom2.*
import org.w3.qt3tests.resolved.ResolvedQt3TestCase

@OptIn(XPathInternal::class)
class ResolvedQt3AssertXML(
    val assertion: XdmDocumentFragment,
    val file: XsdAnyURI?,
    val ignorePrefixes: Boolean
): ResolvedQt3Assertion() {
    override fun verify(evalResult: Result<XdmValue<*>>, testCase: ResolvedQt3TestCase): AssertionResult {
        val evalResult = evalResult.getOrElse { return AssertionResult.Failure(it) }
        val resultFrag: XdmDocumentFragment = when (evalResult) {
            is XdmDocumentFragment -> evalResult
            is XdmDocument -> evalResult.createDocumentFragment().apply { evalResult.getDocumentElement()?.let { appendChild(it) } }

            is XdmNode<*> -> evalResult.getOwnerDocument()!!.createDocumentFragment().apply {
                appendChild(evalResult)
            }

            else -> return AssertionResult.Failure(AssertionError("Unsupported result type: ${evalResult.staticType} with value ($evalResult)"))
        }

        if (assertion.childNodes.getLength() != resultFrag.childNodes.length) {
            return AssertionResult.Failure(AssertionError("Node count mismatch"))
        }

        for ((expectedChild, actualChild) in assertion.getChildNodes().iterator().asSequence().zip(resultFrag.getChildNodes().iterator().asSequence())) {
            compare(expectedChild, actualChild).onFailure { return it }
        }

        return AssertionResult.Success
    }

    fun compare(expectedNode: XdmNode<*>, actualNode: XdmNode<*>): AssertionResult {
        if (expectedNode.getNodetype() != actualNode.getNodetype()) return AssertionResult.Failure(AssertionError("Node type mismatch: ${expectedNode.getNodetype()} != ${actualNode.getNodetype()}"))
        return when (expectedNode) {
            is XdmElement -> compare(expectedNode, actualNode as XdmElement)
            is XdmCharacterData -> compare(expectedNode.getTextContent(), (actualNode as XdmCharacterData).getTextContent())
            is XdmProcessingInstruction -> compare(expectedNode, actualNode as XdmProcessingInstruction)
            else -> AssertionResult.Failure(IllegalArgumentException("Unsupported type: ${expectedNode.getNodetype()}"))
        }
    }

    fun compare(expectedNode: XdmElement, actualNode: XdmElement): AssertionResult {
        var r = compare(expectedNode.localName, actualNode.localName)
            .flatMap { compare(expectedNode.namespaceURI, actualNode.namespaceURI) }
        if (! ignorePrefixes)
            r = r.flatMap { compare(expectedNode.prefix, actualNode.prefix) }

        val expectedAttrs = QNameMap<String>().also { m ->
            for (attr in expectedNode.getAttributes()) {
                if (attr.namespaceURI != XMLConstants.XMLNS_ATTRIBUTE_NS_URI) m[attr.getQName()] = attr.getValue()
            }
        }

        if (r is AssertionResult.Failure) return r

        for (attr in actualNode.getAttributes()) {
            val expectedValue = expectedAttrs.remove(attr.getQName()) ?: return AssertionResult.Failure(AssertionError("Missing attribute (${attr.getQName()}) for ${attr.getQName()})"))
            compare(expectedValue, attr.getValue()).onFailure { return it }
        }

        if (expectedAttrs.isNotEmpty()) {
            return AssertionResult.Failure(AssertionError("Unexpected attributes: ${expectedAttrs.toList()}"))
        }

        return AssertionResult.Success
    }

    fun compare(expectedNode: XdmProcessingInstruction, actualNode: XdmProcessingInstruction): AssertionResult {
        return compare(expectedNode.getTarget(), actualNode.getTarget())
            .flatMap { compare(expectedNode.getTextContent(), actualNode.getTextContent()) }

    }

    fun compare(expected: String?, actual: String?): AssertionResult {
        return when (expected) {
            actual -> AssertionResult.Success
            else -> AssertionResult.Failure(AssertionError("Text not equal: '$expected' != '$actual'"))
        }

    }
}
