/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xmlutil.serialization.structure

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@ExperimentalXmlUtilApi
public enum class DocumentPreserveSpace {
    /** The default preserve for the type */
    DEFAULT {
        override fun withDefault(d: Boolean): Boolean = d
    },
    /** Preserve whitespace, specifie as type default, also in children (where relevant) */
    DEFAULT_PRESERVE {
        override fun withDefault(d: Boolean): Boolean = true
    },
    /** Preserve whitespace, also in children (where relevant) */
    DOCUMENT_PRESERVE {
        override fun withDefault(d: Boolean): Boolean = true
        override fun withDefault(t: TypePreserveSpace): DocumentPreserveSpace = this
    },
    /** Forcibly ignore the whitespace */
    DEFAULT_IGNORE {
        override fun withDefault(d: Boolean): Boolean = false
    };

    public abstract fun withDefault(d: Boolean): Boolean

    public open fun withDefault(t: TypePreserveSpace): DocumentPreserveSpace = when (t) {
        TypePreserveSpace.DEFAULT -> this
        TypePreserveSpace.PRESERVE -> DEFAULT_PRESERVE
        TypePreserveSpace.IGNORE -> DEFAULT_IGNORE
    }
}
