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
import com.acornui.di.Owned
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class CheckboxImpl(
		owner: Owned
) : ButtonImpl(owner) {

	init {
		styleTags.add(CheckboxImpl)
		toggleOnClick = true
	}

	companion object : StyleTag
}

inline fun Owned.checkbox(init: ComponentInit<CheckboxImpl> = {}): CheckboxImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = CheckboxImpl(this)
	c.init()
	return c
}

inline fun Owned.checkbox(label: String, init: ComponentInit<CheckboxImpl> = {}): CheckboxImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = CheckboxImpl(this)
	b.label = label
	b.init()
	return b
}
