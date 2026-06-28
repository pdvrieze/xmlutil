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

import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import kotlin.jvm.JvmName

sealed class RemovalResult {
    object NoMatch: RemovalResult() {
        override val isEmptiable: Boolean get() = false
    }

    object FullMatch: RemovalResult() {
        override val isEmptiable: Boolean get() = true
    }

    class PrefixMatch(val suffix: FlattenedParticle) : RemovalResult() {
        override val isEmptiable: Boolean get() = suffix.isEmptiable
    }

    abstract val isEmptiable: Boolean

    inline fun map(action: (FlattenedParticle) -> FlattenedParticle?): RemovalResult = when(val self = this) {
        is PrefixMatch -> RemovalResult(action(self.suffix))
        else -> self
    }

    companion object {
        operator fun invoke (suffix: FlattenedParticle): RemovalResult = when {
            suffix.maxOccurs == VAllNNI.ZERO -> FullMatch
            else -> PrefixMatch(suffix)
        }

        @JvmName("ofNullable")
        operator fun invoke(maybeSuffix: FlattenedParticle?): RemovalResult = when  {
            maybeSuffix == null -> NoMatch
            maybeSuffix.maxOccurs == VAllNNI.ZERO -> FullMatch
            else -> PrefixMatch(maybeSuffix)
        }

    }
}
