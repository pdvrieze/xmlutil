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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

class ResolvedLocalComplexType(
    override val rawPart: T_LocalComplexType_Base,
    override val schema: ResolvedSchemaLike
) : ResolvedLocalType, ResolvedComplexType, T_LocalComplexType_Base {
    override val mixed: Boolean? get() = rawPart.mixed
    override val defaultAttributesApply: Boolean? get() = rawPart.defaultAttributesApply
    override val name: VNCName? get() = rawPart.name
    override val annotations: List<XSAnnotation> get() = rawPart.annotations
    override val id: VID? get() = rawPart.id
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val content: ResolvedComplexContent by lazy {
        when (val c = rawPart.content as T_ComplexTypeContentSealed) {
            is T_ComplexTypeComplexContent -> ResolvedComplexComplexContent(c, schema)
            is T_ComplexTypeShorthandContent -> ResolvedComplexShorthandContent(c, schema)
            is T_ComplexTypeSimpleContent -> ResolvedComplexSimpleContent(c, schema)
        }
    }

    override fun check(seenTypes: SingleLinkedList<QName>) {
        content.check(name?.let { seenTypes + it.toQname(schema.targetNamespace) } ?: seenTypes)
    }
}

