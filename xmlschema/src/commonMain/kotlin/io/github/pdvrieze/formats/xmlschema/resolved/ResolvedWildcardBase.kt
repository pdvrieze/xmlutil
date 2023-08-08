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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAny
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyBase
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSI_Annotated
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import kotlin.jvm.JvmStatic

abstract class ResolvedWildcardBase<E : VQNameListBase.IElem> : ResolvedAnnotated {
    fun matches(name: QName): Boolean {
        return mdlNamespaceConstraint.matches(name)
    }

    val mdlNamespaceConstraint: VNamespaceConstraint<E>
    val mdlProcessContents: VProcessContents

    final override val model: ResolvedAnnotated.IModel

    constructor(
        mdlNamespaceConstraint: VNamespaceConstraint<E>,
        mdlProcessContents: VProcessContents
    ) {
        this.mdlNamespaceConstraint = mdlNamespaceConstraint
        this.mdlProcessContents = mdlProcessContents
        this.model = ResolvedAnnotated.Empty
    }

    constructor(
        rawPart: XSI_Annotated,
        mdlNamespaceConstraint: VNamespaceConstraint<E>,
        mdlProcessContents: VProcessContents
    ) {
        this.mdlNamespaceConstraint = mdlNamespaceConstraint
        this.mdlProcessContents = mdlProcessContents
        this.model = ResolvedAnnotated.Model(rawPart)
    }

    companion object {

        @JvmStatic
        protected fun XSAny.toConstraint(schemaLike: ResolvedSchemaLike): VNamespaceConstraint<VQNameListBase.Elem> {
            val p = toConstraintHelper(schemaLike)

            return VNamespaceConstraint(p.first, p.second, notQName ?: VQNameList())
        }

        @JvmStatic
        protected fun XSAnyAttribute.toConstraint(schemaLike: ResolvedSchemaLike): VNamespaceConstraint<VQNameListBase.AttrElem> {
            val p = toConstraintHelper(schemaLike)

            return VNamespaceConstraint(p.first, p.second, notQName ?: VAttrQNameList())
        }

        private fun XSAnyBase.toConstraintHelper(schemaLike: ResolvedSchemaLike): Pair<VNamespaceConstraint.Variety, Set<VAnyURI>> {
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
                        add(VAnyURI(""))
                        schemaLike.targetNamespace?.let { add(it) }
                    }
                }

                is VNamespaceList.Values -> {
                    variety = VNamespaceConstraint.Variety.ENUMERATION
                    namespaces = ns.values.mapTo(mutableSetOf()) { it.toUri(schemaLike) }
                }

                null -> when (notNs) {
                    null -> {
                        variety = VNamespaceConstraint.Variety.ANY
                        namespaces = emptySet()
                    }

                    else -> {
                        variety = VNamespaceConstraint.Variety.NOT
                        namespaces = notNs.mapNotNullTo(mutableSetOf()) { it.toUri(schemaLike) }
                    }
                }

            }

            val p = Pair(variety, namespaces)
            return p
        }

        private fun VNamespaceList.Elem.toUri(schemaLike: ResolvedSchemaLike): VAnyURI = when (this) {
            VNamespaceList.LOCAL -> VAnyURI("")
            VNamespaceList.TARGETNAMESPACE -> schemaLike.targetNamespace ?: VAnyURI("")
            is VNamespaceList.Uri -> value
        }

        private fun VNotNamespaceList.Elem.toUri(schemaLike: ResolvedSchemaLike): VAnyURI? = when (this) {
            VNotNamespaceList.LOCAL -> VAnyURI("")
            VNotNamespaceList.TARGETNAMESPACE -> schemaLike.targetNamespace ?: VAnyURI("")
            is VNotNamespaceList.Uri -> value
        }

    }

}

