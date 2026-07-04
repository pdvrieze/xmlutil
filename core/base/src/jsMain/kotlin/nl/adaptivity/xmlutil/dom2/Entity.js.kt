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

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformEntity

public actual interface Entity : Node, PlatformEntity {
    public actual fun getPublicId(): String?
    public actual fun getSystemId(): String?
}

internal fun addEntityPropertiesToPrototype(prototype: dynamic, inherit: Boolean = true) {
    if (inherit) addNodePropertiesToPrototype(prototype)
    val props = js("{}")
    props.publicId = jsProperty<Entity> { getPublicId() }
    props.systemId = jsProperty<Entity> { getSystemId()}
/*
    props.notationName = jsProperty<Entity> { getNotationName() }
    props.inputEncoding = jsProperty<Entity> { getInputEncoding() }
    props.xmlEncoding = jsProperty<Entity> { getXmlEncoding() }
    props.xmlVersion = jsProperty<Entity> { getXmlVersion() }
*/
    js("Object").defineProperties(prototype, props)
}
