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

package com.acornui.time

import kotlin.time.Duration

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <T> Iterable<T>.sumByDuration(selector: (T) -> Duration): Duration =
	fold(Duration.ZERO) { acc, e -> acc + selector(e) }

/**
 * Returns the sum of this list of durations.
 */
fun Iterable<Duration>.sum(): Duration =
	fold(Duration.ZERO) { acc, duration -> acc + duration }