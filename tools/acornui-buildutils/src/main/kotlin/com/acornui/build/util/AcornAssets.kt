/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.build.util

import com.acornui.core.asset.AssetManager
import com.acornui.core.di.inject
import com.acornui.core.io.file.Files
import com.acornui.io.file.FilesManifestSerializer
import com.acornui.jvm.JvmHeadlessApplication
import com.acornui.jvm.io.file.ManifestUtil
import com.acornui.serialization.JsonSerializer
import com.acornui.texturepacker.jvm.TexturePackerUtil
import java.io.File

object AcornAssets {

	fun packAssets(dest: File) {

		JvmHeadlessApplication(dest.path).start {
			// Pack the assets in all directories in the source folder with a name ending in "_unpacked"
			TexturePackerUtil(inject(Files), inject(AssetManager)).packAssets(dest, File("."))

			dest.setLastModified(System.currentTimeMillis())
		}
	}

	fun writeManifest(source: File, dest: File, root: File) {
		val manifest = ManifestUtil.createManifest(source, root)
		dest.let {
			it.mkdirs()
			File(it, "files.json").writeText(JsonSerializer.write(manifest, FilesManifestSerializer))
		}
	}

	private fun copyAssets(source: File, destination: File) {
		if (source.exists()) {
			destination.mkdir()
			source.copyRecursively(destination, true)
		}
	}

}