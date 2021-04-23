/*
 * Copyright (c) 2021.
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

package net.devrieze.gradle.ext

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder

fun Project.configureDokka(myModuleName: String = name, myModuleVersion: String? = property("xmlutil_version") as String?) {
    tasks.withType<DokkaTask> {
        moduleName.set(myModuleName)
        myModuleVersion.let { moduleVersion.set(it) }
        dokkaSourceSets.configureEach {
            this@configureDokka.configureDokkaSourceSet(this)
        }
    }
    tasks.withType<DokkaTaskPartial> {
        moduleName.set(myModuleName)
        myModuleVersion.let { moduleVersion.set(it) }
        dokkaSourceSets.configureEach {
            this@configureDokka.configureDokkaSourceSet(this)
        }
    }
}

private fun Project.configureDokkaSourceSet(
    sourceSet: GradleDokkaSourceSetBuilder
                                           ) = with(sourceSet){
    logger.lifecycle("Configuring dokka on sourceSet: $name = ${displayName.orNull}")
    if (name.startsWith("android")) {
        noAndroidSdkLink.set(false)
        noJdkLink.set(true)
    } else {
        noAndroidSdkLink.set(true)
        noJdkLink.set(false)
    }
    displayName.set(when(val dn = displayName.get()){
        "jvm" -> "JVM"
        "android" -> "Android"
        "common" -> "Common"
        "js" -> "JS"
        else -> dn
    })
    includeNonPublic.set(false)
    skipEmptyPackages.set(true)
    skipDeprecated.set(true)
    perPackageOption {
        matchingRegex.set(".*\\.(impl|internal)(|\\..*)")
        suppress.set(true)
    }
    if ("Main" in displayName.get())  {
        val readme = project.file(project.relativePath("src/README.md"))
        if (readme.exists() && readme.canRead()) {
            includes.from(listOf(readme))
            logger.lifecycle("Adding ${readme} to sourceSet ${project.name}:${name}(${displayName.orNull})")
        } else {
            logger.warn("Missing ${readme} for project ${project.name}")
        }
    }
}