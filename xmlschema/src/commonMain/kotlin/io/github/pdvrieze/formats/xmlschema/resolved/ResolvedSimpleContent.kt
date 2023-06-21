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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContentExtension
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContentRestriction
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_ComplexType
import nl.adaptivity.xmlutil.QName

class ResolvedSimpleContent(
    private val scope: ResolvedComplexType,
    override val rawPart: XSSimpleContent,
    schema: ResolvedSchemaLike
) : ResolvedComplexTypeContent(schema),
    T_ComplexType.SimpleContent {

    override val derivation: ResolvedSimpleContentDerivation by lazy {
        when (val d = rawPart.derivation) {
            is XSSimpleContentExtension -> ResolvedSimpleContentExtension(scope, d, schema)
            is XSSimpleContentRestriction -> ResolvedSimpleContentRestriction(d, schema)
            else -> error("unsupported derivation")
        }
    }

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super.check()
        derivation.check(seenTypes, inheritedTypes)


        //TODO("not implemented")
    }
}
