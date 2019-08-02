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

package com.acornui.component.layout

import com.acornui.component.ComponentInit
import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.di.Owned
import com.acornui.focus.Focusable
import com.acornui.math.Bounds

/**
 * A container with a custom one-off layout.
 * For reusable layouts see [LayoutContainer].
 *
 * Example
 * ```
 *   +customLayout {
 *      val myLabel = +text("Hello")
 *
 *      updateSizeConstraintsCallback = { out ->
 *          out.width.min = 300f + 40f
 *          out.height.min = 40f + 30f
 *      }
 *
 *      updateLayoutCallback = { explicitWidth, explicitHeight, out ->
 *          myLabel.setSize(300f, 40f)
 *          myLabel.moveTo(40f, 30f)
 *          out.set(myLabel.right, myLabel.bottom)
 *      }
 *   }
 * ```
 */
open class CustomLayoutContainer(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), Focusable {

	var updateSizeConstraintsCallback: (out: SizeConstraints) -> Unit = { _ -> }
	var updateLayoutCallback: (explicitWidth: Float?, explicitHeight: Float?, out: Bounds) -> Unit = { _, _, _ -> }

	override fun updateSizeConstraints(out: SizeConstraints) {
		updateSizeConstraintsCallback(out)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		updateLayoutCallback(explicitWidth, explicitHeight, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
	}
}

fun Owned.customLayout(init: ComponentInit<CustomLayoutContainer> = {}): CustomLayoutContainer {
	val c = CustomLayoutContainer(this)
	c.init()
	return c
}
