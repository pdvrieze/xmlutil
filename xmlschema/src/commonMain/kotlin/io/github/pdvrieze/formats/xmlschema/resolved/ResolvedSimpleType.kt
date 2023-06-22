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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import nl.adaptivity.xmlutil.QName

sealed interface ResolvedSimpleType : ResolvedType, T_SimpleType {
    override val simpleDerivation: Derivation

    override fun check(
        seenTypes: SingleLinkedList<QName>,
        inheritedTypes: SingleLinkedList<QName>
    ) { // TODO maybe move to toplevel

        when (val n = name) {
            null -> simpleDerivation.check(SingleLinkedList(), inheritedTypes)
            else -> {
                val qName = n.toQname(schema.targetNamespace)
                simpleDerivation.check(SingleLinkedList(qName), inheritedTypes + qName)
            }
        }
    }

    sealed class Derivation(final override val schema: ResolvedSchemaLike) : T_SimpleType.Derivation, ResolvedPart {
        final override val annotation: XSAnnotation? get() = rawPart.annotation
        final override val id: VID? get() = rawPart.id

        abstract override val rawPart: T_SimpleType.Derivation
        abstract val baseType: ResolvedSimpleType
        abstract fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>)
    }
}
