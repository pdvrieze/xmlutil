/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml.serialization.canary

import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerialSaver

object Canary {

    private val saverMap = mutableMapOf<KSerialSaver<*>, ExtInfo>()


    fun <T> extInfo(saver: KSerialSaver<T>, obj: T): ExtInfo {
        return saverMap.getOrPut(saver) {
            val output = CanaryOutput()
            saver.save(output, obj)
            output.extInfo()
        }
    }


}