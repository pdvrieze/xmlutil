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

package io.github.pdvrieze.formats.xmlschema.resolved.facets

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.*
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSimpleType

sealed class ResolvedLengthBase(schema: ResolvedSchemaLike) : ResolvedFacet(schema) {
    abstract val value: ULong
    override fun check(type: ResolvedSimpleType) {
        when (type.mdlVariety) {
            ResolvedSimpleType.Variety.ATOMIC -> when (val primitive = type.mdlPrimitiveTypeDefinition) {
                null -> error("Length is not supported on simple types not deriving from a primitive")
                else -> error("Length is not supported for type ${primitive.qName}")
            }

            ResolvedSimpleType.Variety.LIST -> {} // fine
            ResolvedSimpleType.Variety.UNION,
            ResolvedSimpleType.Variety.NIL -> error("Variety does not support length facet")
        }
    }

    fun validate(type: ResolvedSimpleType, representation: String): Result<Unit> = runCatching {
        when (type.mdlVariety) {
            ResolvedSimpleType.Variety.ATOMIC -> {
                when (val primitive = type.mdlPrimitiveTypeDefinition) {
                    is AnyURIType,
                    is StringType -> checkLength(representation.length, representation)

                    is HexBinaryType -> checkLength(HexBinaryType.length(representation), "hex value")
                    is Base64BinaryType -> checkLength(Base64BinaryType.length(representation), "base64 value")
                    is QNameType,
                    is NotationType -> Unit

                    else -> error("Unsupported primitive type ${primitive?.qName} for simple type restriction")
                }
            }

            ResolvedSimpleType.Variety.LIST -> {
                val len = representation.split(' ').size
                checkLength(len, "list[$len]")
            }

            ResolvedSimpleType.Variety.UNION,
            ResolvedSimpleType.Variety.NIL -> error("Length Facet not supported in this variety")
        }
    }


    abstract fun checkLength(resolvedLength: Int, repr: String)
}
