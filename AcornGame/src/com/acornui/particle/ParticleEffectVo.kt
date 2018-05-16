/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.particle

import com.acornui.collection.sortedInsertionIndex
import com.acornui.core.UidUtil
import com.acornui.core.graphics.BlendMode
import com.acornui.math.Easing
import com.acornui.math.Interpolation
import com.acornui.math.MathUtils
import com.acornui.serialization.*

data class ParticleEffectVo(

		val emitters: List<ParticleEmitterVo>
) {
	fun createInstance(): ParticleEffectInstanceVo {
		val emitterInstances = ArrayList<ParticleEmitterInstance>(emitters.size)
		for (i in 0..emitters.lastIndex) {
			val emitterInstance = emitters[i].createInstance()
			emitterInstances.add(emitterInstance)
		}
		return ParticleEffectInstanceVo(emitterInstances)
	}
}

data class ParticleEmitterVo(

		val id: String = UidUtil.createUid(),

		val name: String,

		val enabled: Boolean,

		/**
		 * If false, this emitter will not loop after the total duration.
		 */
		val loops: Boolean,

		/**
		 * Represents when and how long this emitter will be active.
		 */
		val duration: EmitterDuration,

		/**
		 * The maximum number of particles to create.
		 */
		val count: Int,

		/**
		 * The rate of emissions, in particles per second.
		 */
		val emissionRate: PropertyTimeline,

		/**
		 * Calculates the life of a newly created particle.
		 */
		val particleLifeExpectancy: PropertyTimeline,

		val spawnLocation: ParticleSpawn,

		val blendMode: BlendMode,

		val premultipliedAlpha: Boolean,

		val imageEntries: List<ParticleImageEntry>,

		val propertyTimelines: List<PropertyTimeline>
) {
	fun createInstance(): ParticleEmitterInstance {
		return ParticleEmitterInstance(this)
	}
}

data class EmitterDuration(

		/**
		 * The minimum number of seconds this emitter will create particles.
		 */
		val durationMin: Float,

		/**
		 * The maximum number of seconds this emitter will create particles.
		 */
		val durationMax: Float,

		/**
		 * The easing to apply when calculating the duration between [durationMin] and [durationMax]
		 */
		val durationEasing: Interpolation,

		/**
		 * The time, in seconds, before the emitter begins.
		 */
		val delayBefore: Float,

		/**
		 * The time, in seconds, after completion before restarting.
		 */
		val delayAfter: Float
) {
	fun calculateDuration(): Float {
		return durationEasing.apply(MathUtils.random()) * (durationMax - durationMin) + durationMin
	}
}

data class ParticleImageEntry(
		val time: Float,
		val path: String
)

data class PropertyTimeline(

		val name: String,

		/**
		 * If true, the final value will not be the high value, but the high + low
		 */
		val relative: Boolean,

		val lowMin: Float,
		val lowMax: Float,

		/**
		 * When selecting a random value between [lowMin] and [lowMax], this easing is applied in order to
		 * allow control over random distribution.
		 */
		val lowEasing: Interpolation,

		val highMin: Float,
		val highMax: Float,

		/**
		 * When selecting a random value between [highMin] and [highMax], this easing is applied in order to
		 * allow control over random distribution.
		 */
		val highEasing: Interpolation,

		/**
		 * A list of control points for interpolated values. If this is empty, the low value will always be used.
		 */
		val timeline: List<TimelineValue>
) {

	fun reset(target: PropertyValue) {
		target.low = (lowMax - lowMin) * lowEasing.apply(MathUtils.random())
		target.high = (highMax - highMin) * highEasing.apply(MathUtils.random())
		if (relative) target.high += target.low
		target.diff = target.high - target.low
		target.current = target.high
		apply(target,  0f)
	}

	private val comparator: (Float, TimelineValue) -> Int = {
		time, element ->
		time.compareTo(element.time)
	}

	fun apply(target: PropertyValue, alpha: Float) {
		val n = timeline.size
		if (n == 0) return
		val timelineIndex = timeline.sortedInsertionIndex(alpha, matchForwards = true, comparator = comparator) - 1
		val timeA: Float
		val valueA: Float
		if (timelineIndex < 0) {
			timeA = 0f
			valueA = timeline.first().value
		} else {
			val timelineEntry = timeline[timelineIndex]
			timeA = timelineEntry.time
			valueA = timelineEntry.value
		}

		val timeB: Float
		val valueB: Float
		if (timelineIndex >= n - 1) {
			timeB = 1f
			valueB = timeline.last().value
		} else {
			val timelineEntry = timeline[timelineIndex + 1]
			timeB = timelineEntry.time
			valueB = timelineEntry.value
		}
		val valueAlpha = (alpha - timeA) / (timeB - timeA)
		val valueValue = (valueB - valueA) * valueAlpha + valueA
		target.current = valueValue * target.diff + target.low
	}
}

/**
 * Represents the range value at the given time.
 */
data class TimelineValue(

		/**
		 * A value of 0f - 1f indicating the current progress of this particle where the [value] will be used.
		 */
		val time: Float,

		/**
		 * A value of 0f - 1f indicating the interpolation of the low to high value.
		 */
		val value: Float
) : Comparable<TimelineValue> {

	override fun compareTo(other: TimelineValue): Int {
		return time.compareTo(other.time)
	}
}

class PropertyValue(
		var low: Float = 0f,
		var high: Float = 0f,
		var diff: Float = 0f,
		var current: Float = 0f
)

object ParticleEffectSerializer : From<ParticleEffectVo>, To<ParticleEffectVo> {

