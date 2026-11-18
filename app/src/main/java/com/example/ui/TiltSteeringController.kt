package com.example.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class TiltSteeringController(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    var tiltSteerValue: Float = 0f
        private set

    fun start() {
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        tiltSteerValue = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // In landscape orientation, tilting phone left/right changes y axis
            val yTilt = event.values[1]
            val deadZone = 0.7f
            val normalized = if (abs(yTilt) < deadZone) {
                0f
            } else {
                ((-yTilt) / 5.2f).coerceIn(-1f, 1f)
            }
            tiltSteerValue = normalized
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
