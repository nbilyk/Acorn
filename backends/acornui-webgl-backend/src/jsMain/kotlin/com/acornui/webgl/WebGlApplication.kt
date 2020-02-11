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

package com.acornui.webgl

import com.acornui.*
import com.acornui.asset.Loaders
import com.acornui.component.HtmlComponent
import com.acornui.component.Stage
import com.acornui.di.Context
import com.acornui.di.dKey
import com.acornui.error.stack
import com.acornui.file.FileIoManager
import com.acornui.focus.FakeFocusMouse
import com.acornui.focus.FocusManager
import com.acornui.focus.FocusManagerImpl
import com.acornui.gl.core.CachedGl20
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.Gl20CachedImpl
import com.acornui.graphic.RgbData
import com.acornui.graphic.Texture
import com.acornui.graphic.Window
import com.acornui.io.Bandwidth
import com.acornui.io.Loader
import com.acornui.io.ProgressReporter
import com.acornui.io.UrlRequestData
import com.acornui.js.BrowserApplicationBase
import com.acornui.js.file.JsFileIoManager
import com.acornui.js.html.JsHtmlComponent
import com.acornui.js.html.WebGl
import com.acornui.js.input.JsClickDispatcher
import com.acornui.logging.Log
import com.acornui.system.userInfo
import kotlinx.coroutines.Job
import org.khronos.webgl.WebGLContextAttributes
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.clear
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * An Acorn UI application for browser-based web gl.
 * @author nbilyk
 */
@Suppress("unused")
open class WebGlApplication(mainContext: MainContext, private val rootId: String) : BrowserApplicationBase(mainContext) {

	private val rootElement: HTMLElement by lazy {
		(document.getElementById(rootId) as HTMLElement?) ?: throw Exception("The root element with id $rootId could not be found.")
	}

	override val canvasTask by task(CANVAS) {
		val root = rootElement
		root.style.setProperty("-webkit-tap-highlight-color", "rgba(0,0,0,0)")
		root.clear()
		val canvas = document.createElement("canvas") as HTMLCanvasElement
		canvas.style.setProperty("-webkit-tap-highlight-color", "rgba(0,0,0,0)")
		canvas.style.apply {
			width = "100%"
			height = "100%"
			position = "absolute"
		}
		root.appendChild(canvas)
		canvas
	}

	protected open val glTask by task(CachedGl20) {
		val glConfig = config().gl
		val attributes = WebGLContextAttributes()
		attributes.alpha = glConfig.alpha
		attributes.antialias = glConfig.antialias
		attributes.depth = glConfig.depth
		attributes.stencil = glConfig.stencil
		attributes.premultipliedAlpha = false
		attributes.preserveDrawingBuffer = true

		val context = WebGl.getContext(get(CANVAS), attributes)
				?: throw Exception("Browser does not support WebGL") // TODO: Make this a better UX
		Gl20CachedImpl(if (glDebug) WebGl20Debug(context) else WebGl20(context))
	}

	override val windowTask by task(Window) {
		WebGlWindowImpl(get(CANVAS), config().window, get(Gl20))
	}

	protected open val uncaughtExceptionHandlerTask by task(uncaughtExceptionHandlerKey) {
		val version = get(Version)
		val window = get(Window)
		uncaughtExceptionHandler = {
			val message = it.stack + "\n${version.toVersionString()}"
			Log.error(message)
			if (debug)
				window.alert(message)
		}
		uncaughtExceptionHandler
	}

	override val componentsTask by task(HtmlComponent.FACTORY_KEY) {
		{ JsHtmlComponent(it, rootElement) }
	}

	protected val focusManagerTask by task(FocusManager) {
		// When the focused element changes, make sure the document's active element is the canvas.
		val canvas = get(CANVAS)
		val focusManager = FocusManagerImpl()
		focusManager.focusedChanged.add { old, new ->
			if (new != null) {
				canvas.focus()
			}
		}
		focusManager
	}

	protected open val fileIoManagerTask by task(FileIoManager) {
		JsFileIoManager(rootElement)
	}

	protected open val textureLoader by task(Loaders.textureLoader) {
		val gl = get(CachedGl20)

		object : Loader<Texture> {
			override val defaultInitialTimeEstimate: Duration
				get() = Bandwidth.downBpsInv.seconds * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration): Texture {
				return loadTexture(gl, requestData, progressReporter, initialTimeEstimate)
			}
		}
	}

	protected open val rgbDataLoader by task(Loaders.rgbDataLoader) {
		val gl = get(CachedGl20)

		object : Loader<RgbData> {
			override val defaultInitialTimeEstimate: Duration
				get() = Bandwidth.downBpsInv.seconds * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Duration): RgbData {
				return loadTexture(gl, requestData, progressReporter, initialTimeEstimate).rgbData
			}
		}
	}

	override suspend fun initializeSpecialInteractivity(owner: Context) {
		super.initializeSpecialInteractivity(owner)
		FakeFocusMouse(owner)
		JsClickDispatcher(owner, get(CANVAS))
	}

	companion object {
		protected val CANVAS = dKey<HTMLCanvasElement, HTMLElement>(BrowserApplicationBase.CANVAS)
	}
}

@Suppress("unused")
fun MainContext.webGlApplication(rootId: String, appConfig: AppConfig = AppConfig(), onReady: Stage.() -> Unit): Job =
		WebGlApplication(this, rootId).startAsync(appConfig, onReady)

/**
 * A flag for enabling getError() gl checks.
 * This is separate from [debug] because of the extremely high performance cost.
 */
val glDebug: Boolean by lazy {
	if (!userInfo.isBrowser) false
	else window.location.search.contains(Regex("""[&?]glDebug=(true|1)"""))
}