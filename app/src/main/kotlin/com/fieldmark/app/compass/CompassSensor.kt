package com.fieldmark.app.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2

class CompassRepository(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnet: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotMatrix = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)
    private val accelValues = FloatArray(3)
    private val magValues = FloatArray(3)
    private var hasAccel = false
    private var hasMag = false

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading
    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch
    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    val azimuthRad = orientation[0]
                    val pitchRad = orientation[1]
                    val rollRad = orientation[2]
                    val azimuthDeg = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
                    val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
                    val rollDeg = Math.toDegrees(rollRad.toDouble()).toFloat()
                    _heading.value = azimuthDeg
                    _pitch.value = pitchDeg
                    _roll.value = rollDeg
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelValues, 0, 3)
                    hasAccel = true
                    computeFromAccelMag()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magValues, 0, 3)
                    hasMag = true
                    computeFromAccelMag()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun computeFromAccelMag() {
        if (!hasAccel || !hasMag || rotationVector != null) return
        if (SensorManager.getRotationMatrix(rotMatrix, null, accelValues, magValues)) {
            SensorManager.getOrientation(rotMatrix, orientation)
            val azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()
            _heading.value = azimuthDeg
            _pitch.value = pitchDeg
            _roll.value = rollDeg
        }
    }

    fun start() {
        rotationVector?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            ?: run {
                accel?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
                magnet?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}

@Composable
fun rememberCompass(context: android.content.Context): CompassRepository {
    val repo = remember(context) { CompassRepository(context) }
    DisposableEffect(repo) {
        repo.start()
        onDispose { repo.stop() }
    }
    return repo
}
