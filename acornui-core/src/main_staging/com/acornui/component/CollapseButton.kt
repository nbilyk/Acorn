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

package com.acornui.component

import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class CollapseButton(
		owner: Context
) : ButtonImpl(owner) {

	init {
		addClass(CollapseButton)
		toggleOnClick = true
	}

	companion object : StyleTag
}

inline fun Context.collapseButton(init: ComponentInit<CollapseButton> = {}): CollapseButton  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = CollapseButton(this)
	c.init()
	return c
}

inline fun Context.collapseButton(label: String, init: ComponentInit<CollapseButton> = {}): CollapseButton  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = CollapseButton(this)
	b.label = label
	b.init()
	return b
}