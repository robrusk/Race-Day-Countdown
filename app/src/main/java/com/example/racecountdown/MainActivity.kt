package com.example.racecountdown

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var countdownPreview: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var previewRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(CountdownDataType.PREFS_NAME, Context.MODE_PRIVATE)

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val titleText = TextView(this).apply {
            text = "Race Day Countdown"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(titleText)

        val instructionsText = TextView(this).apply {
            text = "Set your race start time below. The countdown will " +
                    "appear as a data field on your ride screen.\n\n" +
                    "Alerts fire at 10 min, 5 min, and 1 min with " +
                    "beep + flash. Field disappears at zero.\n\n" +
                    "Survives power cycles!"
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(instructionsText)

        statusText = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }
        layout.addView(statusText)

        countdownPreview = TextView(this).apply {
            textSize = 32f
            gravity = Gravity.CENTER
            setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            setPadding(0, 10, 0, 30)
        }
        layout.addView(countdownPreview)

        val setTimeButton = Button(this).apply {
            text = "Set Race Time"
            textSize = 18f
            setPadding(20, 20, 20, 20)
            setOnClickListener {
                showDateTimePicker(prefs)
            }
        }
        layout.addView(setTimeButton)

        val spacer = TextView(this).apply {
            setPadding(0, 20, 0, 0)
        }
        layout.addView(spacer)

        val clearButton = Button(this).apply {
            text = "Clear Race Time"
            textSize = 16f
            setPadding(20, 20, 20, 20)
            setOnClickListener {
                prefs.edit().putLong(CountdownDataType.KEY_RACE_TIME, 0L).apply()
                updateStatus(prefs)
            }
        }
        layout.addView(clearButton)

        scrollView.addView(layout)
        setContentView(scrollView)

        updateStatus(prefs)
        startPreviewTimer(prefs)
    }

    override fun onDestroy() {
        super.onDestroy()
        previewRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun showDateTimePicker(prefs: android.content.SharedPreferences) {
        val calendar = Calendar.getInstance()

        val existing = prefs.getLong(CountdownDataType.KEY_RACE_TIME, 0L)
        if (existing > 0) {
            calendar.timeInMillis = existing
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val timePicker = TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val raceCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val raceMillis = raceCalendar.timeInMillis
                        prefs.edit().putLong(CountdownDataType.KEY_RACE_TIME, raceMillis).apply()

                        Log.d("RaceCountdown", "Race time set: ${Date(raceMillis)}")
                        updateStatus(prefs)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                )
                timePicker.setTitle("Set Race Start Time")
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.setTitle("Set Race Date")
        datePicker.show()
    }

    private fun updateStatus(prefs: android.content.SharedPreferences) {
        val raceMillis = prefs.getLong(CountdownDataType.KEY_RACE_TIME, 0L)

        if (raceMillis == 0L) {
            statusText.text = "No race time set"
            countdownPreview.text = "--:--"
        } else {
            val dateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
            statusText.text = "Race: ${dateFormat.format(Date(raceMillis))}"
        }
    }

    private fun startPreviewTimer(prefs: android.content.SharedPreferences) {
        previewRunnable = object : Runnable {
            override fun run() {
                val raceMillis = prefs.getLong(CountdownDataType.KEY_RACE_TIME, 0L)

                if (raceMillis == 0L) {
                    countdownPreview.text = "--:--"
                } else {
                    val remaining = raceMillis - System.currentTimeMillis()

                    if (remaining <= 0) {
                        countdownPreview.text = "RACE ON!"
                    } else {
                        val totalSeconds = (remaining / 1000).toInt()
                        val hours = totalSeconds / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60

                        countdownPreview.text = if (hours > 0) {
                            String.format("%d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format("%02d:%02d", minutes, seconds)
                        }
                    }
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(previewRunnable!!)
    }
}
