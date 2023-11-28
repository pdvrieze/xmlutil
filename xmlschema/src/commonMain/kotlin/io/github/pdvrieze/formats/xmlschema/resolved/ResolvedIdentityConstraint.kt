/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.regex.XRegex
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

sealed interface ResolvedIdentityConstraint : ResolvedAnnotated {
    val selector: XSSelector

    val mdlQName: QName?

    /**
     * At least 1 if selector is present
     */
    val fields: List<XSField>

    val owner: ResolvedElement
    val constraint: ResolvedIdentityConstraint
    val mdlIdentityConstraintCategory: Category
    val mdlSelector: XPathExpression
    val mdlFields: List<XPathExpression>

    companion object {
        operator fun invoke(
            rawPart: XSIdentityConstraint,
            schema: ResolvedSchemaLike,
            context: ResolvedElement
        ): ResolvedIdentityConstraint = when (rawPart) {
            is XSKey -> ResolvedKey(rawPart, schema, context)
            is XSUnique -> ResolvedUnique(rawPart, schema, context)
            is XSKeyRef -> ResolvedKeyRef(rawPart, schema, context)
        }

        //language=XsdRegExp
        val SELECTORPATTERN = XRegex("(\\.\\s*//\\s*)?" + // optional start with './/'
                "(((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)\\s*" + // then (child::)?<qname> or '.'
                "(/\\s*(((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)\\s*)*" + // followed by a sequence of above (starting with '/'
                "(\\|\\s*" + // followed by '|' separated repeated
                    "(\\.//\\s*)?" + // start with './/'
                        "(((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)\\s*" + // then qname or '.'
                        "(/\\s*(((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)\\s*)*" + // then optionally more qnames
                ")*", SchemaVersion.V1_1)

        //language=XsdRegExp
        val FIELDPATTERN = XRegex("(\\.\\s*//\\s*)?" + // optional start with './/'
                "((((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)\\s*/\\s*)*" +
                "(" +
                    "(((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)|" +
                    "((attribute\\s*::\\s*|@\\s*)((\\i\\c*:)?(\\i\\c*|\\*)))" +
                ")\\s*" +
                "(\\|\\s*(\\.//\\s*)?" +
                    "((((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)\\s*/\\s*)*" +
                    "(" +
                        "(((child\\s*::\\s*)?((\\i\\c*:)?(\\i\\c*|\\*)))|\\.)|((attribute\\s*::\\s*|@\\s*)((\\i\\c*:)?(\\i\\c*|\\*)))))*", SchemaVersion.V1_1)

        /*
        (\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*
         */

    }

    enum class Category { KEY, KEYREF, UNIQUE }

    fun checkConstraint(checkHelper: CheckHelper) {
        super.checkAnnotated(checkHelper.version)
        for (field in fields) {
            for (otherAttrName in field.otherAttrs.keys) {
                check(otherAttrName.namespaceURI.let { it.isNotEmpty() && it != XmlSchemaConstants.XS_NAMESPACE })
            }
            check(FIELDPATTERN.matches(field.xpath.xmlString)) {
                "Invalid xpath expression for field: '${field.xpath.xmlString}'"
            }
        }
        check(SELECTORPATTERN.matches(mdlSelector.test)) { "Invalid xpath expression for selectors: '${mdlSelector.test}'" }
    }
}
