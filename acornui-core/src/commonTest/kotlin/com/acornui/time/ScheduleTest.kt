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

import com.acornui.test.assertClose
import com.acornui.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.MonoClock
import kotlin.time.seconds

class ScheduleTest {

	// TODO: CI for Mac has a pretty wide variance
	@Ignore
	@Test
	fun scheduleTest() = runTest(4.seconds) {
		var isDone = false
		val mark = MonoClock.markNow()
		schedule(2.seconds) {
			assertClose(2.0, mark.elapsedNow().inSeconds, 0.1)
			isDone = true
		}
//		loopFrames { !isDone }
	}
}