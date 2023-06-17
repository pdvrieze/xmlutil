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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleDerivationSetElem
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TopLevelSimpleType
import nl.adaptivity.xmlutil.QName

interface ResolvedToplevelSimpleType : ResolvedToplevelType, ResolvedSimpleType, T_TopLevelSimpleType

fun ResolvedToplevelSimpleType(rawPart: XSToplevelSimpleType, schema: ResolvedSchemaLike): ResolvedToplevelSimpleType {
    return ResolvedToplevelSimpleTypeImpl(rawPart, schema)
}

class ResolvedToplevelSimpleTypeImpl(
    override val rawPart: XSToplevelSimpleType,
    override val schema: ResolvedSchemaLike
) : ResolvedToplevelSimpleType {
    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val name: VNCName
        get() = rawPart.name

    override val targetNamespace: VAnyURI
        get() = schema.targetNamespace

    override val simpleDerivation: ResolvedSimpleDerivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is XSSimpleUnion -> ResolvedSimpleUnionDerivation(raw, schema)
            is XSSimpleList -> ResolvedSimpleListDerivation(raw, schema)
            is XSSimpleRestriction -> ResolvedSimpleRestrictionDerivation(raw, schema)
            else -> error("unsupported derivation")
        }

    override val final: Set<T_SimpleDerivationSetElem>
        get() = rawPart.final

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super.check(seenTypes, inheritedTypes)
        require(name.isNotEmpty())
    }
}
