/*
 * Copyright 2020 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

rootProject.name = "acornui-skins"

pluginManagement {
	val version: String by settings
	val githubToken: String by settings
	val githubActor: String by settings
	repositories {
		mavenLocal()
		maven("https://maven.pkg.github.com/polyforest/acornui") {
			credentials {
				username = githubActor
				password = githubToken
			}
		}
		gradlePluginPortal()
		maven("https://dl.bintray.com/kotlin/kotlin-eap/")
	}
	resolutionStrategy {
		eachPlugin {
			when {
				requested.id.namespace == "com.acornui" ->
					useVersion(version)
			}
		}
	}
}

include("basic")