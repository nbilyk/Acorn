@file:Suppress("UnstableApiUsage", "UNUSED_PARAMETER", "unused")

package com.acornui.build.plugins.tasks

import com.acornui.build.plugins.tasks.fileprocessors.TokenReplacementFileProcessor
import com.acornui.font.processFonts
import com.acornui.texturepacker.packAssets
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.extra
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

open class AcornUiResourceProcessorTask @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {

	@Incremental
	@PathSensitive(PathSensitivity.RELATIVE)
	@InputFiles
	val sources: ConfigurableFileCollection = objects.fileCollection()

	/**
	 * Adds the given file tree to the list of sources.
	 */
	fun from(tree: FileTree): AcornUiResourceProcessorTask {
		sources.from(tree)
		return this
	}

	fun from(source: File): AcornUiResourceProcessorTask {
		sources.from(source)
		return this
	}

	fun from(sources: Iterable<File>): AcornUiResourceProcessorTask {
		this.sources.from(sources)
		return this
	}

	fun into(directory: File?) {
		outputDir.set(directory)
	}

	fun into(directory: Directory?) {
		outputDir.set(directory)
	}

	@get:OutputDirectory
	val outputDir: DirectoryProperty = objects.directoryProperty()

	private val directoryProcessors: Map<String, DirectoryProcessor> = mapOf("_unpacked" to ::packAcornAssets, "_unprocessedFonts" to ::processBitmapFonts)

	/**
	 * The file extensions to be considered text files and transform with [textFileProcessors]. (Lowercase)
	 */
	var textFilePatterns = listOf("asp", "aspx", "cfm", "cshtml", "css", "go", "htm", "html", "json", "jsp", "jspx",
			"php", "php3", "php4", "phtml", "rhtml", "txt", "properties")

	var textFileProcessors: List<TextFileProcessor> = listOf(TokenReplacementFileProcessor())

	@TaskAction
	fun execute(inputChanges: InputChanges) {
		val directoriesToProcess = mutableMapOf<String, MutableSet<DirectoryToProcessEntry>>()
		directoriesToProcess += directoryProcessors.map { it.key to mutableSetOf<DirectoryToProcessEntry>() }

		val properties = project.extra.properties.mapValues { it.value.toString() }

		inputChanges.getFileChanges(sources).forEach { change ->
			val relPath = change.normalizedPath
			if (relPath.isEmpty()) return@forEach
			val sourceFile = change.file
			val targetFile = outputDir.file(relPath).get().asFile

			val found = change.file.findSpecialFolder()
			if (found != null) {
				val (suffix, foundSpecialFolder) = found
				val specialFolderToFilePath = sourceFile.relativeTo(foundSpecialFolder).invariantSeparatorsPath
				val specialFolderDest = outputDir.file(relPath.removeSuffix(specialFolderToFilePath)).get().asFile
				val dest = specialFolderDest.parentFile.resolve(specialFolderDest.name.removeSuffix(suffix))

				directoriesToProcess[suffix]!!.add(DirectoryToProcessEntry(
						foundSpecialFolder,
						dest
				))
			} else {
				if (change.changeType == ChangeType.REMOVED || !sourceFile.exists()) {
					if (targetFile.exists())
						targetFile.deleteRecursively()
				} else {
					if (change.fileType != FileType.DIRECTORY) {
						targetFile.parentFile.mkdirs()
						if (textFileProcessors.isNotEmpty() && textFilePatterns.contains(targetFile.extension.toLowerCase())) {
							var str = sourceFile.readText()
							for (fileProcessor in textFileProcessors) {
								str = fileProcessor.process(targetFile.path, str, properties)
							}
							targetFile.writeText(str)
						} else {
							sourceFile.copyTo(targetFile, overwrite = true)
						}
					}
				}
			}
		}

		directoryProcessors.forEach { (suffix, processor) ->
			val directoryToProcess = directoriesToProcess[suffix]!!
			if (directoryToProcess.isNotEmpty()) {
				processor.invoke(suffix, directoryToProcess)
			}
		}
	}

	private fun File.findSpecialFolder(): Pair<String, File>? {
		val keys = directoryProcessors.keys
		var p: File? = this
		while (p != null) {
			if (p.isDirectory) {
				val found = keys.find { p!!.name.endsWith(it) }
				if (found != null)
					return found to p
			}
			p = p.parentFile
		}
		return null
	}

	private val packedExtensions = arrayOf("json", "png")

	private fun packAcornAssets(suffix: String, entries: Iterable<DirectoryToProcessEntry>) {
		entries.forEach {
			if (it.sourceDir.exists()) {
				logger.lifecycle("Packing assets: ${it.sourceDir.path} dest: ${it.destinationDir.parentFile} outputDir: $outputDir")
				packAssets(it.sourceDir, it.destinationDir.parentFile, suffix)
			} else {
				logger.lifecycle("Removing assets: " + it.sourceDir.path)
				val name = it.sourceDir.name.removeSuffix(suffix)
				it.destinationDir.parentFile.listFiles()?.forEach { child ->
					if (child.name.startsWith(name) && packedExtensions.contains(child.extension.toLowerCase()))
						child.delete()
				}
			}
		}
	}

	private fun processBitmapFonts(suffix: String, entries: Iterable<DirectoryToProcessEntry>) {
		entries.forEach {
			it.destinationDir.deleteRecursively()
			if (it.sourceDir.exists()) {
				logger.lifecycle("Processing fonts: " + it.sourceDir.path)
				processFonts(it.sourceDir, it.destinationDir)
			} else {
				logger.lifecycle("Removing fonts: " + it.destinationDir.path)
			}
		}
	}
}

data class DirectoryToProcessEntry(
		val sourceDir: File,
		val destinationDir: File
)

typealias DirectoryProcessor = (suffix: String, entries: Iterable<DirectoryToProcessEntry>) -> Unit

interface TextFileProcessor {
	fun process(path: String, input: String, properties: Map<String, String>): String
}