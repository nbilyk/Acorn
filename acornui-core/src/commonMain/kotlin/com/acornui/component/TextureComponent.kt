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

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.acornui.component

import com.acornui.asset.CacheSet
import com.acornui.asset.cacheSet
import com.acornui.asset.loadAndCacheTexture
import com.acornui.collection.sortedInsertionIndex
import com.acornui.di.Context
import com.acornui.graphic.*
import com.acornui.io.UrlRequestData
import com.acornui.io.toUrlRequestData
import com.acornui.math.IntRectangleRo
import com.acornui.math.RectangleRo
import com.acornui.recycle.Clearable
import kotlinx.coroutines.launch
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A UiComponent representing a single Texture.
 * @author nbilyk
 */
open class TextureComponent(owner: Context) : RenderableComponent<Sprite>(owner), Clearable {

	override val renderable: Sprite = Sprite(gl)

	val naturalWidth: Float
		get() = renderable.naturalWidth

	val naturalHeight: Float
		get() = renderable.naturalHeight

	var blendMode: BlendMode
		get() = renderable.blendMode
		set(value) {
			renderable.blendMode = value
			window.requestRender()
		}

	@Deprecated("", ReplaceWith("TextureComponent(owner).apply { texture(path) }"))
	constructor (owner: Context, path: String) : this(owner) {
		texture(path)
	}

	@Deprecated("", ReplaceWith("TextureComponent(owner).apply { texture(texture) }"))
	constructor (owner: Context, texture: Texture) : this(owner) {
		texture(texture)
	}

	val dpiStyle = bind(DpiStyle())

	private var texturePaths: TexturePaths? = null
	private var currentLoadId = 0
	private var currentDensity: Float = -1f

	init {
		watch(dpiStyle) {
			load()
		}
	}

	/**
	 * If the texture was set explicitly via `texture(value: Texture?)`, that value can be retrieved here.
	 * @see texture
	 */
	var explicitTexture: TextureRo? = null
		private set

	/**
	 * The current texture rendered.
	 */
	val texture: TextureRo?
		get() = renderable.texture

	private var cacheSet: CacheSet? = null
		set(value) {
			field?.dispose()
			field = value
		}

	protected open fun setTextureInternal(value: Texture?) {
		if (renderable.texture == value) return
		val oldTexture = renderable.texture
		if (isActive) {
			value?.refInc()
			oldTexture?.refDec()
		}
		renderable.texture = value
		invalidateLayout()
	}

	/**
	 * Sets the explicit texture.
	 * @param value The texture value to set.
	 * @param scaleX The x dpi scaling of the texture.
	 * @param scaleY The y dpi scaling of the texture.
	 */
	fun texture(value: Texture?, scaleX: Float = 1f, scaleY: Float = 1f) {
		if (explicitTexture == value) return
		clear()
		explicitTexture = value
		setTextureInternal(value)
		renderable.setScaling(scaleX, scaleY)
	}

	/**
	 * Loads the texture at the given path and sets the texture on completion.
	 * This will clear the current texture immediately.
	 */
	fun texture(path: String) = texture(TexturePaths(mapOf(1f to path.toUrlRequestData())))

	fun texture(paths: Map<Float, UrlRequestData>) = texture(TexturePaths(paths))

	/**
	 * Sets the texture as a map of pixel densities to texture paths.
	 * @see toUrlRequestData
	 */
	fun texture(paths: Map<Float, String>) = texture(TexturePaths(paths.mapValues { it.value.toUrlRequestData() }))

	/**
	 * Loads the texture from the given url request and sets the texture on completion.
	 * This will immediately cancel and clear any current texture.
	 */
	fun texture(paths: TexturePaths) {
		clear()
		cacheSet = cacheSet()
		texturePaths = paths
	}

	private fun load() {
		val texturePaths = texturePaths ?: return
		val cacheSet = cacheSet!!

		val requestedDensity = maxOf(dpiStyle.scaleX, dpiStyle.scaleY)
		val actualDensity = pickBestDensity(texturePaths, requestedDensity)

		val scale = scaleIfPastAffordance(actualDensity, requestedDensity, dpiStyle.scalingSnapAffordance)
		renderable.setScaling(dpiStyle.scaleX * scale, dpiStyle.scaleY * scale)

		if (currentDensity == actualDensity) return
		currentDensity = actualDensity

		launch {
			val request = texturePaths.paths[actualDensity] as UrlRequestData
			setTextureInternal(loadAndCacheTexture(request, cacheSet = cacheSet))
		}
	}

	protected open fun pickBestDensity(texturePaths: TexturePaths, requestedDensity: Float): Float {
		val densities = texturePaths.densities
		val nearestSupportedIndex = minOf(densities.sortedInsertionIndex(requestedDensity, matchForwards = false), densities.lastIndex)
		return densities[nearestSupportedIndex]
	}

	/**
	 * If true, the texture's region is rotated.
	 */
	val isRotated: Boolean
		get() = renderable.isRotated

	override fun onActivated() {
		super.onActivated()
		renderable.texture?.refInc()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		renderable.texture?.refDec()
	}

	/**
	 * Sets the UV coordinates of the image to display.
	 */
	fun setUv(u: Float, v: Float, u2: Float, v2: Float, isRotated: Boolean = false) {
		renderable.setUv(u, v, u2, v2, isRotated)
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Sets the region of the texture to display.
	 */
	fun region(x: Float, y: Float, width: Float, height: Float, isRotated: Boolean = false) {
		renderable.region(x, y, width, height, isRotated)
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Sets the region of the texture to display.
	 */
	fun region(region: RectangleRo, isRotated: Boolean = false) {
		region(region.x, region.y, region.width, region.height, isRotated)
	}

	fun region(region: IntRectangleRo, isRotated: Boolean = false) {
		region(region.x.toFloat(), region.y.toFloat(), region.width.toFloat(), region.height.toFloat(), isRotated)
	}

	/**
	 * Clears the current texture. If a texture is currently being loaded via `texture(path)`, the loading will cancel.
	 */
	override fun clear() {
		texturePaths = null
		explicitTexture = null
		cacheSet = null
		setTextureInternal(null)
		invalidateLayout()
	}

	override fun dispose() {
		clear()
		super.dispose()
	}
}

data class TexturePaths(
		val paths: Map<Float, UrlRequestData>
) {

	val densities: List<Float> = paths.keys.toList()
}

inline fun Context.textureC(init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TextureComponent(this).apply(init)
}

inline fun Context.textureC(path: String, init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TextureComponent(this).apply {
		texture(path)
		init()
	}
}

inline fun Context.textureC(paths: Map<Float, String>, init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TextureComponent(this).apply {
		texture(paths)
		init()
	}
}

inline fun Context.textureC(texture: Texture, init: ComponentInit<TextureComponent> = {}): TextureComponent  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TextureComponent(this).apply {
		texture(texture)
		init()
	}
}