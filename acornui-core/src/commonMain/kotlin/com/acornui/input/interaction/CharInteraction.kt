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

package com.acornui.input.interaction

import com.acornui.input.InteractionEventBase
import com.acornui.input.InteractionEventRo
import com.acornui.input.InteractionType

interface CharInteractionRo : InteractionEventRo {

	val char: Char

	/**
	 * If true, this interaction was triggered from code, not user input.
	 */
	val isFabricated: Boolean

	companion object {
		val CHAR = InteractionType<CharInteractionRo>("char")
	}
}

/**
 * An event representing a character input.
 */
open class CharInteraction : InteractionEventBase(), CharInteractionRo {

	override var char: Char = 0.toChar()

	override var isFabricated: Boolean = false

	fun set(other: CharInteractionRo) {
		char = other.char
		isFabricated = other.isFabricated
	}

	override fun clear() {
		super.clear()
		char = 0.toChar()
		isFabricated = false
	}
}