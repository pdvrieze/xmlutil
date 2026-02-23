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

interface Qt3CodeAttr {
    /**
     *  denotes an attribute which in most cases provides a NCName to be used as an error code
     *  in the scope of a error element. The attribute gives the local name of the
     *  error code; the code is assumed to be in the standard error namespace.</p>
     *  The value "*" indicates that any error code is allowed.</p>
     *  The value may also be an EQName (Q{uri}local) to allow for user-defined error codes </p>
     */
    val code: String? // Replace with EQName or NCName
}

