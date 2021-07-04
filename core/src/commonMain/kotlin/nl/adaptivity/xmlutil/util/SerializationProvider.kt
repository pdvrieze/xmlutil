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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import kotlin.reflect.KClass

public interface SerializationProvider {
    public interface XmlSerializerFun<in T : Any> {
        public operator fun invoke(output: XmlWriter, value: T)
    }

    public interface XmlDeserializerFun {
        public operator fun <T : Any> invoke(input: XmlReader, type: KClass<T>): T
    }

    public fun <T : Any> serializer(type: KClass<T>): XmlSerializerFun<T>?
    public fun <T : Any> deSerializer(type: KClass<T>): XmlDeserializerFun?
}
