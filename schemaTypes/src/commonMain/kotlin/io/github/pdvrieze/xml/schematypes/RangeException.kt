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

@file:OptIn(ExperimentalContracts::class)

package io.github.pdvrieze.xml.schematypes

import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Subtype of NumberFormatException that indicates that the value is out of range for the type.
 */
class RangeException: NumberFormatException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

@XmlUtilInternal
inline fun requireRange(value: Boolean, onError: () -> String) {
    contract {
        returns() implies value
    }

    if (!value) throw RangeException(onError())
}

@Suppress("NOTHING_TO_INLINE")
@XmlUtilInternal
inline fun requireRange(value: Boolean) {
    contract {
        returns() implies value
    }

    if (!value) throw RangeException("Value is out of range")
}
