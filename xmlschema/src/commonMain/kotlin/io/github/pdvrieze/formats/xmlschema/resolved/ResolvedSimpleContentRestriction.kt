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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleContentRestriction

/**
 * Restriction is used for simple types.
 */
class ResolvedSimpleContentRestriction(
    context: ResolvedComplexType,
    override val rawPart: XSSimpleContentRestriction,
    schema: ResolvedSchemaLike,
    inheritedTypes: SingleLinkedList<ResolvedType>,
) : ResolvedSimpleContentDerivation(rawPart, schema) {

    init {
        if (rawPart.base!=null) {
            require(rawPart.simpleType==null) { "Restriction cannot specify both base and simpleType" }
        } else {
            requireNotNull(rawPart.simpleType) { "Restriction must specify either a base or contain a simpleType child" }
        }
    }


    override val model: Model by lazy { Model(rawPart, schema, context, inheritedTypes) }

    class Model(
        rawPart: XSSimpleContentRestriction,
        schema: ResolvedSchemaLike,
        context: ResolvedComplexType,
        inheritedTypes: SingleLinkedList<ResolvedType>
    ) : ResolvedSimpleContentDerivation.Model(rawPart, schema) {
        override val baseType: ResolvedType = rawPart.base?.let { schema.type(it, inheritedTypes) }
            ?: ResolvedLocalSimpleType(requireNotNull(rawPart.simpleType), schema, context, inheritedTypes)
    }

}
