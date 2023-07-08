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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSComplexContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexRestrictionType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

class ResolvedComplexRestriction(
    override val rawPart: XSComplexContent.XSRestriction,
    scope: ResolvedComplexType,
    schema: ResolvedSchemaLike
) : ResolvedDerivation(scope, schema), T_ComplexRestrictionType {

    override val simpleType: ResolvedLocalSimpleType? by lazy { rawPart.simpleType?.let {
        ResolvedLocalSimpleType(
            it,
            schema,
            scope
        )
    } }

    override val facets: List<XSFacet> get() = rawPart.facets

    override val otherContents: List<CompactFragment>
        get() = rawPart.otherContents

    override val attributes: List<ResolvedLocalAttribute> =
        DelegateList(rawPart.attributes) { ResolvedLocalAttribute(scope, it, schema) }

    override val attributeGroups: List<ResolvedAttributeGroupRef> =
        DelegateList(rawPart.attributeGroups) { ResolvedAttributeGroupRef(it, schema) }

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedDerivation>.check(seenTypes, inheritedTypes)
    }
}
