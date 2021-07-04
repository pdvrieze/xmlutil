/*
 * Copyright (c) 2019.
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

package nl.adaptivity.xmlutil

/**
 *
 * Simple delegating writer that passes all calls on to the delegate. This class is abstract for the only reason that any
 * direct instances of this class make little sense.
 *
 *
 * Created by pdvrieze on 17/11/15.
 *
 */
public abstract class XmlDelegatingWriter(protected val delegate: XmlWriter) : XmlWriter by delegate
