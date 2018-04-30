import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

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

plugins {
    base
    id("kotlin-platform-js")
}

base {
    archivesBaseName="xmlutil-core"
}

dependencies {
    expectedBy(project(":xmlutil:core:common"))
    implementation(project(":multiplatform:js"))
    implementation(kotlin("stdlib-js"))
}

getTasks().withType<Kotlin2JsCompile> {
    kotlinOptions {
        sourceMap = true
        suppressWarnings = false
        verbose = true
        metaInfo = true
        moduleKind = "umd"
        main = "call"
    }
}


//compileKotlin2Js {
//    dependsOn tasks.copyCore
//        kotlinOptions.outputFile = outDir + "xmlutil.js"
//    kotlinOptions.sourceMap = true
//    kotlinOptions.suppressWarnings = false
//    kotlinOptions.verbose = true
//    kotlinOptions.metaInfo = true
//    kotlinOptions.moduleKind = "umd"
//    kotlinOptions.main = "call"
//}
//
//jar {
//    baseName='xmlutil-js'
//}
