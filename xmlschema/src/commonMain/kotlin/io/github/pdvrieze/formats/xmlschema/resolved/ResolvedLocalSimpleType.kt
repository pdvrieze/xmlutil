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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

class ResolvedLocalSimpleType(
    override val rawPart: XSLocalSimpleType,
    override val schema: ResolvedSchemaLike,
    context: VSimpleTypeScope.Member,
    inheritedTypes: SingleLinkedList<ResolvedType>,
) : ResolvedLocalType, ResolvedSimpleType {

    override val mdlScope: VSimpleTypeScope.Local = VSimpleTypeScope.Local(context)
    override val mdlContext: VTypeScope.MemberBase get() = mdlScope.parent
    override val mdlFinal: Set<VDerivationControl.Type> = schema.finalDefault

    override val simpleDerivation: ResolvedSimpleType.Derivation = when (val raw = rawPart.simpleDerivation) {
        is XSSimpleUnion -> ResolvedUnionDerivation(raw, schema, this, inheritedTypes)
        is XSSimpleList -> ResolvedListDerivation(raw, schema, this, inheritedTypes)
        is XSSimpleRestriction -> ResolvedSimpleRestriction(raw, schema, this, inheritedTypes)
        else -> error("Derivations must be union, list or restriction")
    }

    override val model: Model by lazy { ModelImpl(rawPart, schema, context, inheritedTypes) }

    interface Model : ResolvedSimpleType.Model {
        val mdlContext: VSimpleTypeScope.Member
    }

    private inner class ModelImpl(
        rawPart: XSLocalSimpleType,
        schema: ResolvedSchemaLike,
        override val mdlContext: VSimpleTypeScope.Member,
        inheritedTypes: SingleLinkedList<ResolvedType>,
    ) : ResolvedSimpleType.ModelBase(rawPart, schema, this@ResolvedLocalSimpleType, inheritedTypes), Model {
    }

    override fun checkType(checkHelper: CheckHelper, inheritedTypes: SingleLinkedList<ResolvedType>) {
        super<ResolvedSimpleType>.checkType(checkHelper, inheritedTypes)
        checkNotNull(model)
    }
}
