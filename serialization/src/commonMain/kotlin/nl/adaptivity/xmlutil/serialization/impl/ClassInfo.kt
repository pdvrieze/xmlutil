/*
 * Copyright (c) 2020.
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

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.serialutil.impl.name
import kotlin.reflect.KClass

sealed class ClassInfo {

    abstract val klassName: String

    class ByType<T: Any>(val kClass: KClass<T>): ClassInfo() {

        override val klassName: String
            get() = kClass.name

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByType<*>) return false

            if (kClass != other.kClass) return false

            return true
        }

        override fun hashCode(): Int {
            return kClass.hashCode()
        }
    }

    class ByName(override val klassName: String): ClassInfo() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByName) return false

            if (klassName != other.klassName) return false

            return true
        }

        override fun hashCode(): Int {
            return klassName.hashCode()
        }
    }

    companion object {
        operator fun invoke(klassName: String) = ByName(klassName)
        operator fun <T: Any> invoke(klass: KClass<T>) = ByType(klass)
    }
}