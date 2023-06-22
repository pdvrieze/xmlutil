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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_FormChoice
import io.github.pdvrieze.formats.xmlschema.types.T_LocalElement
import io.github.pdvrieze.formats.xmlschema.types.T_Scope
import nl.adaptivity.xmlutil.QName

class ResolvedLocalElement(
    val parent: ResolvedComplexType,
    override val rawPart: XSLocalElement,
    schema: ResolvedSchemaLike
) : ResolvedElement(schema), ResolvedParticle, T_LocalElement {
    override val scope: T_Scope get() = T_Scope.LOCAL

    override val ref: QName? get() = rawPart.ref

    val refererenced: ResolvedElement by lazy {
        ref?.let { schema.element(it) } ?: this
    }

    override val minOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger(1)

    override val maxOccurs: T_AllNNI
        get() = rawPart.maxOccurs ?: T_AllNNI(1)

    override val form: T_FormChoice?
        get() = rawPart.form

    override val targetNamespace: VAnyURI?
        get() = rawPart.targetNamespace ?: schema.targetNamespace


    override val keyrefs: List<ResolvedKeyRef> = DelegateList(rawPart.keyrefs) { ResolvedKeyRef(it, schema, this) }
    override val uniques: List<ResolvedUnique> = DelegateList(rawPart.uniques) { ResolvedUnique(it, schema, this) }
    override val keys: List<ResolvedKey> = DelegateList(rawPart.keys) { ResolvedKey(it, schema, this) }

    override fun check() {
        super<ResolvedElement>.check()
        if (rawPart.ref!= null) {
            refererenced// Don't check as that would already be done at top level
        }
        keyrefs.forEach { it.check() }
        uniques.forEach { it.check() }
        keys.forEach { it.check() }
    }

}
