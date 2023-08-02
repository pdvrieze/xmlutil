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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.TypeModel

sealed interface ResolvedLocalType : ResolvedType {
    override val rawPart: XSLocalType

    val mdlContext: ResolvedTypeContext

    companion object {
        @Deprecated("This is unsafe")
        operator fun invoke(rawPart: XSLocalType, schema: ResolvedSchemaLike, context: ResolvedTypeContext): ResolvedLocalType {
            return when (rawPart) {
                is XSLocalComplexTypeComplex -> ResolvedLocalComplexType(rawPart, schema, context as ResolvedComplexTypeContext)
                is XSLocalComplexTypeShorthand -> ResolvedLocalComplexType(rawPart, schema, context as ResolvedComplexTypeContext)
                is XSLocalComplexTypeSimple -> ResolvedLocalComplexType(rawPart, schema, context as ResolvedComplexTypeContext)
                is XSLocalSimpleType -> ResolvedLocalSimpleType(rawPart, schema, context as ResolvedSimpleTypeContext)
            }
        }

        operator fun invoke(rawPart: XSLocalSimpleType, schema: ResolvedSchemaLike, context: ResolvedSimpleTypeContext): ResolvedLocalType {
            return ResolvedLocalSimpleType(rawPart, schema, context)
        }

        operator fun invoke(rawPart: XSLocalComplexType, schema: ResolvedSchemaLike, context: ResolvedComplexTypeContext): ResolvedLocalType {
            return when (rawPart) {
                is XSLocalComplexTypeComplex -> ResolvedLocalComplexType(rawPart, schema, context)
                is XSLocalComplexTypeShorthand -> ResolvedLocalComplexType(rawPart, schema, context)
                is XSLocalComplexTypeSimple -> ResolvedLocalComplexType(rawPart, schema, context)
            }
        }
    }
}

