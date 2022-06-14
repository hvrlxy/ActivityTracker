package com.example.activitytracker

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.database.*
import java.time.Instant.*
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity(), SensorEventListener {
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

    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    private var thread: Thread? = null
    private var plotData = true

    private var currentActivity : String? = null
    private var isRecorded = false

    private lateinit var db :FirebaseDatabase
    private lateinit var ref : DatabaseReference

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

        db = FirebaseDatabase.getInstance()
        db.reference.root.child("data").setValue(null)
        ref = db.getReference("data")


        summaryPanel.setOnClickListener {
            val intent = Intent(this@MainActivity, SummaryViewer::class.java) //
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
        }

        setUpSensor()

        // allow users to interact with the chart
        compoundChart.setTouchEnabled(true)
        compoundChart.isDragEnabled = true
        compoundChart.setScaleEnabled(true)
        compoundChart.setDrawGridBackground(false)

        // if disabled, scaling can be done on x- and y-axis separately
        compoundChart.setPinchZoom(true)

        // set an alternative background color
        compoundChart.setBackgroundColor(Color.BLACK)

        //set up the data stream
        val dataStream : LineData = LineData()
        dataStream.setValueTextColor(Color.WHITE)
        compoundChart.data = dataStream

        //set up the legend
        val chartLegend : Legend = compoundChart.legend
        chartLegend.form = Legend.LegendForm.LINE
        chartLegend.textColor = Color.WHITE

        //set up the x-axis
        val xs = compoundChart.xAxis
        xs.textColor = Color.WHITE
        xs.setDrawGridLines(true)
        xs.setAvoidFirstLastClipping(true)
        xs.isEnabled = true

        //set up the y-axis (there can be 2 y axis on the left and right, we only need the left one
        val ys = compoundChart.axisLeft
        ys.textColor = Color.WHITE
        ys.setDrawGridLines(true)
        ys.axisMaximum = 10f
        ys.axisMinimum = 0f

        //disabled the right y-axis
        compoundChart.axisRight.isEnabled = false
        compoundChart.setDrawBorders(false)

        if (currentActivity == null){ isRecorded = false }

        startButton.setOnClickListener {
            isRecorded = true
            startButton.isEnabled = false
            stopButton.isEnabled = true
            pauseButton.isEnabled = true
        }

        stopButton.setOnClickListener {
            isRecorded = false
            startButton.isEnabled = true
            stopButton.isEnabled = false
            pauseButton.isEnabled = true
        }

        pauseButton.setOnClickListener {
            isRecorded = false
            startButton.isEnabled = true
            stopButton.isEnabled = true
            pauseButton.isEnabled = false
        }

        staticButton.setOnClickListener {
            resetActionButton()
            currentActivity = "static"
            staticButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        walkButton.setOnClickListener {
            resetActionButton()
            currentActivity = "walk"
            walkButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        runButton.setOnClickListener {
            resetActionButton()
            currentActivity = "run"
            runButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        jumpButton.setOnClickListener {
            resetActionButton()
            currentActivity = "jump"
            jumpButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        stairUpButton.setOnClickListener {
            resetActionButton()
            currentActivity = "up"
            stairUpButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        stairDownButton.setOnClickListener {
            resetActionButton()
            currentActivity = "down"
            stairDownButton.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        feedMultiple()
    }

    private fun resetActionButton(){
        staticButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEC72"))
        walkButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEC72"))
        runButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEC72"))
        jumpButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEC72"))
        stairUpButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEC72"))
        stairDownButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEC72"))
    }

    private fun feedMultiple() {
        thread?.interrupt()
        thread = Thread {
            while (true) {
                plotData = true
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        thread!!.start()
    }

    private fun setUpSensor(){
        // initialize the sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // declare the type of sensor we want to use
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun createSet(color: Int, label: String): LineDataSet {
        val set = LineDataSet(null, label)
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 3f
        set.color = color
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        return set
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        // retrieve the information return by the sensor listener
        if (plotData) {
            plotData = false

            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                // check if the event returned by the sensor is the type of sensor we want
                val x = event.values[0] // left right
                val y = event.values[1] // top down side of the phone
                val z = event.values[2] // side of the screen, positive indicate the phone face up
                if (isRecorded && currentActivity != null) {
                    val dataObject =
                        AccelerometerData(x, y, z, currentActivity!!)
                    addDataToFirebase(dataObject)
                }

                val data = compoundChart.data
                if (data != null){

                    if (data.getDataSetByIndex(0) == null){
                        data.addDataSet(createSet(Color.MAGENTA, "x-axis"))
                    }

                    if (data.getDataSetByIndex(1) == null){
                        data.addDataSet(createSet(Color.GREEN, "y-axis"))
                    }

                    if (data.getDataSetByIndex(2) == null){
                        data.addDataSet(createSet(Color.YELLOW, "z-axis"))
                    }


                    val xSet : ILineDataSet = data.getDataSetByIndex(0)
                    data.addEntry( Entry(xSet.entryCount.toFloat(), x + 5), 0)

                    val ySet : ILineDataSet = data.getDataSetByIndex(1)
                    data.addEntry( Entry(ySet.entryCount.toFloat(), y), 1)

                    val zSet : ILineDataSet = data.getDataSetByIndex(2)
                    data.addEntry( Entry(zSet.entryCount.toFloat(), z), 2)
                    data.notifyDataChanged()

                    compoundChart.notifyDataSetChanged()
                    // set the maximum visible latest entry
                    compoundChart.setVisibleXRangeMaximum(150F)

                    // move the chart to the newest entries
                    compoundChart.moveViewToX(data.entryCount.toFloat())
                }
            }
        }
    }

    private fun addDataToFirebase(dataObject: AccelerometerData){
        ref.addValueEventListener(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(snapshot: DataSnapshot) {
                // inside the method of on Data change we are setting
                // our object class to our database reference.
                // data base reference will sends data to firebase.
                val dateString = DateTimeFormatter.ISO_INSTANT.format(now()).replace('.', ',')
                ref.child(
                    dateString
                ).setValue(dataObject)
            }

            override fun onCancelled(error: DatabaseError) {
                // if the data is not added or it is cancelled then
                // we are displaying a failure toast message.
                Toast.makeText(this@MainActivity, "Fail to add data $error", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //do nothing
        return
    }

    override fun onDestroy() {
        //unregister the sensor when we close the app to prevent any memory leak
        sensorManager.unregisterListener(this)
        thread?.interrupt()
        super.onDestroy()
    }

    override fun onResume() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        thread?.interrupt()
        sensorManager.unregisterListener(this)
    }
}