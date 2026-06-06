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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.dom2.EmptyNamedNodeMap
import nl.adaptivity.xmlutil.dom2.addAttrPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addCharacterDataPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addDocumentPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addDocumentTypePropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addElementPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addNamedNodeMapPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addNodeListPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addNodePropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addProcessingInstructionPropertiesToPrototype
import nl.adaptivity.xmlutil.dom2.addTextPropertiesToPrototype

internal actual fun platformAbstractDomInit() {
    addNodePropertiesToPrototype(AbstractNode::class.js.asDynamic().prototype)
    addAttrPropertiesToPrototype(AbstractAttr::class.js.asDynamic().prototype, false)
    addCharacterDataPropertiesToPrototype(AbstractCharacterData::class.js.asDynamic().prototype, false)
    addDocumentPropertiesToPrototype(AbstractDocument::class.js.asDynamic().prototype, false)
    addDocumentTypePropertiesToPrototype(AbstractDocumentType::class.js.asDynamic().prototype, false)
    addElementPropertiesToPrototype(AbstractElement::class.js.asDynamic().prototype, false)

    addNamedNodeMapPropertiesToPrototype(AbstractAttrStorage::class.js.asDynamic().prototype)
    addNamedNodeMapPropertiesToPrototype(EmptyNamedNodeMap::class.js.asDynamic().prototype)

    addNodeListPropertiesToPrototype(EmptyNodeList::class.js.asDynamic().prototype)
    addNodeListPropertiesToPrototype(LinearNodeStorage::class.js.asDynamic().prototype)
    addNodeListPropertiesToPrototype(LinkedNodeList::class.js.asDynamic().prototype)
    addNodeListPropertiesToPrototype(NodeListImpl::class.js.asDynamic().prototype)

    addProcessingInstructionPropertiesToPrototype(AbstractProcessingInstruction::class.js.asDynamic().prototype, false)
    addTextPropertiesToPrototype(AbstractText::class.js.asDynamic().prototype, false)
}
