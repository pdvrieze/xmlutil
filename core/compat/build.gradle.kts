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

import net.devrieze.gradle.ext.doPublish

/*
 * Copyright (c) 2024-2026.
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

plugins {
    id("projectPlugin")
    `maven-publish`
    `java-platform`
    signing
}

config {
    generateJavaModules = false // compatibility BOM that doesn't need a module
    createAndroidCompatComponent = false
}

val emptyJar = tasks.register<Jar>("emptyJar") {
    description = "Empty jar for relocation"
}

publishing {
    publications.create<MavenPublication>("relocateCommon") {
        groupId = "io.github.pdvrieze.xmlutil"
        artifactId = "core-jvmcommon"
        from(components["javaPlatform"])

        pom {
            name = "core-jvmcommon"
            description = "Compatibility relocatoin for the core-jvm module"
            withXml {
                asNode().appendNode("distributionManagement").apply {
                    appendNode("relocation").apply {
                        appendNode("groupId", groupId)
                        appendNode("artifactId", "core-jvm")
                        appendNode("version", version)
                        appendNode("message", "Relocated to io.github.pdvrieze.xmlutil.core-jvm")
                    }
                }
            }
        }
    }

}

doPublish("core-jvmcommon", "Relocation to standard convention module", generateJavadoc =false)

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

