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

package nl.adaptivity.xmlutil.core.impl.multiplatform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import platform.posix.size_t

/**
 * Wrapper type to stand in for `size_t` as that type has inconsistent sizes in different architectures.
 */
@OptIn(UnsafeNumber::class)
public value class SizeT(public val value: size_t) {
//    public constructor(value: UInt) : this(value.toULong())
    public operator fun minus(other: SizeT): SizeT = SizeT(value - other.value)
    public operator fun plus(other: SizeT): SizeT = SizeT(value + other.value)
    public fun toInt(): Int = value.toInt()
    public fun toULong(): ULong = value.toULong()
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
public fun sizeT(l: Long): SizeT {
    return SizeT(l.convert<size_t>())
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
public fun sizeT(i: Int): SizeT {
    return SizeT(i.convert<size_t>())
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
public fun sizeT(l: ULong): SizeT {
    return SizeT(l.convert<size_t>())
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
public fun sizeT(i: UInt): SizeT {
    return SizeT(i.convert<size_t>())
}
