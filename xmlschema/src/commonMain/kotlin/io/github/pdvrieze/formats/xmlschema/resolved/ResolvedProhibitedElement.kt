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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.impl.invariant
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedProhibitedElement(
    rawPart: XSLocalElement,
    schema: ResolvedSchemaLike
) : IResolvedElementUse {
    override val model: ResolvedAnnotated.IModel by lazy { ResolvedAnnotated.Model(rawPart) }

    override val mdlQName: QName = when (val n = rawPart.name) {
        null -> requireNotNull(rawPart.ref)
        else -> n.toQname(
            rawPart.targetNamespace ?: when (rawPart.form ?: schema.elementFormDefault) {
                VFormChoice.QUALIFIED -> schema.targetNamespace
                else -> null
            }
        )
    }

    init {
        invariant(rawPart.minOccurs == VNonNegativeInteger.ZERO)
        invariant(rawPart.maxOccurs == VNonNegativeInteger.ZERO)
        if (rawPart.targetNamespace!=null) {
            requireNotNull(rawPart.name) { "3.3.3(4.1) - If an element specifies a target namespace it must have a name" }
            require(rawPart.form==null) { "3.3.3(4.2) - If an element specifies a target namespace it may nothave a form" }
        } else {
            require(rawPart.name != null || rawPart.ref != null)
        }
    }

    override val mdlMinOccurs: VNonNegativeInteger get() = VNonNegativeInteger.ZERO

    override val mdlMaxOccurs: VAllNNI get() = VAllNNI.ZERO

    override val mdlTerm: Nothing
        get() = throw UnsupportedOperationException("Prohibited elements have no terms")

}
