package com.example.activitytracker

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import com.github.mikephil.charting.charts.LineChart

class MainActivity : AppCompatActivity(){
    private lateinit var usernameField: TextView
    private lateinit var dataPanel: Button
    private lateinit var summaryPanel: Button

    private lateinit var compoundChart: LineChart
    private lateinit var pauseButton: Button
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var staticButton: Button
    private lateinit var walkButton: Button
    private lateinit var runButton: Button
    private lateinit var jumpButton: Button
    private lateinit var stairUpButton: Button
    private lateinit var stairDownButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usernameField = findViewById(R.id.user_name)
        dataPanel = findViewById(R.id.data_button)
        summaryPanel = findViewById(R.id.progress_button)
        compoundChart = findViewById(R.id.compound_chart)
        pauseButton = findViewById(R.id.pause_button)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        staticButton = findViewById(R.id.nothing_button)
        walkButton = findViewById(R.id.walk_button)
        runButton = findViewById(R.id.run_button)
        jumpButton = findViewById(R.id.jump_button)
        stairUpButton = findViewById(R.id.stair_up_button)
        stairDownButton = findViewById(R.id.stair_down_button)

        dataPanel.isEnabled = false
        dataPanel.setTextColor(Color.GREEN)

        summaryPanel.setOnClickListener {
            val intent = Intent(this@MainActivity, SummaryViewer::class.java) //
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }

    }
}