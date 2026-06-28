/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved.flattened

import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.AllNNIRange
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI

object FlattenedEmptyGroup : FlattenedSequence(VAllNNI.ZERO..VAllNNI.ZERO, emptyList()) {
    override fun toString(): String = "()"

    override fun effectiveTotalRange(): AllNNIRange = range
    override fun single(): FlattenedSequence = this

    context(checkHelper: CheckHelper, siblingContext: SiblingContextProvider)
    override fun restricts(reference: FlattenedParticle): Boolean {
        return reference.isEmptiable
    }

    override fun plus(other: FlattenedParticle): FlattenedParticle {
        return other // empty is never anything
    }
}
