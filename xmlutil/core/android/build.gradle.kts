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
    id("com.android.library")
    id("kotlin-platform-android")
    id("kotlin-kapt")
    idea
}

val `kotlin_version`: String by rootProject
val kotlinVersion get() = `kotlin_version` as String?

android {
    compileSdkVersion(27)
}

base {
    archivesBaseName="xmlutil-core"
}

version = "0.5.0"
description = "Utility classes for xml handling that works across platforms (jvm/js/android), and more powerful than jaxb"


dependencies {
    expectedBy(project(":xmlutil:core:common"))
    implementation(project(":xmlutil:core:java"))
    implementation(project(":multiplatform:jvm"))
    implementation(kotlin("stdlib-jdk7", kotlinVersion))
}
/*

test {
    useTestNG()
}

jar {
    baseName = "${project.parent.name}-${project.name}"
}

sourceCompatibility = myJavaVersion
targetCompatibility = myJavaVersion
*/

idea {
    module {
        name = "${parent?.name}-${project.name}"
    }
}
