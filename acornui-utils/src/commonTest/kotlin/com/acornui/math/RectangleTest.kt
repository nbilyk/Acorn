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

package com.acornui.math

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RectangleTest {
	@Test
	fun serialize() {
		val v = Foo648(Rectangle(1.1f, 2.1f, 3.1f, 4.1f))
		val str = jsonStringify(Foo648.serializer(), v)
		assertEquals("""{"rect":[1.1,2.1,3.1,4.1]}""", str)
		assertEquals(v, jsonParse(Foo648.serializer(), """{"rect":[1.1,2.1,3.1,4.1]}"""))
	}

	@Test
	fun area() {
		assertEquals(12f, Rectangle(1f, 2f, 3f, 4f).area)
	}

	@Test
	fun containsPoint() {
		assertTrue(Rectangle(1f, 2f, 3f, 4f).contains(2f, 3f))
		assertTrue(Rectangle(1f, 2f, 3f, 4f).contains(3.99f, 5.99f))
		assertFalse(Rectangle(1f, 2f, 3f, 4f).contains(1f, 2f))
		assertFalse(Rectangle(1f, 2f, 3f, 4f).contains(0f, 2f))
		assertFalse(Rectangle(1f, 2f, 3f, 4f).contains(2f, 0f))
		assertTrue(Rectangle(-1f, 2f, 3f, 4f).contains(0f, 5f))
		assertTrue(Rectangle(-1f, -2f, 3f, 4f).contains(1f, 1f))
	}

	@Test
	fun containsRectangle() {
		assertTrue(Rectangle(1f, 2f, 3f, 4f).contains(Rectangle(1f, 2f, 3f, 4f)))
		assertFalse(Rectangle(1f, 2f, 3f, 4f).contains(Rectangle(1f, 2f, 3f, 5f)))
		assertFalse(Rectangle(1f, 2f, 3f, 4f).contains(Rectangle(1f, 2f, 4f, 4f)))
		assertTrue(Rectangle(1f, 2f, 3f, 4f).contains(Rectangle(1f, 2f, 2f, 3f)))
	}
}

@Serializable
private data class Foo648(val rect: RectangleRo)