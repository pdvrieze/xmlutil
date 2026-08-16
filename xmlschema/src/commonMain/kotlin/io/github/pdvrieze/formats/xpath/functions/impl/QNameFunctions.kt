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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNCName
import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.dom.XdmElement
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.atomicArgOrEmpty
import io.github.pdvrieze.formats.xpath.functions.xdmArg
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.NodeKindTest
import io.github.pdvrieze.formats.xpath.impl.SequenceType
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.XsdString

@XPathInternal
object QNameFunctions : AbstractFunctionObject() {
    //region 10.1 Functions to create a QName

    @OptIn(NeedsXPath2::class)
    val fnResolveQName = BuiltinFunctionImpl.Fn(
        "resolve-QName",
        functionType(QNAME.single, STRING.opt, XdmNodeKindTest(NodeKindTest.ElementTest(), SequenceType.OccurrenceType.SINGLE))
    ) Fn@{ args ->
        val qname = args.atomicArgOrEmpty<XsdString>(0)?.xmlString ?: return@Fn XdmSequence.EMPTY
        val elem = args.xdmArg<XdmElement>(1)

        val cIdx = qname.indexOf(':')
        val prefix: String
        val localName: String
        when {
            cIdx >= 0 -> {
                prefix = qname.substring(0, cIdx)
                localName = qname.substring(cIdx + 1)
            }
            else -> {
                prefix = ""
                localName = qname
            }
        }
        if (! (prefix.isNCName() && localName.isNCName()))
            throw EvaluationException(ErrorCodes.FOCA0002, "QName '$qname' is not a valid QName")
        // TODO check qname syntax

        val nsUri = elem.lookupNamespaceURI(prefix) ?: throw EvaluationException(
            ErrorCodes.FONS0004,
            "Namespace prefix '$prefix' not bound to namespace"
        )

        atomic(XsdQName(nsUri, localName, prefix))
    }

    val fnQName = BuiltinFunctionImpl.Fn(
        "QName",
        functionType(QNAME.single, STRING.opt, STRING)
    ) Fn@{ args ->
        val nsUri = args.atomicArgOrEmpty<XsdString>(0)?.xmlString ?: ""
        val cName = args.atomicArgOrEmpty<XsdString>(1)?.xmlString ?: ""
        val cIdx = cName.indexOf(':')
        when {
            cIdx >= 0 -> atomic(XsdQName(nsUri, cName.substring(cIdx + 1), cName.substring(0, cIdx)))
            else -> atomic(XsdQName(nsUri, cName))
        }
    }

    //end region

    //region 10.2 Functions related to QNames
    val fnPrefixFromQName = BuiltinFunctionImpl.Fn(
        "prefix-from-QName",
        functionType(NCNAME.opt, QNAME.single)
    ) Fn@{ args ->
        val qName = args.atomicArgOrEmpty<XsdQName>(0) ?: return@Fn XdmSequence.EMPTY

        atomic(XsdNCName(qName.getPrefix()))
    }

    val fnLocalnameFromQName = BuiltinFunctionImpl.Fn(
        "local-name-from-QName",
        functionType(NCNAME.opt, QNAME.single)
    ) Fn@{ args ->
        val qName = args.atomicArgOrEmpty<XsdQName>(0) ?: return@Fn XdmSequence.EMPTY

        atomic(XsdNCName(qName.getLocalPart()))
    }

    val fnNamespaceUriFromQName = BuiltinFunctionImpl.Fn(
        "namespace-uri-from-QName",
        functionType(ANYURI.opt, QNAME.single)
    ) Fn@{ args ->
        val qName = args.atomicArgOrEmpty<XsdQName>(0) ?: return@Fn XdmSequence.EMPTY

        atomic(XsdAnyURI(qName.getNamespaceURI()))
    }

    @OptIn(NeedsXPath2::class)
    val fnNamespaceUriForPrefix = BuiltinFunctionImpl.Fn(
        "namespace-uri-for-prefix",
        functionType(ANYURI.single, STRING.opt, XdmNodeKindTest(NodeKindTest.ElementTest(), SequenceType.OccurrenceType.SINGLE))
    ) Fn@{ args ->
        val prefix = args.atomicArgOrEmpty<XsdString>(0)?.xmlString ?: return@Fn XdmSequence.EMPTY
        val elem = args.xdmArg<XdmElement>(1)

        val nsUri = elem.lookupNamespaceURI(prefix) ?: return@Fn XdmSequence.EMPTY

        atomic(nsUri)
    }

    @OptIn(NeedsXPath2::class)
    val fnInScopePrefixes = BuiltinFunctionImpl.Fn(
        "in-scope-prefixes",
        functionType(STRING.any, XdmNodeKindTest(NodeKindTest.ElementTest(), SequenceType.OccurrenceType.SINGLE))
    ) Fn@{ args ->
        val elem = args.xdmArg<XdmElement>(1)

        XdmSequence.buildAtomic {
            for (prefix in elem.inScopePrefixes()) {
                add(atomic(prefix))
            }
        }
    }

    //endregion

}
