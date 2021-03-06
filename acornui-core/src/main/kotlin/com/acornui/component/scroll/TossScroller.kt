///*
// * Copyright 2019 Poly Forest, LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//@file:Suppress("UNUSED_ANONYMOUS_PARAMETER", "unused")
//
//package com.acornui.component.scroll
//
//import com.acornui.Disposable
//import com.acornui.collection.poll
//import com.acornui.component.UiComponent
//import com.acornui.component.createOrReuseAttachment
//import com.acornui.di.ContextImpl
//
//import com.acornui.input.EventType
//import com.acornui.input.interaction.*
//import com.acornui.math.*
//import com.acornui.math.clamp
//import com.acornui.signal.StoppableSignal
//import com.acornui.signal.StoppableSignalImpl
//import com.acornui.time.nowMs
//import com.acornui.time.tick
//import kotlin.time.Duration
//import kotlin.time.seconds
//
///**
// * A toss scroller lets you grab a target component, and update [ScrollModelRo] objects by dragging it.
// */
//class TossScroller(
//		val target: UiComponent,
//
//		/**
//		 * The duration to slow down after a toss.
//		 */
//		var slowTime: Duration = DEFAULT_SLOW_TIME,
//
//		/**
//		 * The easing to slow down after a toss.
//		 */
//		val slowEase: Interpolation = DEFAULT_SLOW_EASE
//) : ContextImpl(target) {
//
//	private val dragAttachment = DragAttachment(target, minTossDistance)
//
//	private val _tossStart = own(StoppableSignalImpl<DragEvent>())
//
//	/**
//	 *  Dispatched when the toss has begun.
//	 */
//	val tossStart: StoppableSignal<DragEventRo> = _tossStart
//
//	private val _toss = own(StoppableSignalImpl<DragEvent>())
//
//	/**
//	 *  Dispatched every frame the toss is being updated.
//	 */
//	val toss: StoppableSignal<DragEventRo> = _toss
//
//	private val _tossEnd = own(StoppableSignalImpl<DragEvent>())
//
//	/**
//	 * Dispatched when the toss has ended.
//	 */
//	val tossEnd: StoppableSignal<DragEventRo> = _tossEnd
//
//	/**
//	 * The velocity when the drag ended.
//	 */
//	private val velocityStart = vec2()
//
//	/**
//	 * The 0-1 range of the toss slow tween.
//	 */
//	private var slowAlpha = 0.0
//
//	private val velocityCurrent = vec2()
//
//	/**
//	 * The current velocity of the toss in dp per second.
//	 */
//	val velocity: Vector2Ro = velocityCurrent
//
//	/**
//	 * Returns true if the user is currently interacting or if there is still momentum.
//	 * @see isDragging
//	 * @see hasVelocity
//	 */
//	val userIsActive: Boolean
//		get() = dragAttachment.userIsActive
//
//	/**
//	 * Returns true if the user is currently dragging (the drag has surpassed the drag affordance,
//	 * and the user has not yet released.)
//	 */
//	val isDragging: Boolean
//		get() = dragAttachment.isDragging
//
//	/**
//	 * If true, the touchMove event will have default prevented.
//	 * This will prevent drag scrolling on mobile.
//	 */
//	var preventDefaultOnTouchMove: Boolean
//		get() = dragAttachment.preventDefaultOnTouchMove
//		set(value) {
//			dragAttachment.preventDefaultOnTouchMove = value
//		}
//
//	/**
//	 * Returns true if the toss scrolling currently has momentum.
//	 * @see stop
//	 */
//	val hasVelocity: Boolean
//		get() = _timer != null
//
//	private val event = DragEvent()
//	private val startPosition = vec2()
//	private val position = vec2()
//
//	private val historyPoints = ArrayList<Vector2>()
//	private val historyTimes = ArrayList<Long>()
//
//	private var _timer: Disposable? = null
//	private var clickPreventer = 0
//
//	private val diff = vec2()
//
//	private fun pushHistory() {
//		historyPoints.add(position.copy())
//		historyTimes.add(nowMs())
//		if (historyPoints.size > MAX_HISTORY) {
//			historyPoints.poll()
//			historyTimes.poll()
//		}
//	}
//
//	private fun startEnterFrame() {
//		if (_timer == null) {
//			_timer = tick { dT ->
//				if (dragAttachment.isDragging) {
//					// History is also added in an enter frame instead of the drag handler so that if the user stops dragging, the history reflects that.
//					pushHistory()
//				} else {
//					slowAlpha += dT / slowTime.inSeconds.toDouble()
//					val slowAlphaEased = slowEase.apply(clamp(slowAlpha, 0.0, 1.0))
//					if (clickPreventer > 0) clickPreventer--
//					velocityCurrent.set(velocityStart).lerp(0.0, 0.0, slowAlphaEased)
//					position.add(velocityCurrent.x * dT, velocityCurrent.y * dT)
//					dispatchDragEvent(TOSS, _toss)
//					if (slowAlpha >= 1.0) {
//						stop()
//					}
//				}
//			}
//		}
//	}
//
//	private fun dispatchDragEvent(type: EventType<DragEvent>, signal: StoppableSignalImpl<DragEvent>) {
//		event.clear()
//		event.target = target
//		event.currentTarget = target
//		event.type = type
//		event.startPosition.set(startPosition)
//		event.position.set(position)
//		signal.dispatch(event)
//	}
//
//	private fun clearHistory() {
//		historyPoints.clear()
//		historyTimes.clear()
//	}
//
//	init {
//		dragAttachment.dragStart.add(::dragStartHandler)
//		dragAttachment.drag.add(::dragHandler)
//		dragAttachment.dragEnd.add(::dragEndHandler)
//		target.click(isCapture = true).add(::clickHandler)
//	}
//
//	private fun dragStartHandler(event: DragEventRo) {
//		stop()
//		startPosition.set(event.startPosition)
//		position.set(event.position)
//
//		clickPreventer = 5
//		clearHistory()
//		pushHistory()
//		startEnterFrame()
//		dispatchDragEvent(TOSS_START, _tossStart)
//	}
//
//	private fun dragHandler(event: DragEventRo) {
//		position.set(event.position)
//		dispatchDragEvent(TOSS, _toss)
//	}
//
//	private fun dragEndHandler(event: DragEventRo) {
//		position.set(event.position)
//		pushHistory()
//		// Calculate the velocity.
//		if (historyPoints.size >= 2) {
//			diff.set(historyPoints.last()).sub(historyPoints.first())
//			val time = (historyTimes.last() - historyTimes.first()) * 0.001
//			velocityCurrent.set(diff.x / time, diff.y / time)
//			velocityStart.set(velocityCurrent)
//			slowAlpha = 0.0
//		}
//		clearHistory()
//	}
//
//	private fun clickHandler(event: ClickEventRo) {
//		if (clickPreventer > 0) {
//			event.propagation.stopImmediatePropagation()
//		}
//	}
//
//	/**
//	 * Enables or disables this toss scroller. If this is set to false in the middle of a toss, the toss will
//	 * still finish as if the drag were immediately released. Use [stop] to halt the current velocity.
//	 */
//	var enabled: Boolean
//		get() = dragAttachment.enabled
//		set(value) {
//			dragAttachment.enabled = value
//		}
//
//	fun stop() {
//		if (_timer != null) {
//			dispatchDragEvent(TOSS_END, _tossEnd)
//			clickPreventer = 0
//			velocityCurrent.clear()
//			_timer?.dispose()
//			_timer = null
//			event.clear()
//		}
//	}
//
//	override fun dispose() {
//		super.dispose()
//		stop()
//		target.click(isCapture = true).remove(::clickHandler)
//	}
//
//	companion object {
//
//		val TOSS_START = EventType<DragEvent>("tossStart")
//		val TOSS = EventType<DragEvent>("toss")
//		val TOSS_END = EventType<DragEvent>("tossEnd")
//
//		val DEFAULT_SLOW_TIME = 0.8.seconds
//		val DEFAULT_SLOW_EASE = Easing.pow3Out
//
//		private const val MAX_HISTORY = 10
//
//		var minTossDistance: Double = 7.0
//	}
//}
//
///**
// * Converts the current toss difference in dp to whatever unit the scroll models are using.
// */
//typealias PointsToModel = (modelStart: Vector2Ro, diffPoints: Vector2Ro, out: Vector2) -> Unit
//
//open class TossScrollModelBinding(
//		private val tossScroller: TossScroller,
//		private val handler: (diff: Vector2Ro) -> Unit
//) : Disposable {
//
//	constructor(tossScroller: TossScroller, hScrollModel: ScrollModel, vScrollModel: ScrollModel) : this(tossScroller, {
//		hScrollModel.value -= it.x
//		vScrollModel.value -= it.y
//	})
//
//	private val lastPositionLocal = vec2()
//	private val diff = vec2()
//
//	init {
//		tossScroller.tossStart.add(::tossStartHandler)
//		tossScroller.toss.add(::tossHandler)
//	}
//
//	private fun tossStartHandler(event: DragEventRo) {
//		lastPositionLocal.set(event.positionLocal)
//	}
//
//	private fun tossHandler(event: DragEventRo) {
//		diff.set(event.positionLocal).sub(lastPositionLocal)
//		localToModel(diff)
//		handler(diff)
//		lastPositionLocal.set(event.positionLocal)
//	}
//
//	/**
//	 * Converts points, in global coordinate space, to model.
//	 */
//	protected open fun localToModel(diffPoints: Vector2) {
//	}
//
//
//	override fun dispose() {
//		tossScroller.tossStart.remove(::tossStartHandler)
//		tossScroller.toss.remove(::tossHandler)
//	}
//}
//
//fun UiComponent.enableTossScrolling(slowTime: Duration = TossScroller.DEFAULT_SLOW_TIME, slowEase: Interpolation = TossScroller.DEFAULT_SLOW_EASE): TossScroller {
//	return createOrReuseAttachment(TossScroller) { TossScroller(this, slowTime, slowEase) }
//}
//
//fun UiComponent.disableTossScrolling() {
//	removeAttachment<TossScroller>(TossScroller)?.dispose()
//}
