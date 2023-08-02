/*
 * Copyright (c) 2022.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalComplexType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.toDerivationSet
import nl.adaptivity.xmlutil.QName

private fun VNCName.toQName(schema: XSSchema): QName {
    return toQname(schema.targetNamespace)
}

fun XSAnnotation?.models(): ResolvedAnnotation? = when (this){
    null -> null
    else -> ResolvedAnnotation(this)
}

internal fun calcProhibitedSubstitutions(
    rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike
): Set<VDerivationControl.Complex> {
    return rawPart.block ?: schema.blockDefault.toDerivationSet()
}

internal fun calcFinalSubstitutions(
    rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike
): Set<VDerivationControl.Complex> {
    return rawPart.final ?: schema.finalDefault.toDerivationSet()
}

