/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

import java.util.Properties
import java.io.FileInputStream

plugins {
    `kotlin-dsl`
}

run {
    val properties = Properties()
    FileInputStream(file("../gradle.properties")).use { input ->
        properties.load(input)
    }
    for(key in properties.stringPropertyNames()) {
        ext[key]=properties[key]
    }
}

val bintrayVersion: String by project

dependencies {
    implementation( "com.jfrog.bintray.gradle:gradle-bintray-plugin:$bintrayVersion")
}

repositories {
    mavenLocal()
    jcenter()
}
