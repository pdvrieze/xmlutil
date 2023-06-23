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
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_NamedType
import io.github.pdvrieze.formats.xmlschema.types.T_GlobalSimpleType
import nl.adaptivity.xmlutil.QName

interface ResolvedBuiltinType : ResolvedGlobalType, ResolvedSimpleType, T_GlobalSimpleType {
    override val rawPart: T_NamedType get() = this
    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) = Unit
    override val schema: ResolvedSchemaLike get() = BuiltinXmlSchema
    override val annotation: Nothing? get() = null
    override val id: Nothing? get() = null
    override val otherAttrs: Map<QName, String> get() = emptyMap()
    override val mdlAnnotations: List<AnnotationModel> get() = emptyList()
    override val mdlVariety: SimpleTypeModel.Variety get() = SimpleTypeModel.Variety.ATOMIC
}
