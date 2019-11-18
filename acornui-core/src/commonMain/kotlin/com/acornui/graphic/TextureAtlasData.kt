/*
 * Copyright 2019 Poly Forest, LLC
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

@file:Suppress("unused")

package com.acornui.graphic

import com.acornui.asset.CachedGroup
import com.acornui.asset.cachedGroup
import com.acornui.asset.loadAndCacheJsonAsync
import com.acornui.di.Scoped
import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TexturePixelFormat
import com.acornui.math.IntRectangleRo
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author nbilyk
 */
@Serializable
data class TextureAtlasData(
		val pages: List<AtlasPageData>
) {

	/**
	 * Does a search for a region with the given name.
	 * This result should be cached instead of searching every frame.
	 */
	fun findRegion(name: String): Pair<AtlasPageData, AtlasRegionData>? {
		for (page in pages) {
			for (region in page.regions) {
				if (region.name.equalsIgnoreExtension(name)) {
					return Pair(page, region)
				}
			}
		}
		return null
	}

	private fun String.equalsIgnoreExtension(name: String): Boolean {
		if (this.startsWith(name)) {
			if (this.length == name.length) return true
			if (this[name.length] == '.') return true
		}
		return false
	}
}

/**
 * Represents a single texture atlas page.
 */
@Serializable
data class AtlasPageData(

		/**
		 * A path to the texture relative to the atlas page data.
		 */
		val texturePath: String,
		val width: Int,
		val height: Int,
		val pixelFormat: TexturePixelFormat,
		val premultipliedAlpha: Boolean,
		val filterMin: TextureMinFilter,
		val filterMag: TextureMagFilter,
		val regions: List<AtlasRegionData>,
		val hasWhitePixel: Boolean = false
) {

	fun containsRegion(regionName: String): Boolean {
		return regions.find { it.name == regionName } != null
	}

	fun getRegion(regionName: String): AtlasRegionData? {
		return regions.find { it.name == regionName }
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return hashCode() == other?.hashCode()
	}

	private val _hashCode: Int = run {
		var result = texturePath.hashCode()
		result = 31 * result + width
		result = 31 * result + height
		result = 31 * result + pixelFormat.hashCode()
		result = 31 * result + premultipliedAlpha.hashCode()
		result = 31 * result + filterMin.hashCode()
		result = 31 * result + filterMag.hashCode()
		result = 31 * result + regions.hashCode()
		result = 31 * result + hasWhitePixel.hashCode()
		result
	}

	override fun hashCode(): Int {
		return _hashCode
	}
}

fun AtlasPageData.configure(target: Texture): Texture {
	target.pixelFormat = pixelFormat
	target.filterMin = filterMin
	target.filterMag = filterMag
	target.hasWhitePixel = hasWhitePixel
	return target
}

/**
 * A model representing a region within an atlas page.
 */
@Serializable
data class AtlasRegionData(

		/**
		 * The name of the region. This is typically the path of the image used to pack into the region.
		 */
		val name: String,

		/**
		 * True if this region was rotated clockwise 90 degrees.
		 */
		val isRotated: Boolean,

		/**
		 * The bounding rectangle of the region within the page.
		 */
		val bounds: IntRectangleRo,

		/**
		 * Used for 9 patches. An float array of left, top, right, bottom
		 */
		val splits: List<Float>? = null,

		/**
		 * The whitespace padding stripped from the original image, that should be added by the component.
		 * An int array of left, top, right, bottom
		 * The padding values will not be rotated.
		 */
		var padding: List<Int> = listOf(0, 0, 0, 0)
) {

	/**
	 * The original width of the image, before whitespace was stripped.
	 */
	@Transient
	val originalWidth: Int
		get() {
			return if (isRotated)
				padding[0] + padding[2] + bounds.height
			else
				padding[0] + padding[2] + bounds.width
		}

	/**
	 * The original width of the image, before whitespace was stripped.
	 */
	@Transient
	val originalHeight: Int
		get() {
			return if (isRotated)
				padding[1] + padding[3] + bounds.width
			else
				padding[1] + padding[3] + bounds.height
		}

	/**
	 * The packed width of the image, after whitespace was stripped.
	 */
	@Transient
	val packedWidth: Int
		get() = if (isRotated) bounds.height else bounds.width

	/**
	 * The packed height of the image, after whitespace was stripped.
	 */
	@Transient
	val packedHeight: Int
		get() = if (isRotated) bounds.width else bounds.height
}

data class LoadedAtlasRegion(val texture: Texture, val region: AtlasRegionData)

suspend fun Scoped.loadAndCacheAtlasRegion(atlasPath: String, regionName: String, group: CachedGroup = cachedGroup()): LoadedAtlasRegion {
	val atlasData = loadAndCacheJsonAsync(TextureAtlasData.serializer(), atlasPath, group).await()
	val (page, region) = atlasData.findRegion(regionName)
			?: throw RegionNotFoundException(atlasPath, regionName)
	val texture = loadAndCacheAtlasPage(atlasPath, page, group).await()
	return LoadedAtlasRegion(texture, region)
}

class RegionNotFoundException(val atlasPath: String, val regionName: String) : Exception("Region '$regionName' not found in atlas $atlasPath.")

