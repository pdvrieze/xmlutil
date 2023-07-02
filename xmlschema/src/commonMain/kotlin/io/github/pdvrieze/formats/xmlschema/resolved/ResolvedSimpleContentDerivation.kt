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
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexType
import io.github.pdvrieze.formats.xmlschema.types.T_Type
import nl.adaptivity.xmlutil.QName

sealed class ResolvedSimpleContentDerivation(override val schema: ResolvedSchemaLike) : ResolvedPart,
    T_ComplexType.SimpleDerivation {
    abstract override val rawPart: T_ComplexType.SimpleDerivation

    abstract val baseType: ResolvedType

    override val annotation: XSAnnotation? get() = rawPart.annotation

    override val id: VID? get() = rawPart.id

    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    abstract fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>)

}