package com.gexetr.plinthproductphotos.data.coach

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import com.gexetr.plinthproductphotos.domain.coach.LevelGuide
import kotlin.math.atan2

data class CoachLevelState(
    val available: Boolean,
    val degreesFromLevel: Double? = null,
) {
    val isLevel: Boolean?
        get() = degreesFromLevel?.let { LevelGuide.isLevel(it) }
}

class CoachLevelSensor(
    context: Context,
    private val displayRotation: () -> Int,
    private val onStateChanged: (CoachLevelState) -> Unit,
) : SensorEventListener {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    fun start() {
        if (gravitySensor == null) {
            onStateChanged(CoachLevelState(available = false))
            return
        }
        val registered = manager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI)
        if (!registered) onStateChanged(CoachLevelState(available = false))
    }

    fun stop() {
        manager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 2) return
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val (screenX, screenY) = when (displayRotation()) {
            Surface.ROTATION_90 -> y to -x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> -y to x
            else -> x to y
        }
        val angle = Math.toDegrees(atan2(screenX, screenY))
        onStateChanged(CoachLevelState(available = true, degreesFromLevel = angle))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
