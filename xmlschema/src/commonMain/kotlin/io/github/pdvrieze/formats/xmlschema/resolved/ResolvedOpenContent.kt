/*
 * Copyright (c) 2024.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDefaultOpenContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSOpenContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSOpenContentBase
import io.github.pdvrieze.formats.xmlschema.types.VContentMode

class ResolvedDefaultOpenContent(rawPart: XSDefaultOpenContent, schema: ResolvedSchemaLike) :
    ResolvedOpenContent(rawPart, schema, false) {

    val appliesToEmpty: Boolean = rawPart.appliesToEmpty
}

open class ResolvedOpenContent(
    val mdlWildCard: ResolvedAny?,
    val mdlMode: Mode,
) {

    constructor(rawPart: XSOpenContentBase, schema: ResolvedSchemaLike, localInContext: Boolean) : this(
        rawPart.any?.let { ResolvedAny(it, schema) },
        when (rawPart.mode) {
            VContentMode.INTERLEAVE -> Mode.INTERLEAVE
            VContentMode.SUFFIX -> Mode.SUFFIX
            VContentMode.NONE -> Mode.NONE
        }
    ) {
        when (mdlMode) {
            Mode.NONE -> require(mdlWildCard == null)
            else -> requireNotNull(mdlWildCard)
        }
    }

    fun restricts(other: ResolvedOpenContent?, schemaVersion: SchemaVersion, derivedContentEmpty: Boolean): Boolean {
        if (other == null) return false
        return (derivedContentEmpty || other.mdlMode.extends(mdlMode)) && when (val w = mdlWildCard) {
            null -> other.mdlWildCard == null
            else -> when(val owc = other.mdlWildCard){
                null -> true
                else -> w.isSubsetOf(owc, schemaVersion)
            }
        }
    }


    enum class Mode { INTERLEAVE, SUFFIX, NONE;

        fun extends(base: Mode): Boolean {
            return this == INTERLEAVE || (this == SUFFIX && base == SUFFIX)
        }
    }
}
