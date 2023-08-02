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
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

interface ResolvedBuiltinType : ResolvedGlobalType, ResolvedSimpleType {
    override val rawPart: Nothing get() = throw UnsupportedOperationException("Builtins have no raw parts")
    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) = Unit
    override val schema: ResolvedSchemaLike get() = BuiltinSchemaXmlschema
    override val annotation: Nothing? get() = null
    override val id: Nothing? get() = null
    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()
    override val mdlAnnotations: Nothing? get() = null
    override val mdlVariety: ResolvedSimpleType.Variety get() = ResolvedSimpleType.Variety.ATOMIC
    override val mdlFinal: Set<VDerivationControl.Type> get() = emptySet()
    override val mdlTargetNamespace: VAnyURI? get() = BuiltinSchemaXmlschema.targetNamespace
    override val mdlBaseTypeDefinition: ResolvedSimpleType
    override val mdlItemTypeDefinition: ResolvedSimpleType?
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
}
