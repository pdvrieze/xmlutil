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

package io.github.pdvrieze.formats.xpath.impl

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only valid after XPath 2+ check")
internal annotation class XPath2

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only valid after XPath 3+ check")
internal annotation class XPath3_0

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only valid after XPath 3.1+ check")
internal annotation class XPath3_1

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only valid after XQuery check")
internal annotation class XQuery

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only valid after XQuery 3.1+ check")
internal annotation class XQuery3_1
