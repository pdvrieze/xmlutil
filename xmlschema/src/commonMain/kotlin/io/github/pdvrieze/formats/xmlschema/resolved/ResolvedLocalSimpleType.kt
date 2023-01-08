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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleListType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleRestrictionType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_SimpleUnionType
import nl.adaptivity.xmlutil.QName

class ResolvedLocalSimpleType(
    override val rawPart: T_LocalSimpleType,
    override val schema: ResolvedSchemaLike
) : ResolvedLocalType, ResolvedSimpleType, T_LocalSimpleType {
    override val name: Nothing? get() = null

    override val annotations: List<XSAnnotation>
        get() = rawPart.annotations

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val simpleDerivation: ResolvedSimpleDerivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is T_SimpleUnionType -> ResolvedSimpleUnionDerivation(raw, schema)
            is T_SimpleListType -> ResolvedSimpleListDerivation(raw, schema)
            is T_SimpleRestrictionType -> ResolvedSimpleRestrictionDerivation(raw, schema)
            else -> error("Derivations must be union, list or restriction")
        }
}
