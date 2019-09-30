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

package com.acornui.observe

import com.acornui.Disposable
import com.acornui.signal.Signal1
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A base class for the [Observable] interface. Contains utility methods for [watched] properties.
 */
abstract class ObservableBase : Observable, Disposable {

	private val _changed = Signal1<ObservableBase>()
	override val changed = _changed.asRo()

	/**
	 * A property that, if changed, will dispatch the [changed] signal.
	 */
	protected fun <T> watched(initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
		override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
			notifyChanged()
		}
	}

	/**
	 * Dispatches the [changed] signal.
	 */
	protected fun notifyChanged() {
		_changed.dispatch(this)
	}

	override fun dispose() {
		_changed.dispose()
	}
}