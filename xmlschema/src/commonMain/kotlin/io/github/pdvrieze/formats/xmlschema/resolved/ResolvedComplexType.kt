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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSComplexType
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexType
import io.github.pdvrieze.formats.xmlschema.types.XSI_Annotated

sealed class ResolvedComplexType(
    final override val schema: ResolvedSchemaLike
) : ResolvedType, T_ComplexType, XSI_Annotated, ResolvedLocalAttribute.Parent {
    abstract override val rawPart: XSComplexType

    abstract override val content: ResolvedComplexTypeContent

    val contentType: ResolvedType? by lazy {
        when (val c = content) {
            is ResolvedSimpleContent -> {

                val derivation = c.derivation
                val baseType = derivation.baseType
                when {
                    baseType is ResolvedSimpleType  -> {
                        check(derivation is ResolvedSimpleContentExtension)
                        baseType
                    }
                    baseType !is ResolvedComplexType -> error("Unexpected type: ${baseType}")
                    derivation is ResolvedSimpleContentExtension -> baseType
                    baseType.contentType is ResolvedSimpleType -> {
                        check(derivation is ResolvedSimpleContentRestriction)
                        val derivedType = baseType.contentType
                        if (derivedType is ResolvedSimpleType) {
                            derivation.simpleType ?: baseType.contentType
                        } else if (derivedType is ResolvedComplexType && derivedType.mixed == true /*&& derivedType.isEmptiable*/) {
                            null
                        }

                    }
                }


            }
            is ResolvedComplexContent -> {
                TODO()
            }
            is ResolvedComplexShorthandContent -> {
                TODO()
            }
        }

        TODO()
    }


}
