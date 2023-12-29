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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAny
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyBase
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSI_Annotated
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlin.jvm.JvmStatic

abstract class ResolvedWildcardBase<E : VQNameListBase.IElem> internal constructor(
    val mdlNamespaceConstraint: VNamespaceConstraint<E>,
    val mdlProcessContents: VProcessContents,
    final override val model: ResolvedAnnotated.IModel
) : ResolvedAnnotated {

    constructor(
        mdlNamespaceConstraint: VNamespaceConstraint<E>,
        mdlProcessContents: VProcessContents,
    ) : this (mdlNamespaceConstraint, mdlProcessContents, ResolvedAnnotated.Empty)

    constructor(
        rawPart: XSI_Annotated,
        mdlNamespaceConstraint: VNamespaceConstraint<E>,
        mdlProcessContents: VProcessContents
    ) : this(
        mdlNamespaceConstraint,
        mdlProcessContents,
        ResolvedAnnotated.Model(rawPart),
    )

    abstract val mdlNotQName: VQNameListBase<E>



    override fun toString(): String {
        return buildString {
            append("wildcard(")
            append(mdlProcessContents).append(", ")
            append(mdlNamespaceConstraint)
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ResolvedWildcardBase<*>

        if (mdlNamespaceConstraint != other.mdlNamespaceConstraint) return false
        if (mdlProcessContents != other.mdlProcessContents) return false
        if (mdlNotQName != other.mdlNotQName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mdlNamespaceConstraint.hashCode()
        result = 31 * result + mdlProcessContents.hashCode()
        result = 31 * result + mdlNotQName.hashCode()
        return result
    }

    companion object {

        @JvmStatic
        protected fun XSAny.toConstraint(schemaLike: ResolvedSchemaLike): VNamespaceConstraint<VQNameListBase.Elem> {
            val p = toConstraintHelper(schemaLike)
            notQName?.check(schemaLike.version)
            return VNamespaceConstraint(p.first, p.second, notQName ?: VQNameList())
        }

        @JvmStatic
        protected fun XSAnyAttribute.toConstraint(schemaLike: ResolvedSchemaLike): VNamespaceConstraint<VQNameListBase.AttrElem> {

            val (variety, nsSet) = toConstraintHelper(schemaLike)

            notQName?.check(schemaLike.version)
            return VNamespaceConstraint(variety, nsSet, notQName ?: VAttrQNameList())
        }

        private fun XSAnyBase.toConstraintHelper(schema: ResolvedSchemaLike): Pair<VNamespaceConstraint.Variety, Set<VAnyURI>> {
            val ns = namespace
            val notNs = notNamespace

            require(ns == null || notNs == null) { "A wildcard cannot specify both presence and absence ofa a namespace" }
            val variety: VNamespaceConstraint.Variety
            val namespaces: Set<VAnyURI>
            when (ns) {
                VNamespaceList.ANY -> {
                    variety = VNamespaceConstraint.Variety.ANY
                    namespaces = emptySet()
                }

                VNamespaceList.OTHER -> {
                    variety = VNamespaceConstraint.Variety.NOT
                    namespaces = buildSet {
                        // only add local when relevant to the context (there is something in the context
                        // This should handle "other" being special

                        // in version 1.0 it doesn't include the "absent" namespace, but in 1.1 it does
                        if (schema.version != SchemaVersion.V1_0 || schema.targetNamespace == null) {
                            add(VAnyURI.EMPTY)
                        }
                        schema.targetNamespace?.let { add(it) }
                    }
                }

                is VNamespaceList.Values -> {
                    variety = VNamespaceConstraint.Variety.ENUMERATION
                    namespaces = ns.values.mapTo(mutableSetOf()) { it.toUri(schema) }
                }

                null -> when (notNs) {
                    null -> {
                        variety = VNamespaceConstraint.Variety.ANY
                        namespaces = emptySet()
                    }

                    else -> {
                        variety = VNamespaceConstraint.Variety.NOT
                        namespaces = notNs.mapNotNullTo(mutableSetOf()) { it.toUri(schema) }
                    }
                }

            }

            val p = Pair(variety, namespaces)
            return p
        }

        private fun VNamespaceList.Elem.toUri(schemaLike: ResolvedSchemaLike): VAnyURI = when (this) {
            VNamespaceList.LOCAL -> "".toAnyUri()
            VNamespaceList.TARGETNAMESPACE -> schemaLike.targetNamespace ?: "".toAnyUri()
            is VNamespaceList.Uri -> value
        }

        private fun VNotNamespaceList.Elem.toUri(schemaLike: ResolvedSchemaLike): VAnyURI? = when (this) {
            VNotNamespaceList.LOCAL -> "".toAnyUri()
            VNotNamespaceList.TARGETNAMESPACE -> schemaLike.targetNamespace ?: "".toAnyUri()
            is VNotNamespaceList.Uri -> value
        }

    }

}