	override fun read(reader: Reader): ParticleEffectVo {
		return ParticleEffectVo(reader.arrayList("emitters", ParticleEmitterSerializer)!!)
	}

	override fun ParticleEffectVo.write(writer: Writer) {
		writer.array("emitters", emitters, ParticleEmitterSerializer)
	}
}

object ParticleEmitterSerializer : From<ParticleEmitterVo>, To<ParticleEmitterVo> {

	override fun read(reader: Reader): ParticleEmitterVo {
		return ParticleEmitterVo(
				id = reader.string("id")!!,
				name = reader.string("name")!!,
				enabled = reader.bool("enabled") ?: true,
				loops = reader.bool("loops") ?: true,
				duration = reader.obj("duration", EmitterDurationSerializer)!!,
				count = reader.int("count")!!,
				emissionRate = reader.obj("emissionRate", PropertyTimelineSerializer)!!,
				particleLifeExpectancy = reader.obj("particleLifeExpectancy", PropertyTimelineSerializer)!!,
				spawnLocation = reader.obj("spawnLocation", ParticleSpawnSerializer)!!,
				blendMode = BlendMode.fromStr(reader.string("blendMode") ?: "normal")!!,
				premultipliedAlpha = reader.bool("premultipliedAlpha") ?: false,
				imageEntries = reader.arrayList("imageEntries", ParticleImageEntrySerializer)!!,
				propertyTimelines = reader.arrayList("propertyTimelines", PropertyTimelineSerializer)!!
		)
	}

	override fun ParticleEmitterVo.write(writer: Writer) {
		writer.string("id", id)
		writer.string("blendMode", blendMode.name)
		writer.int("count", count)
		writer.obj("duration", duration, EmitterDurationSerializer)
		writer.obj("emissionRate", emissionRate, PropertyTimelineSerializer)
		writer.bool("enabled", enabled)
		writer.array("imageEntries", imageEntries, ParticleImageEntrySerializer)
		writer.bool("loops", loops)
		writer.string("name", name)
		writer.obj("particleLifeExpectancy", particleLifeExpectancy, PropertyTimelineSerializer)
		writer.bool("premultipliedAlpha", premultipliedAlpha)
		writer.array("propertyTimelines", propertyTimelines, PropertyTimelineSerializer)
		writer.obj("spawnLocation", spawnLocation, ParticleSpawnSerializer)
	}
}

object EmitterDurationSerializer : From<EmitterDuration>, To<EmitterDuration> {

	override fun read(reader: Reader): EmitterDuration {
		val easingName = reader.string("durationEasing") ?: "linear"
		return EmitterDuration(
				durationMin = reader.float("durationMin")!!,
				durationMax = reader.float("durationMax")!!,
				durationEasing = Easing.fromString(easingName) ?: throw Exception("Unknown easing '$easingName'"),
				delayBefore =  reader.float("delayBefore") ?: 0f,
				delayAfter =  reader.float("delayAfter") ?: 0f

		)
	}

	override fun EmitterDuration.write(writer: Writer) {
		writer.float("delayAfter", delayAfter)
		writer.float("delayBefore", delayBefore)
		writer.string("durationEasing", Easing.toString(durationEasing))
		writer.float("durationMax", durationMax)
		writer.float("durationMin", durationMin)
	}
}

object PropertyTimelineSerializer : From<PropertyTimeline>, To<PropertyTimeline> {

	override fun read(reader: Reader): PropertyTimeline {
		val lowEasingName = reader.string("lowEasing") ?: "linear"
		val highEasingName = reader.string("highEasing") ?: "linear"

		val timelineFloats = reader.floatArray("timeline") ?: floatArrayOf()
		val timeline = ArrayList<TimelineValue>(timelineFloats.size shr 1)
		for (i in 0..timelineFloats.lastIndex step 2) {
			timeline.add(TimelineValue(timelineFloats[i], timelineFloats[i + 1]))
		}

		return PropertyTimeline(
				name = reader.string("name")!!,
				relative = reader.bool("relative") ?: false,
				lowMin = reader.float("lowMin")!!,
				lowMax = reader.float("lowMax")!!,
				lowEasing = Easing.fromString(lowEasingName) ?: throw Exception("Unknown easing '$lowEasingName'"),
				highMin = reader.float("highMin")!!,
				highMax = reader.float("highMax")!!,
				highEasing = Easing.fromString(highEasingName) ?: throw Exception("Unknown easing '$highEasingName'"),
				timeline = timeline

		)
	}

	override fun PropertyTimeline.write(writer: Writer) {
		writer.string("highEasing", Easing.toString(highEasing))
		writer.float("highMax", highMax)
		writer.float("highMin", highMin)
		writer.string("lowEasing", Easing.toString(lowEasing))
		writer.float("lowMax", lowMax)
		writer.float("lowMin", lowMin)
		writer.string("name", name)
		writer.bool("relative", relative)
		val timelineFloats = FloatArray(timeline.size shl 1)
		for (i in 0..timeline.lastIndex) {
			val j = i shl 1
			val t = timeline[i]
			timelineFloats[j] = t.time
			timelineFloats[j + 1] = t.value
		}
		writer.floatArray("timeline", timelineFloats)
	}
}

object ParticleImageEntrySerializer : From<ParticleImageEntry>, To<ParticleImageEntry> {

	override fun read(reader: Reader): ParticleImageEntry {
		return ParticleImageEntry(
				time = reader.float("time")!!,
				path = reader.string("path")!!
		)
	}

	override fun ParticleImageEntry.write(writer: Writer) {
		writer.string("path", path)
		writer.float("time", time)
	}
}