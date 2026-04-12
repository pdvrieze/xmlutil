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

package io.github.pdvrieze.xml.schematypes.types

import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic

interface AnySimpleUnion<out T : XsdAnySimple> : AnySimpleType.AtomicOrUnion<T> {
    fun isPureUnion(): Boolean

    val members: Collection<AnySimpleType<T>>

    /**
     * Implements a base check. If there are no members then use the parent implementation
     * that does not handle unions. This allows union member types to be used as base types
     * without them having to override this method while they can still inherit the union base.
     *
     * @see NumericType
     */
    override fun isBaseOf(maybeSubType: AnyType): Boolean = when (members.size) {
        0 -> super.isBaseOf(maybeSubType)
        else -> NumericType.Instance.members.any { it.isBaseOf(maybeSubType) }
    }

    override fun canCastFrom(sourceType: AnySimpleType<*>): Boolean {
        if (members.any { it.canCastFrom(sourceType) }) return true
        return super.canCastFrom(sourceType)
    }

    override fun castFrom(other: XsdAtomic): T {
        var result: Result<T>? = null
        for (m in members) {
            if (m is AnySimpleType.AtomicOrUnion<*>) {
                @Suppress("UNCHECKED_CAST")
                val r = runCatching { m.castFrom(other) as T }
                if (r.isSuccess) return r.getOrThrow()
                result = r
            }
        }
        if (result == null) throw IllegalStateException("Union is empty or only contains list elements")
        return result.getOrThrow() // should always throw
    }
}
