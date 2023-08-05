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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSFacet
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.util.CompactFragment

abstract class ResolvedSimpleRestrictionBase(rawPart: XSSimpleRestriction?, schema: ResolvedSchemaLike) :
    ResolvedSimpleType.Derivation(rawPart, schema) {

    abstract override val rawPart: XSSimpleRestriction

    open val otherContents: List<CompactFragment> get() = rawPart.otherContents

    open val base: QName? get() = rawPart.base

    open val facets: List<XSFacet> get() = rawPart.facets

    abstract val simpleType: ResolvedLocalSimpleType?

    override val baseType: ResolvedSimpleType by lazy {
        base?.let{ schema.simpleType(it) } ?: checkNotNull(simpleType)
    }

    override fun checkDerivation(checkHelper: CheckHelper, inheritedTypes: SingleLinkedList<QName>) {
        val b = base
        if (b == null) {
            requireNotNull(simpleType)
        } else {
            require(simpleType == null)
        }
        check(b !in inheritedTypes.dropLastOrEmpty()) { "Indirect recursive use of simple base types: $b in ${inheritedTypes.last()}"}
        val inherited = (baseType as? ResolvedGlobalType)?.mdlQName?.let(::SingleLinkedList) ?: SingleLinkedList.empty()
        baseType.checkType(checkHelper, inherited)
    }
}
