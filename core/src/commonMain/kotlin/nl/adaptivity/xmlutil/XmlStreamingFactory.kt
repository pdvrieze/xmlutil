/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil

public expect interface XmlStreamingFactory

/** Flag to indicate that the xml declaration should be omitted, when possible.  */
public const val FLAG_OMIT_XMLDECL: Int = 1

/** Flag to indicate that the namespace usable should be automatically repaired. */
public const val FLAG_REPAIR_NS: Int = 2

/** The default used flags */
public const val DEFAULT_FLAGS: Int = FLAG_OMIT_XMLDECL
