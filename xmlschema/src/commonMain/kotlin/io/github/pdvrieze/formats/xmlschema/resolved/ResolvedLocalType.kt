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

sealed interface ResolvedLocalType : ResolvedType {
    override val rawPart: XSLocalType

    override val mdlScope: VTypeScope.Local
    val mdlContext: VTypeScope.MemberBase

    companion object {
        operator fun invoke(
            rawPart: XSLocalType,
            schema: ResolvedSchemaLike,
            scope: VTypeScope.Member
        ): ResolvedLocalType {
            return when (rawPart) {
                is XSLocalComplexTypeComplex -> ResolvedLocalComplexType(
                    rawPart,
                    schema,
                    scope as VComplexTypeScope.Member
                )

                is XSLocalComplexTypeShorthand -> ResolvedLocalComplexType(
                    rawPart,
                    schema,
                    scope as VComplexTypeScope.Member
                )

                is XSLocalComplexTypeSimple -> ResolvedLocalComplexType(
                    rawPart,
                    schema,
                    scope as VComplexTypeScope.Member
                )

                is XSLocalSimpleType -> ResolvedLocalSimpleType(rawPart, schema,
                    scope as VSimpleTypeScope.Member
                )
            }
        }

        operator fun invoke(
            rawPart: XSLocalSimpleType,
            schema: ResolvedSchemaLike,
            scope: VSimpleTypeScope.Member
        ): ResolvedLocalType {
            return ResolvedLocalSimpleType(rawPart, schema, scope)
        }

        operator fun invoke(
            rawPart: XSLocalComplexType,
            schema: ResolvedSchemaLike,
            scope: VComplexTypeScope.Member
        ): ResolvedLocalType {
            return when (rawPart) {
                is XSLocalComplexTypeComplex -> ResolvedLocalComplexType(rawPart, schema, scope)
                is XSLocalComplexTypeShorthand -> ResolvedLocalComplexType(rawPart, schema, scope)
                is XSLocalComplexTypeSimple -> ResolvedLocalComplexType(rawPart, schema, scope)
            }
        }
    }
}

