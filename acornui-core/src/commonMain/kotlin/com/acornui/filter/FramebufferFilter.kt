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

package com.acornui.filter

import com.acornui.AppConfig
import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.di.Owned
import com.acornui.di.inject
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Texture
import com.acornui.math.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Draws a region to a frame buffer.
 */
class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val sprite = Sprite(gl)

	private val _transform = Matrix4()
	
	/**
	 * When rendering to this frame buffer, the canvas region gets expanded out to the next pixel.
	 * This translation matrix represents the canvas coordinate position of the [drawable].
	 */
	val transform: Matrix4Ro = _transform

	override fun updateWorldVertices(regionCanvas: RectangleRo, transform: Matrix4Ro, tint: ColorRo): RectangleRo {
		_transform.setTranslation(regionCanvas.x, regionCanvas.y)
		framebuffer.setSize(regionCanvas.width * scaleX, regionCanvas.height * scaleY, scaleX, scaleY)
		framebuffer.drawable(sprite)
		sprite.updateWorldVertices(transform = _transform, tint = tint)
		return Rectangle(regionCanvas.x, regionCanvas.y, sprite.naturalWidth, sprite.naturalHeight)
	}

	override fun render(inner: () -> Unit) {
		drawToFramebuffer(inner)
		drawToScreen()
	}

	fun drawToFramebuffer(inner: () -> Unit) {
		framebuffer.begin()
//		gl.viewport(-viewport.x, -viewport.y, fB.width, fB.height) // TODO
		gl.clearAndReset(clearColor, clearMask)
		inner()
		framebuffer.end()
	}

	fun drawToScreen() {
		sprite.render()
	}

	/**
	 * Configures a drawable to match what was last rendered. The world vertices will not be updated.
	 * @see transform
	 */
	fun drawable(out: Sprite = Sprite(gl)): Sprite {
		return out.set(sprite)
	}

	override fun dispose() {
		super.dispose()
		framebuffer.dispose()
	}
}

/**
 * A frame buffer filter will cache the render target as a bitmap.
 */
inline fun Owned.framebufferFilter(init: ComponentInit<FramebufferFilter> = {}): FramebufferFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = FramebufferFilter(this)
	b.init()
	return b
}
