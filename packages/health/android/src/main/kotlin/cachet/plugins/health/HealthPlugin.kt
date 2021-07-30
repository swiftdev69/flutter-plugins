package cachet.plugins.health

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1111

class HealthPlugin(val activity: Activity, val channel: MethodChannel) : MethodCallHandler, ActivityResultListener, Result {

    private var result: Result? = null
    private var handler: Handler? = null

    private var BODY_FAT_PERCENTAGE = "BODY_FAT_PERCENTAGE"
    private var HEIGHT = "HEIGHT"
    private var WEIGHT = "WEIGHT"
    private var STEPS = "STEPS"
    private var ACTIVE_ENERGY_BURNED = "ACTIVE_ENERGY_BURNED"
    private var HEART_RATE = "HEART_RATE"
    private var BODY_TEMPERATURE = "BODY_TEMPERATURE"
    private var BLOOD_PRESSURE_SYSTOLIC = "BLOOD_PRESSURE_SYSTOLIC"
    private var BLOOD_PRESSURE_DIASTOLIC = "BLOOD_PRESSURE_DIASTOLIC"
    private var BLOOD_OXYGEN = "BLOOD_OXYGEN"
    private var BLOOD_GLUCOSE = "BLOOD_GLUCOSE"
    private var MOVE_MINUTES = "MOVE_MINUTES"
    private var DISTANCE_DELTA = "DISTANCE_DELTA"

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_health")
            val plugin = HealthPlugin(registrar.activity(), channel)
            registrar.addActivityResultListener(plugin)
            channel.setMethodCallHandler(plugin)
            /*channel.setMethodCallHandler { call, result ->
                    val result: Result = MethodResultWrapper(result)
            }*/
        }
    }

    /// DataTypes to register
    private val fitnessOptions = FitnessOptions.builder()
            .addDataType(keyToHealthDataType(BODY_FAT_PERCENTAGE), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(HEIGHT), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(WEIGHT), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(STEPS), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(ACTIVE_ENERGY_BURNED), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(HEART_RATE), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(BODY_TEMPERATURE), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(BLOOD_PRESSURE_SYSTOLIC), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(BLOOD_OXYGEN), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(BLOOD_GLUCOSE), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(MOVE_MINUTES), FitnessOptions.ACCESS_READ)
            .addDataType(keyToHealthDataType(DISTANCE_DELTA), FitnessOptions.ACCESS_READ)
            .build()


    override fun success(p0: Any?) {
        handler?.post(
                Runnable { result?.success(p0) })
    }

    override fun notImplemented() {
        handler?.post(
                Runnable { result?.notImplemented() })
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
        handler?.post(
                Runnable { result?.error(errorCode, errorMessage, errorDetails) })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("FLUTTER_HEALTH", "Access Granted!")
                mResult?.success(true)
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("FLUTTER_HEALTH", "Access Denied!")
                mResult?.success(false);
            }
        }
        return false
    }

    private var mResult: Result? = null

    private fun keyToHealthDataType(type: String): DataType {
        return when (type) {
            BODY_FAT_PERCENTAGE -> DataType.TYPE_BODY_FAT_PERCENTAGE
            HEIGHT -> DataType.TYPE_HEIGHT
            WEIGHT -> DataType.TYPE_WEIGHT
            STEPS -> DataType.TYPE_STEP_COUNT_DELTA
            /* STEPS -> DataType.AGGREGATE_STEP_COUNT_DELTA*/
            ACTIVE_ENERGY_BURNED -> DataType.TYPE_CALORIES_EXPENDED
            HEART_RATE -> DataType.TYPE_HEART_RATE_BPM
            BODY_TEMPERATURE -> HealthDataTypes.TYPE_BODY_TEMPERATURE
            BLOOD_PRESSURE_SYSTOLIC -> HealthDataTypes.TYPE_BLOOD_PRESSURE
            BLOOD_PRESSURE_DIASTOLIC -> HealthDataTypes.TYPE_BLOOD_PRESSURE
            BLOOD_OXYGEN -> HealthDataTypes.TYPE_OXYGEN_SATURATION
            BLOOD_GLUCOSE -> HealthDataTypes.TYPE_BLOOD_GLUCOSE
            MOVE_MINUTES -> DataType.TYPE_MOVE_MINUTES
            DISTANCE_DELTA -> DataType.TYPE_DISTANCE_DELTA
            else -> DataType.TYPE_STEP_COUNT_DELTA
            /*else -> DataType.AGGREGATE_STEP_COUNT_DELTA*/
        }
    }

    private fun getUnit(type: String): Field {
        return when (type) {
            BODY_FAT_PERCENTAGE -> Field.FIELD_PERCENTAGE
            HEIGHT -> Field.FIELD_HEIGHT
            WEIGHT -> Field.FIELD_WEIGHT
            STEPS -> Field.FIELD_STEPS
            ACTIVE_ENERGY_BURNED -> Field.FIELD_CALORIES
            HEART_RATE -> Field.FIELD_BPM
            BODY_TEMPERATURE -> HealthFields.FIELD_BODY_TEMPERATURE
            BLOOD_PRESSURE_SYSTOLIC -> HealthFields.FIELD_BLOOD_PRESSURE_SYSTOLIC
            BLOOD_PRESSURE_DIASTOLIC -> HealthFields.FIELD_BLOOD_PRESSURE_DIASTOLIC
            BLOOD_OXYGEN -> HealthFields.FIELD_OXYGEN_SATURATION
            BLOOD_GLUCOSE -> HealthFields.FIELD_BLOOD_GLUCOSE_LEVEL
            MOVE_MINUTES -> Field.FIELD_DURATION
            DISTANCE_DELTA -> Field.FIELD_DISTANCE
            else -> Field.FIELD_PERCENTAGE
        }
    }

    /// Extracts the (numeric) value from a Health Data Point
    private fun getHealthDataValue(dataPoint: DataPoint, unit: Field): Any {

        // Log.e("TEMP DATA","${dataPoint.dataType} ======> ${dataPoint.getValue(unit)}")
        return try {
            dataPoint.getValue(unit).asFloat()
        } catch (e1: Exception) {
            try {
                dataPoint.getValue(unit).asInt()
            } catch (e2: Exception) {
                try {
                    dataPoint.getValue(unit).asString()
                } catch (e3: Exception) {
                    Log.e("FLUTTER_HEALTH::ERROR", e3.toString())
                }
            }
        }
    }

    fun removeLastNDigits(x: Long, n: Long): Long {
        return (x / Math.pow(10.0, n.toDouble())).toLong()
    }


    /// Called when the "getHealthDataByType" is invoked from Flutter
    private fun getData(call: MethodCall, result: Result) {
        val type = call.argument<String>("dataTypeKey")!!
        var startTimeFromFlutter = call.argument<Long>("startDate")!!
        var endTimeFromFlutter = call.argument<Long>("endDate")!!

        /*  startTimeFromFlutter = removeLastNDigits(startTimeFromFlutter, 3)
          endTimeFromFlutter = removeLastNDigits(endTimeFromFlutter, 3)
  */

        // Look up data type and unit for the type key
        val dataType = keyToHealthDataType(type)
        val unit = getUnit(type)

        var total = 0
        var expendedCalories = 0f

        /// Start a new thread for doing a GoogleFit data lookup
        thread {
            try {

                val fitnessOptions = FitnessOptions.builder().addDataType(dataType).build()
                val googleSignInAccount = GoogleSignIn.getAccountForExtension(activity.applicationContext, fitnessOptions)

                val ESTIMATED_STEP_DELTAS: DataSource = DataSource.Builder()
                        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .setType(DataSource.TYPE_DERIVED)
                        .setStreamName("estimated_steps")
                        .setAppPackageName("com.google.android.gms")
                        .build()

                val newRequest = DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                        .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .bucketByActivitySegment(1, TimeUnit.MILLISECONDS)
                        .setTimeRange(startTimeFromFlutter, endTimeFromFlutter, TimeUnit.MILLISECONDS)
                        .build()

                ///NEW CODE START

                /* val request = DataReadRequest.Builder()
                         .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                         .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                         .bucketByTime(1, TimeUnit.DAYS)
                         .setTimeRange(startTimeFromFlutter, endTimeFromFlutter, TimeUnit.SECONDS)
                         .build()*/



                Fitness.getHistoryClient(activity.applicationContext, googleSignInAccount)
                        .readData(newRequest)
                        .addOnSuccessListener { response ->

                            val newDataList = mutableListOf<Map<String, Any>>()

                            response.buckets.forEach {

                                val dataSetx: List<DataSet> = it.dataSets

                                dataSetx.forEach { dataSet ->
                                    if (dataSet.dataType.name == "com.google.step_count.delta") {

                                        if (dataSet.dataPoints.size > 0) {

                                            //total step
                                            total += dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()

                                            Log.e("STEPS IS", "${dataSet.dataPoints[0].getStartTime(TimeUnit.MILLISECONDS)} And ${dataSet.dataPoints[0].getValue(Field.FIELD_STEPS)} AND ${dataSet.dataPoints[0].getStartTime(TimeUnit.MILLISECONDS)}")
                                        }
                                    }
                                }


                                val dataSets: List<DataSet> = it.dataSets

                                dataSets.forEach { dataSet ->
                                    if (dataSet.dataType.name == "com.google.calories.expended") {

                                        dataSet.dataPoints.forEach { dp ->

                                            if (dp.getEndTime(TimeUnit.MILLISECONDS) > dp.getStartTime(TimeUnit.MILLISECONDS)) {
                                                for (field in dp.dataType.fields) {
                                                    // total calories burned
                                                    expendedCalories += dp.getValue(field).asFloat()
                                                    Log.e("CALOURIE IS", "${dp.getStartTime(TimeUnit.MILLISECONDS)} And ${dp.getValue(field).asFloat()} AND ${dp.getEndTime(TimeUnit.MILLISECONDS)}")
                                                }
                                            }

                                        }
                                    }
                                }



                            }


                            val dataList = mutableListOf<Map<String, Any>>()
                            response.buckets.forEach {

                                it.dataSets.forEach {

                                    it.dataPoints.forEach { dataPoint ->
                                        val data = hashMapOf(
                                                "value" to getHealthDataValue(dataPoint, unit),
                                                "date_from" to dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                                                "date_to" to dataPoint.getEndTime(TimeUnit.MILLISECONDS),
                                                "unit" to unit.toString()
                                        )
                                        dataList.add(data)
                                    }
                                }
                            }


                            Log.e("GoogleFit", "Steps total is $total")
                            Log.e("GoogleFit", "Total cal is $expendedCalories")


                            val data = hashMapOf(
                                    "value" to total,
                                    "date_from" to startTimeFromFlutter,
                                    "date_to" to endTimeFromFlutter,
                                    "unit" to Field.FIELD_STEPS
                            )
                            newDataList.add(data)
                            val calouriData = hashMapOf(
                                    "value" to expendedCalories,
                                    "date_from" to startTimeFromFlutter,
                                    "date_to" to endTimeFromFlutter,
                                    "unit" to Field.FIELD_CALORIES
                            )
                            newDataList.add(calouriData)

                            result.success(newDataList)
                        }
                        .addOnFailureListener { e ->
                            Log.i("ERROR ", "There was an error reading data from Google Fit", e)
                        }


                ///New CODE ENDS

                ///OLD CODE START

                /* val response = Fitness.getHistoryClient(activity.applicationContext, googleSignInAccount).readData(
                         DataReadRequest.Builder()

                                 .read(dataType)
                                 .setTimeRange(startTimeFromFlutter, endTimeFromFlutter, TimeUnit.MILLISECONDS)
                                 .build()
                 )

                 /// Fetch all data points for the specified DataType
                 val dataPoints = Tasks.await<DataReadResponse>(response).getDataSet(dataType)

                 /// For each data point, extract the contents and send them to Flutter, along with date and unit.
                 val healthData = dataPoints.dataPoints.mapIndexed { _, dataPoint ->
                     return@mapIndexed hashMapOf(
                             "value" to getHealthDataValue(dataPoint, unit),
                             "date_from" to dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                             "date_to" to dataPoint.getEndTime(TimeUnit.MILLISECONDS),
                             "unit" to unit.toString()
                     )
                 }

                 activity.runOnUiThread { result.success(healthData) }*/
                ///OLD CODE ENDS

            } catch (e3: Exception) {
                activity.runOnUiThread { result.success(null) }
            }
        }
    }

    private fun callToHealthTypes(call: MethodCall): FitnessOptions {
        val typesBuilder = FitnessOptions.builder()
        val args = call.arguments as HashMap<*, *>
        val types = args["types"] as ArrayList<*>
        for (typeKey in types) {
            if (typeKey !is String) continue
            typesBuilder.addDataType(keyToHealthDataType(typeKey), FitnessOptions.ACCESS_READ)
        }
        return typesBuilder.build()
    }

    /// Called when the "requestAuthorization" is invoked from Flutter
    private fun requestAuthorization(call: MethodCall, result: Result) {
        val optionsToRegister = callToHealthTypes(call)
        mResult = result

        val isGranted = GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), fitnessOptions)

        /// Not granted? Ask for permission
        if (!isGranted) {
            GoogleSignIn.requestPermissions(
                    activity,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(activity),
                    optionsToRegister)
        }
        /// Permission already granted
        else {
            mResult?.success(true)
        }
    }

    /// Handle calls from the MethodChannel
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "requestAuthorization" -> requestAuthorization(call, result)
            "getData" -> getData(call, result)
            else -> result.notImplemented()
        }
    }
}

/*

///fetch only current date date

Fitness.getHistoryClient(activity.applicationContext,googleSignInAccount)
                       .readDailyTotal(dataType)
                       .addOnSuccessListener { dataSet ->
                           val total = when {
                               dataSet.isEmpty -> 0
                               else -> {
                                   val healthData = dataSet.dataPoints.mapIndexed { _, dataPoint ->
                                       return@mapIndexed hashMapOf(
                                               "value" to getHealthDataValue(dataPoint, unit),
                                               "date_from" to dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                                               "date_to" to dataPoint.getEndTime(TimeUnit.MILLISECONDS),
                                               "unit" to unit.toString()
                                       )
                                   }
                                   activity.runOnUiThread { result.success(healthData) }

                                   dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                               }
                           }
                          Log.i("LOG IS THIS+++++++>", "Total steps: $total")
                       }
                       .addOnFailureListener { e ->
                           //Log.w(TAG, "There was a problem getting the step count.", e)
                       }*/