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

import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI

interface Qt3UriAttr {
    /**
     * This attribute provides a URI to be used as an abstract identifier of a resource within
     * the test suite (for example, a source document, or a module). The URI is designed to
     * be independent of the location of the resource.
     *
     * The URI may be an absolute URI or a relative URI reference. If it is a relative URI reference,
     * it is resolved relative to the base URI of the element in which it appears (in practice, that is, the
     * base URI of the test-set catalog file).
     *
     * For source documents, the URI can be used in a call to the `doc()` function to retrieve
     * this source document (so the actual query does not need to know its location).
     *
     * For modules, the URI defines the module URI and is again independent of location.
     *
     * For schemas, the URI defines the target namespace URI.
     */
    val uri: XsdAnyURI?
}

