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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.StringType
import io.github.pdvrieze.xml.schematypes.values.XSQName
import io.github.pdvrieze.xml.schematypes.values.XSString

/**
 * Special string type that captures a namespace
 */
class XSPrefixString(val namespace: String, val prefix: String, val localname: String) : XSString {
    override val xmlString: String
        get() = when {
            prefix.isEmpty() -> localname
            else -> "$prefix:$localname"
        }
    override val schemaType: StringType<*> get() = StringType.Instance

    fun toQName(): XSQName = XSQName(namespace, localname, prefix)

    override fun toString(): String = xmlString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        when (other) {
            is XSPrefixString -> {
                if (namespace != other.namespace) return false
                if (prefix != other.prefix) return false
                if (localname != other.localname) return false

                return true
            }
            is XSString -> {
                return xmlString == other.xmlString
            }
            else -> return false
        }
    }

    override fun hashCode(): Int = xmlString.hashCode()

}
