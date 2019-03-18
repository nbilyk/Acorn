/*
 * Copyright 2018 Poly Forest
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

package com.acornui.component

import com.acornui.component.drawing.*
import com.acornui.core.di.Owned
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.putIndex
import com.acornui.gl.core.putQuadIndices
import com.acornui.gl.core.putTriangleIndices
import com.acornui.gl.core.putVertex
import com.acornui.graphic.Color
import com.acornui.math.Bounds
import com.acornui.math.MinMaxRo
import com.acornui.math.PI
import com.acornui.math.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

open class Rect(
		owner: Owned
) : ContainerImpl(owner) {

	val style = bind(BoxStyle())

	var segments = 40

	/**
	 * If true, we don't need a mesh -- no corners and no gradient, just use the sprite batch.
	 */
	private var simpleMode = false
	private val simpleModeObj by lazy { SimpleMode() }
	private val complexModeObj by lazy { ComplexMode() }

	private inner class SimpleMode {
		val outerRect = Array(4) { Vector3() }
		val innerRect = Array(4) { Vector3() }
		val fillColor = Color()
		val borderColors = BorderColors()
		val normal = Vector3()
	}

	private inner class ComplexMode {

		val topLeftCorner = Sprite()
		val topLeftStrokeCorner = Sprite()
		val topRightCorner = Sprite()
		val topRightStrokeCorner = Sprite()
		val bottomRightCorner = Sprite()
		val bottomRightStrokeCorner = Sprite()
		val bottomLeftCorner = Sprite()
		val bottomLeftStrokeCorner = Sprite()

		val fill = staticMesh()
		val gradient = staticMesh()
		val stroke = staticMesh()
		val fillC = staticMeshC {
			mesh = fill
			interactivityMode = InteractivityMode.NONE
		}
		val gradientC = staticMeshC {
			mesh = gradient
			interactivityMode = InteractivityMode.NONE
		}
		val strokeC = staticMeshC {
			mesh = stroke
			interactivityMode = InteractivityMode.NONE
		}
	}

	init {
		defaultWidth = 100f
		defaultHeight = 50f
		watch(style) {
			simpleMode = it.borderRadii.isEmpty() && it.linearGradient == null
			if (simpleMode) clearChildren(dispose = false)
			else {
				addChild(complexModeObj.fillC)
				addChild(complexModeObj.gradientC)
				addChild(complexModeObj.strokeC)
			}
		}
		validation.addNode(ValidationFlags.RESERVED_1, ValidationFlags.STYLES or ValidationFlags.CONCATENATED_TRANSFORM or ValidationFlags.LAYOUT or ValidationFlags.CONCATENATED_COLOR_TRANSFORM, this::updateSimpleModeVertices)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (simpleMode) return
		val margin = style.margin
		val w = margin.reduceWidth2(explicitWidth ?: 0f)
		val h = margin.reduceHeight2(explicitHeight ?: 0f)
		if (w <= 0f || h <= 0f) return

		val corners = style.borderRadii

		complexModeObj.apply {
			fill.clear()
			stroke.clear()
			gradient.clear()

			val topLeftX = fitSize(corners.topLeft.x, corners.topRight.x, w)
			val topLeftY = fitSize(corners.topLeft.y, corners.bottomLeft.y, h)
			val topRightX = fitSize(corners.topRight.x, corners.topLeft.x, w)
			val topRightY = fitSize(corners.topRight.y, corners.bottomRight.y, h)
			val bottomRightX = fitSize(corners.bottomRight.x, corners.bottomLeft.x, w)
			val bottomRightY = fitSize(corners.bottomRight.y, corners.topRight.y, h)
			val bottomLeftX = fitSize(corners.bottomLeft.x, corners.bottomRight.x, w)
			val bottomLeftY = fitSize(corners.bottomLeft.y, corners.topLeft.y, h)

			// Stroke properties.
			val borderColors = style.borderColors
			val border = style.borderThicknesses
//			val topBorder = minOf(h - bottomLeftY, h - bottomRightY, fitSize(border.top, border.bottom, h))
			val topBorder = fitSize(border.top, border.bottom, h)
			val leftBorder = fitSize(border.left, border.right, w)
			val rightBorder = fitSize(border.right, border.left, w)
			val bottomBorder = fitSize(border.bottom, border.top, h)
			val innerTopLeftX = maxOf(topLeftX, leftBorder)
			val innerTopLeftY = maxOf(topLeftY, topBorder)
			val innerTopRightX = maxOf(topRightX, rightBorder)
			val innerTopRightY = maxOf(topRightY, topBorder)
			val innerBottomRightX = maxOf(bottomRightX, rightBorder)
			val innerBottomRightY = maxOf(bottomRightY, bottomBorder)
			val innerBottomLeftX = maxOf(bottomLeftX, leftBorder)
			val innerBottomLeftY = maxOf(bottomLeftY, bottomBorder)

			createSmoothCorner(topLeftX, topLeftY, spriteOut = topLeftCorner, flipX = true, flipY = true)
			createSmoothCorner(topRightX, topRightY, spriteOut = topRightCorner, flipX = false, flipY = true)
			createSmoothCorner(bottomRightX, bottomRightY, spriteOut = bottomRightCorner, flipX = false, flipY = false)
			createSmoothCorner(bottomLeftX, bottomLeftY, spriteOut = bottomLeftCorner, flipX = true, flipY = false)

			createSmoothCorner(topLeftX, topLeftY, strokeThicknessX = leftBorder, strokeThicknessY = topBorder, spriteOut = topLeftStrokeCorner, flipX = true, flipY = true)
			createSmoothCorner(topRightX, topRightY, strokeThicknessX = rightBorder, strokeThicknessY = topBorder, spriteOut = topRightStrokeCorner, flipX = false, flipY = true)
			createSmoothCorner(bottomRightX, bottomRightY, strokeThicknessX = rightBorder, strokeThicknessY = bottomBorder, spriteOut = bottomRightStrokeCorner, flipX = false, flipY = false)
			createSmoothCorner(bottomLeftX, bottomLeftY, strokeThicknessX = leftBorder, strokeThicknessY = bottomBorder, spriteOut = bottomLeftStrokeCorner, flipX = true, flipY = false)

			fill.buildMesh {
				// If we have a linear gradient, fill with white; we will be using the fill as a mask inside draw.
				val tint = if (style.linearGradient == null) style.backgroundColor else Color.WHITE
				if (tint.a > 0f) {
					run {
						// Middle vertical strip
						val left = maxOf(topLeftX, bottomLeftX)
						val right = w - maxOf(topRightX, bottomRightX)
						val width = right - left
						if (width > 0f)
							rect(left, 0f, width, h, tint)
					}
					if (topLeftX > 0f || bottomLeftX > 0f) {
						// Left vertical strip
						val width = maxOf(topLeftX, bottomLeftX)
						val height = h - bottomLeftY - topLeftY
						if (height > 0f)
							rect(0f, topLeftY, width, height, tint)
					}
					if (topRightX > 0f || bottomRightX > 0f) {
						// Right vertical strip
						val width = maxOf(topRightX, bottomRightX)
						val height = h - bottomRightY - topRightY
						if (height > 0f)
							rect(w - width, topRightY, width, height, tint)
					}
					if (topLeftX < bottomLeftX) {
						// Vertical slice to the right of top left corner
						if (topLeftY > 0f)
							rect(topLeftX, 0f, bottomLeftX - topLeftX, topLeftY, tint)
					} else if (topLeftX > bottomLeftX) {
						// Vertical slice to the right of bottom left corner
						if (bottomLeftY > 0f)
							rect(bottomLeftX, h - bottomLeftY, topLeftX - bottomLeftX, bottomLeftY, tint)
					}
					if (topRightX < bottomRightX) {
						// Vertical slice to the left of top right corner
						if (topRightY > 0f)
							rect(w - bottomRightX, 0f, bottomRightX - topRightX, topRightY, tint)
					} else if (topRightX > bottomRightX) {
						// Vertical slice to the left of bottom right corner
						if (bottomRightY > 0f)
							rect(w - topRightX, h - bottomRightY, topRightX - bottomRightX, bottomRightY, tint)
					}

					if (topLeftCorner.texture != null) {
						topLeftCorner.updateVertices(x = 0f, y = 0f)
						topLeftCorner.draw(glState, tint)
					}

					if (topRightCorner.texture != null) {
						topRightCorner.updateVertices(x = w - topRightX, y = 0f)
						topRightCorner.draw(glState, tint)
					}

					if (bottomRightCorner.texture != null) {
						bottomRightCorner.updateVertices(x = w - bottomRightX, y = h - bottomRightY)
						bottomRightCorner.draw(glState, tint)
					}

					if (bottomLeftCorner.texture != null) {
						bottomLeftCorner.updateVertices(x = 0f, y = h - bottomLeftY)
						bottomLeftCorner.draw(glState, tint)
					}

					trn(margin.left, margin.top)
				}
			}

			stroke.buildMesh {

				if (topBorder > 0f && borderColors.top.a > 0f) {
					// Top middle
					val width = w - innerTopRightX - innerTopLeftX
					if (width > 0f)
						rect(innerTopLeftX, 0f, width, topBorder, borderColors.top)
				}

				if (rightBorder > 0f && borderColors.right.a > 0f) {
					// Right middle
					val height = h - innerBottomRightY - innerTopRightY
					if (height > 0f)
						rect(w - rightBorder, innerTopRightY, rightBorder, height, borderColors.right)
				}

				if (bottomBorder > 0f && borderColors.bottom.a > 0f) {
					// Bottom middle
					val width = w - innerBottomRightX - innerBottomLeftX
					if (width > 0f)
						rect(innerBottomLeftX, h - bottomBorder, width, bottomBorder, borderColors.bottom)
				}

				if (leftBorder > 0f && borderColors.left.a > 0f) {
					// Left middle
					val height = h - innerBottomLeftY - innerTopLeftY
					if (height > 0f)
						rect(0f, innerTopLeftY, leftBorder, height, borderColors.left)

				}

//				if (topLeftStrokeCorner.texture != null) {
//					topLeftStrokeCorner.updateVertices(x = 0f, y = 0f)
//					topLeftStrokeCorner.draw(glState, borderColors.top) // TODO: Colors
//				}

				topLeftStrokeCorner.apply {
					val texture = texture
					val u: Float
					val v: Float
					val u2: Float
					val v2: Float
					if (texture != null) {
						glState.setTexture(texture)
						u = this.u
						u2 = (topLeftX - innerTopLeftX) / texture.width
						v = this.v
						v2 = (topLeftY - innerTopLeftY) / texture.height
					} else {
						glState.setTexture(glState.whitePixel)
						u = 0f; v = 0f; u2 = 0f; v2 = 0f
					}
					val x2 = innerTopLeftX
					val y2 = innerTopLeftY
					batch.putVertex(0f, 0f, 0f, colorTint = borderColors.top, u = u, v = v)
					batch.putVertex(x2, 0f, 0f, colorTint = borderColors.top, u = u2, v = v)
					batch.putVertex(x2, y2, 0f, colorTint = borderColors.top, u = u2, v = v2)
					batch.putTriangleIndices()

					batch.putVertex(x2, y2, 0f, colorTint = borderColors.left, u = u2, v = v2)
					batch.putVertex(0f, y2, 0f, colorTint = borderColors.left, u = u, v = v2)
					batch.putVertex(0f, 0f, 0f, colorTint = borderColors.left, u = u, v = v)
					batch.putTriangleIndices()
				}

				topRightStrokeCorner.apply {
					val texture = texture
					val u: Float
					val v: Float
					val u2: Float
					val v2: Float
					if (texture != null) {
						glState.setTexture(texture)
						u = (topRightX - innerTopRightX) / texture.width
						u2 = this.u2
						v = this.v
						v2 = (topRightY - innerTopRightY) / texture.height
					} else {
						glState.setTexture(glState.whitePixel)
						u = 0f; v = 0f; u2 = 0f; v2 = 0f
					}
					val x = w - innerTopRightX
					batch.putVertex(x, 0f, 0f, colorTint = borderColors.top, u = u, v = v)
					batch.putVertex(w, 0f, 0f, colorTint = borderColors.top, u = u2, v = v)
					batch.putVertex(x, innerTopRightY, 0f, colorTint = borderColors.top, u = u, v = v2)
					batch.putTriangleIndices()

					batch.putVertex(w, 0f, 0f, colorTint = borderColors.right, u = u2, v = v)
					batch.putVertex(w, innerTopRightY, 0f, colorTint = borderColors.right, u = u2, v = v2)
					batch.putVertex(x, innerTopRightY, 0f, colorTint = borderColors.right, u = u, v = v2)
					batch.putTriangleIndices()
				}

				bottomRightStrokeCorner.apply {
					val texture = texture
					val u: Float
					val v: Float
					val u2: Float
					val v2: Float
					if (texture != null) {
						glState.setTexture(texture)
						u = (bottomRightX - innerBottomRightX) / texture.width
						u2 = this.u2
						v = (bottomRightY - innerBottomRightY) / texture.height
						v2 = this.v2
					} else {
						glState.setTexture(glState.whitePixel)
						u = 0f; v = 0f; u2 = 0f; v2 = 0f
					}
					val x = w - innerBottomRightX
					val y = h - innerBottomRightY
					batch.putVertex(x, y, 0f, colorTint = borderColors.right, u = u, v = v)
					batch.putVertex(w, y, 0f, colorTint = borderColors.right, u = u2, v = v)
					batch.putVertex(w, h, 0f, colorTint = borderColors.right, u = u2, v = v2)
					batch.putTriangleIndices()

					batch.putVertex(x, y, 0f, colorTint = borderColors.bottom, u = u, v = v)
					batch.putVertex(w, h, 0f, colorTint = borderColors.bottom, u = u2, v = v2)
					batch.putVertex(x, h, 0f, colorTint = borderColors.bottom, u = u, v = v2)
					batch.putTriangleIndices()
				}

				bottomLeftStrokeCorner.apply {
					val texture = texture
					val u: Float
					val v: Float
					val u2: Float
					val v2: Float
					if (texture != null) {
						glState.setTexture(texture)
						u = this.u
						u2 = (bottomLeftX - innerBottomLeftX) / texture.width
						v = (bottomLeftY - innerBottomLeftY) / texture.height
						v2 = this.v2
					} else {
						glState.setTexture(glState.whitePixel)
						u = 0f; v = 0f; u2 = 0f; v2 = 0f
					}
					val y = h - innerBottomLeftY
					batch.putVertex(0f, y, 0f, colorTint = borderColors.left, u = u, v = v)
					batch.putVertex(innerBottomLeftX, y, 0f, colorTint = borderColors.left, u = u2, v = v)
					batch.putVertex(0f, h, 0f, colorTint = borderColors.left, u = u, v = v2)
					batch.putTriangleIndices()

					batch.putVertex(innerBottomLeftX, y, 0f, colorTint = borderColors.bottom, u = u2, v = v)
					batch.putVertex(innerBottomLeftX, h, 0f, colorTint = borderColors.bottom, u = u2, v = v2)
					batch.putVertex(0f, h, 0f, colorTint = borderColors.bottom, u = u, v = v2)
					batch.putTriangleIndices()
				}

//				if (bottomRightStrokeCorner.texture != null) {
//					bottomRightStrokeCorner.updateVertices(x = w - bottomRightX, y = h - bottomRightY)
//					bottomRightStrokeCorner.draw(glState, borderColors.right)
//				}
//				if (bottomLeftStrokeCorner.texture != null) {
//					bottomLeftStrokeCorner.updateVertices(x = 0f, y = h - bottomLeftY)
//					bottomLeftStrokeCorner.draw(glState, borderColors.bottom) // TODO: Colors
//				}
				trn(margin.left, margin.top)
			}

			if (style.linearGradient != null) {
				val linearGradient = style.linearGradient!!
				gradient.buildMesh {
					val angle = linearGradient.getAngle(w, h) - PI * 0.5f
					val a = cos(angle) * w
					val b = sin(angle) * h
					val len = abs(a) + abs(b)
					val thickness = sqrt(w * w + h * h)

					var pixel = 0f
					var n = 2
					putVertex(0f, 0f, 0f, colorTint = linearGradient.colorStops[0].color)
					putVertex(0f, thickness, 0f, colorTint = linearGradient.colorStops[0].color)
					val numColorStops = linearGradient.colorStops.size
					for (i in 0..numColorStops - 1) {
						val colorStop = linearGradient.colorStops[i]

						if (colorStop.percent != null) {
							pixel = maxOf(pixel, colorStop.percent!! * len)
						} else if (colorStop.pixels != null) {
							pixel = maxOf(pixel, colorStop.pixels!!)
						} else if (i == numColorStops - 1) {
							pixel = len
						} else if (i > 0) {
							var nextKnownPixel = len
							var nextKnownJ = numColorStops - 1
							for (j in (i + 1)..linearGradient.colorStops.lastIndex) {
								val jColorStop = linearGradient.colorStops[j]
								if (jColorStop.percent != null) {
									nextKnownJ = j
									nextKnownPixel = maxOf(pixel, jColorStop.percent!! * len)
									break
								} else if (jColorStop.pixels != null) {
									nextKnownJ = j
									nextKnownPixel = maxOf(pixel, jColorStop.pixels!!)
									break
								}
							}
							pixel += (nextKnownPixel - pixel) / (1f + nextKnownJ.toFloat() - i.toFloat())
						}
						if (pixel > 0f) {
							putVertex(pixel, 0f, 0f, colorTint = colorStop.color)
							putVertex(pixel, thickness, 0f, colorTint = colorStop.color)

							if (i > 0) {
								putIndex(n)
								putIndex(n + 1)
								putIndex(n - 1)
								putIndex(n - 1)
								putIndex(n - 2)
								putIndex(n)
							}
							n += 2
						}
					}

					if (pixel < len) {
						val lastColor = linearGradient.colorStops.last().color
						putVertex(len, 0f, 0f, colorTint = lastColor)
						putVertex(len, thickness, 0f, colorTint = lastColor)
						putIndex(n)
						putIndex(n + 1)
						putIndex(n - 1)
						putIndex(n - 1)
						putIndex(n - 2)
						putIndex(n)
					}

					transform(position = Vector3(margin.left + w * 0.5f, margin.top + h * 0.5f), rotation = Vector3(z = angle), origin = Vector3(len * 0.5f, thickness * 0.5f))
				}
			}
		}

	}

	private fun updateSimpleModeVertices() {
		if (!simpleMode) return
		simpleModeObj.apply {
			val margin = style.margin
			val w = margin.reduceWidth2(width)
			val h = margin.reduceHeight2(height)
			if (w <= 0f || h <= 0f) return
			val cT = _concatenatedTransform

			val borderThicknesses = style.borderThicknesses

			val innerX = margin.left + borderThicknesses.left
			val innerY = margin.top + borderThicknesses.top
			val fillW = borderThicknesses.reduceWidth2(w)
			val fillH = borderThicknesses.reduceHeight2(h)
			cT.prj(innerRect[0].set(innerX, innerY, 0f))
			cT.prj(innerRect[1].set(innerX + fillW, innerY, 0f))
			cT.prj(innerRect[2].set(innerX + fillW, innerY + fillH, 0f))
			cT.prj(innerRect[3].set(innerX, innerY + fillH, 0f))

			if (style.borderThicknesses.isNotEmpty()) {
				val outerX = margin.left
				val outerY = margin.top
				cT.prj(outerRect[0].set(outerX, outerY, 0f))
				cT.prj(outerRect[1].set(outerX + w, outerY, 0f))
				cT.prj(outerRect[2].set(outerX + w, outerY + h, 0f))
				cT.prj(outerRect[3].set(outerX, outerY + h, 0f))
			}

			cT.rot(normal.set(Vector3.NEG_Z)).nor()

			val tint = concatenatedColorTint
			fillColor.set(style.backgroundColor).mul(tint)
			borderColors.set(style.borderColors).mul(tint)
		}
	}

	override fun draw(clip: MinMaxRo) {
		val margin = style.margin
		val w = margin.reduceWidth2(_bounds.width)
		val h = margin.reduceHeight2(_bounds.height)
		if (w <= 0f || h <= 0f) return
		if (simpleMode) {
			simpleModeObj.apply {
				val batch = glState.batch
				glState.setTexture(glState.whitePixel)
				glState.setCamera(camera)
				glState.blendMode(BlendMode.NORMAL, false)
				batch.begin()

				val fillColor = fillColor
				if (fillColor.a > 0f) {
					// Fill
					batch.putVertex(innerRect[0], normal, fillColor)
					batch.putVertex(innerRect[1], normal, fillColor)
					batch.putVertex(innerRect[2], normal, fillColor)
					batch.putVertex(innerRect[3], normal, fillColor)
					batch.putQuadIndices()
				}

				val borderThicknesses = style.borderThicknesses
				val borderColors = borderColors

				if (borderThicknesses.left > 0f) {
					batch.putVertex(outerRect[0], normal, borderColors.left)
					batch.putVertex(innerRect[0], normal, borderColors.left)
					batch.putVertex(innerRect[3], normal, borderColors.left)
					batch.putVertex(outerRect[3], normal, borderColors.left)
					batch.putQuadIndices()
				}

				if (borderThicknesses.top > 0f) {
					batch.putVertex(outerRect[0], normal, borderColors.top)
					batch.putVertex(outerRect[1], normal, borderColors.top)
					batch.putVertex(innerRect[1], normal, borderColors.top)
					batch.putVertex(innerRect[0], normal, borderColors.top)
					batch.putQuadIndices()
				}

				if (borderThicknesses.right > 0f) {
					batch.putVertex(innerRect[1], normal, borderColors.right)
					batch.putVertex(outerRect[1], normal, borderColors.right)
					batch.putVertex(outerRect[2], normal, borderColors.right)
					batch.putVertex(innerRect[2], normal, borderColors.right)
					batch.putQuadIndices()
				}

				if (borderThicknesses.bottom > 0f) {
					batch.putVertex(innerRect[3], normal, borderColors.bottom)
					batch.putVertex(innerRect[2], normal, borderColors.bottom)
					batch.putVertex(outerRect[2], normal, borderColors.bottom)
					batch.putVertex(outerRect[3], normal, borderColors.bottom)
					batch.putQuadIndices()
				}
			}
		} else {
			if (style.linearGradient != null) {
				complexModeObj.apply {
					StencilUtil.mask(glState.batch, gl, {
						if (fillC.visible)
							fillC.render(clip)
					}) {
						if (gradientC.visible)
							gradientC.render(clip)
						if (strokeC.visible)
							strokeC.render(clip)
					}
				}
			} else {
				super.draw(clip)
			}
		}
	}
}

/**
 * Proportionally scales value to fit in max if `value + other > max`
 */
private fun fitSize(value: Float, other: Float, max: Float): Float {
	val v1 = if (value < 0f) 0f else value
	val v2 = if (other < 0f) 0f else other
	val total = v1 + v2
	return if (total > max) {
		v1 * max / total
	} else {
		v1
	}
}

fun Owned.rect(init: ComponentInit<Rect> = {}): Rect {
	val r = Rect(this)
	r.init()
	return r
}