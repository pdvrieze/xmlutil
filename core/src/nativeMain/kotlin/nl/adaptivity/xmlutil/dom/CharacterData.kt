/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.dom

public actual interface CharacterData : Node {
    public var data: String

    public actual fun substringData(offset: Int, count: Int): String

    public actual fun appendData(data: String)

    public actual fun insertData(offset: Int, data: String)

    public actual fun deleteData(offset: Int, count: Int)

    public actual fun replaceData(offset: Int, count: Int, data: String)
}

public actual inline fun CharacterData.getData(): String = data
public actual inline fun CharacterData.setData(value: String) { data = value }
