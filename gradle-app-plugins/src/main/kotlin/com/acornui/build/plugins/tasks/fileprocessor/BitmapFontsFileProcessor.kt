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

package com.acornui.build.plugins.tasks.fileprocessor

import com.acornui.font.processFonts
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import java.io.File

class BitmapFontsFileProcessor : DirectoryChangeProcessorBase() {

	@Input
	var suffix = "_unprocessedFonts"
		set(value) {
			field = value
			directoryRegex = Regex(".*$suffix$")
		}

	override var directoryRegex: Regex = Regex(".*$suffix$")

	override fun process(sourceDir: File, destinationDir: File, task: Task) {
		val destinationDirNoSuffix = destinationDir.removeSuffix(suffix)
		destinationDirNoSuffix.deleteRecursively()
		if (sourceDir.exists()) {
			task.logger.lifecycle("Processing fonts: " + sourceDir.path)
			processFonts(sourceDir, destinationDirNoSuffix)
		} else {
			task.logger.lifecycle("Removing fonts: " + destinationDirNoSuffix.path)
		}
	}
}