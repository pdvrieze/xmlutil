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

package org.w3.qt3tests.attrGroups

interface Qt3RoleAttr {
    /**
     * Describes how a source document is made available to the query.
     *
     * The value "." indicates that the source document will be the context
     * item for the query.
     *
     * A value in the form `$varname` indicates that the source document will
     * be made available as the value of the external variable `$varname`. This variable
     * will *not* be declared in the query (this is to allow the mechanism
     * to be used in XPath). The query will always be such that it is possible to
     * add <code>declare variable</code> declarations at the start before compiling the query.</p>
     *
     * If the source document is to be made available to the query using the
     * `doc()` function, the "source" element should have a "uri" attribute, and
     * the "role" attribute should be absent.
     *
     * The "role" attribute should be omitted if the source is part of a collection
     * definition.
     */
    val role: String?
}

