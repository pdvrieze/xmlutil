/*
 * Copyright (c) 2020.
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
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.dokka.gradle.GradleDokkaSourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


fun GradleDokkaSourceSet.sourceRoots(project: Project, vararg names: String) {
    val kotlin = project.extensions.getByName<KotlinMultiplatformExtension>("kotlin")
    names.asSequence()
        .map { kotlin.sourceSets.getByName(it) as KotlinSourceSet }
        .flatMap { it.kotlin.srcDirs.asSequence() }
        .forEach {
            if (project.buildDir.absolutePath !in it.absolutePath) {
                sourceRoot {
                    path = it.path
                }
            }
        }
}
