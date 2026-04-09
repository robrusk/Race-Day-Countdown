package com.ruskracing.racecountdown

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.PerformHardwareAction
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RaceCountdownExtension : KarooExtension("race-countdown", "1") {

    override fun onCreate() {
        super.onCreate()
        Log.d("RaceCountdown", "RACEEXT onCreate called")
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        Log.d("RaceCountdown", "RACEEXT onStartCommand called")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d("RaceCountdown", "RACEEXT onDestroy called")
        super.onDestroy()
    }

    override val types: List<DataTypeImpl> by lazy {
        listOf(CountdownDataType(extension, applicationContext))
    }
}

class CountdownDataType(
    extension: String,
    private val appContext: Context
) : DataTypeImpl(extension, "race-timer") {

    companion object {
        const val TAG = "RaceCountdown"
        const val PREFS_NAME = "race_countdown_prefs"
        const val KEY_RACE_TIME = "race_start_millis"
        const val ALERT_10_MIN = 10 * 60 * 1000L
        const val ALERT_5_MIN = 5 * 60 * 1000L
        const val ALERT_1_MIN = 1 * 60 * 1000L
    }

    @Volatile
    private var countdownText: String = "--:--"
    @Volatile
    private var textColor: Int = Color.WHITE
    @Volatile
    private var bgColor: Int = Color.TRANSPARENT
    @Volatile
    private var clockMode: Boolean = false

    private var alert10fired = false
    private var alert5fired = false
    private var alert1fired = false
    private var raceOnFired = false

    private val clockFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        Log.d(TAG, "startView called")

        val views = RemoteViews(context.packageName, R.layout.countdown_view)
        views.setTextViewText(R.id.countdown_time, countdownText)
        emitter.updateView(views)

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val updatedViews = RemoteViews(context.packageName, R.layout.countdown_view)

                if (clockMode) {
                    updatedViews.setTextViewText(R.id.countdown_time, clockFormat.format(Date()))
                    updatedViews.setTextColor(R.id.countdown_time, Color.WHITE)
                    updatedViews.setInt(R.id.countdown_layout, "setBackgroundColor", Color.TRANSPARENT)
                } else {
                    updatedViews.setTextViewText(R.id.countdown_time, countdownText)
                    updatedViews.setTextColor(R.id.countdown_time, textColor)
                    updatedViews.setInt(R.id.countdown_layout, "setBackgroundColor", bgColor)
                }

                emitter.updateView(updatedViews)
                handler.postDelayed(this, 500)
            }
        }
        handler.postDelayed(runnable, 500)

        emitter.setCancellable {
            handler.removeCallbacks(runnable)
        }
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        Log.d(TAG, "STARTING COUNTDOWN STREAM")

        val karooSystem = KarooSystemService(appContext)
        var karooConnected = false
        karooSystem.connect { connected ->
            karooConnected = connected
            Log.d(TAG, "BEEP KarooSystem connected: $connected")
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val handler = Handler(Looper.getMainLooper())

        alert10fired = false
        alert5fired = false
        alert1fired = false
        raceOnFired = false
        clockMode = false

        val tickRunnable = object : Runnable {
            override fun run() {
                if (clockMode) {
                    emitter.onNext(StreamState.Streaming(
                        DataPoint(dataTypeId = "race-timer", values = mapOf("value" to 0.0))
                    ))
                    handler.postDelayed(this, 1000)
                    return
                }

                val raceStartMillis = prefs.getLong(KEY_RACE_TIME, 0L)

                if (raceStartMillis == 0L) {
                    countdownText = clockFormat.format(Date())
                    textColor = Color.WHITE
                    bgColor = Color.TRANSPARENT
                    emitter.onNext(StreamState.Streaming(
                        DataPoint(dataTypeId = "race-timer", values = mapOf("value" to -1.0))
                    ))
                    handler.postDelayed(this, 1000)
                    return
                }

                val now = System.currentTimeMillis()
                val remaining = raceStartMillis - now

                if (remaining <= 0) {
                    if (!raceOnFired) {
                        raceOnFired = true
                        Log.d(TAG, "RACE ON! Countdown complete.")

                        countdownText = "RACE ON!"
                        textColor = Color.BLACK
                        bgColor = Color.WHITE

                        if (karooConnected) {
                            // Auto-start ride if enabled (default: ON)
                            val autoStart = prefs.getBoolean("auto_start_ride", true)
                            if (autoStart) {
                                try {
                                    karooSystem.dispatch(PerformHardwareAction.BottomRightPress)
                                    Log.d(TAG, "Dispatched BottomRightPress - ride started!")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Start ride failed: ${e.message}")
                                }
                            } else {
                                Log.d(TAG, "Auto-start ride disabled by user")
                            }

                            // Fire the go beep
                            try {
                                karooSystem.dispatch(
                                    PlayBeepPattern(
                                        listOf(
                                            PlayBeepPattern.Tone(1200, 150),
                                            PlayBeepPattern.Tone(0, 100),
                                            PlayBeepPattern.Tone(1200, 150),
                                            PlayBeepPattern.Tone(0, 100),
                                            PlayBeepPattern.Tone(1500, 300)
                                        )
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Beep dispatch failed: ${e.message}")
                            }
                        }

                        handler.postDelayed({
                            clockMode = true
                            bgColor = Color.TRANSPARENT
                            textColor = Color.WHITE
                            prefs.edit().putLong(KEY_RACE_TIME, 0L).apply()
                            Log.d(TAG, "Switched to clock mode, race time cleared")
                        }, 3000)
                    }

                    emitter.onNext(StreamState.Streaming(
                        DataPoint(dataTypeId = "race-timer", values = mapOf("value" to 0.0))
                    ))
                    handler.postDelayed(this, 500)
                    return
                }

                bgColor = Color.TRANSPARENT

                val totalSeconds = (remaining / 1000).toInt()
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                countdownText = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }

                textColor = when {
                    remaining <= ALERT_1_MIN -> Color.RED
                    remaining <= ALERT_5_MIN -> Color.YELLOW
                    else -> Color.WHITE
                }

                if (remaining <= ALERT_10_MIN && !alert10fired) {
                    alert10fired = true
                    Log.d(TAG, "10 MINUTE WARNING")
                    fireAlert(karooSystem, karooConnected, 800, "10 MIN")
                }

                if (remaining <= ALERT_5_MIN && !alert5fired) {
                    alert5fired = true
                    Log.d(TAG, "5 MINUTE WARNING")
                    fireAlert(karooSystem, karooConnected, 900, "5 MIN")
                }

                if (remaining <= ALERT_1_MIN && !alert1fired) {
                    alert1fired = true
                    Log.d(TAG, "1 MINUTE WARNING")
                    fireAlert(karooSystem, karooConnected, 1000, "1 MIN")
                }

                emitter.onNext(StreamState.Streaming(
                    DataPoint(dataTypeId = "race-timer", values = mapOf("value" to totalSeconds.toDouble()))
                ))

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(tickRunnable)

        emitter.setCancellable {
            handler.removeCallbacks(tickRunnable)
            try {
                karooSystem.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error: ${e.message}")
            }
        }
    }

    private fun fireAlert(
        karooSystem: KarooSystemService,
        connected: Boolean,
        frequency: Int,
        message: String
    ) {
        if (connected) {
            try {
                karooSystem.dispatch(
                    PlayBeepPattern(
                        listOf(
                            PlayBeepPattern.Tone(frequency, 200),
                            PlayBeepPattern.Tone(0, 150),
                            PlayBeepPattern.Tone(frequency, 200)
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Alert beep failed: ${e.message}")
            }
        }

        val savedBg = bgColor
        val savedTextColor = textColor

        countdownText = message
        textColor = Color.BLACK
        bgColor = Color.WHITE

        Handler(Looper.getMainLooper()).postDelayed({
            if (!clockMode) {
                textColor = savedTextColor
                bgColor = savedBg
            }
        }, 2000)
    }
}
