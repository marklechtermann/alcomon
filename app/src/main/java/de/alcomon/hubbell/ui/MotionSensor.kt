package de.alcomon.hubbell.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Liefert Beschleunigungswerte (x, y, z, dt) nur, solange [start] aktiv ist – schont den Akku. */
class MotionSensor(context: Context, private val onSample: (Float, Float, Float, Float) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var running = false
    private var lastNanos = 0L

    val available: Boolean get() = sensor != null

    fun start() {
        if (running || sensor == null) return
        running = true
        lastNanos = 0L
        manager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        running = false
        manager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val dt = if (lastNanos == 0L) 0.02f else ((event.timestamp - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.2f)
        lastNanos = event.timestamp
        onSample(event.values[0], event.values[1], event.values[2], dt)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
