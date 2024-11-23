package com.weatherapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.weatherapp.databinding.ActivityMainBinding
import com.weatherapp.models.WeatherResponse
import com.weatherapp.network.WeatherService
import retrofit.Call
import retrofit.Callback
import retrofit.GsonConverterFactory
import retrofit.Response
import retrofit.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {
    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    private var mProgressDialog:Dialog? = null

    private var binding : ActivityMainBinding? =null

    // A global variable for Current Latitude
    private var mLatitude: Double = 0.0
    // A global variable for Current Longitude
    private var mLongitude: Double = 0.0

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
       binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)

        setUpUI()

        if (!isLocationEnabled()){
            Toast.makeText(this,"Your location provider is turned off.Please turn it",Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        else{

            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
           // Toast.makeText(this,"your location  provider is already turned ON",Toast.LENGTH_LONG).show()

        }

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {0
        return when (item.itemId) {
            R.id.action_refresh -> {
                getLocationDetails()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun isLocationEnabled():Boolean{

        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }
    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,mLocationCallback,
            Looper.myLooper()
        )
    }
    private val mLocationCallback = object :LocationCallback(){
        override fun onLocationResult(locationresult: LocationResult) {
            val mLastLocation : Location = locationresult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")
            //super.onLocationResult(locationresult)

            getLocationDetails()
        }

    }
    private fun getLocationDetails(){
        if (Constants.isNetworkAvailable(this)){

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service:WeatherService = retrofit.
            create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                mLatitude, mLongitude, Constants.METRIC_UNIT,Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if (response!!.isSuccess){

                        hideProgressDialog()
                        val weatherList:WeatherResponse = response.body()

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()
                        setUpUI()
                        Log.i("Response Result","$weatherList")
                    }
                    else{
                        val rc = response.code()
                        when(rc){
                            400 -> {
                                Log.e("Error 400", "Bad Connection")

                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")

                            }
                            else ->{
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    Log.e("Error", t!!.message.toString())
                    hideProgressDialog()
                }

            })

            /*   Toast.makeText(
                   this@MainActivity,
                   "You have connected to the internet. Now you can make an api call",
                   Toast.LENGTH_SHORT
               ).show()*/
        }

        else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun showCustomProgressDialog(){

        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()

    }

    private fun hideProgressDialog(){
        if (mProgressDialog!=null){

            mProgressDialog!!.dismiss()
        }
    }

    private fun setUpUI(){

        val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if (!weatherResponseJsonString.isNullOrEmpty()) {

            val weatherList = Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
            for(i in weatherList.weather.indices){
                Log.i("Weather name",weatherList.weather.toString())

                binding?.tvMain?.text = weatherList.weather[i].main
                binding?.tvMainDescription?.text = weatherList.weather[i].description
                binding?.tvTemp?.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                binding?.tvHumidity?.text = weatherList.main.humidity.toString()+" per cent"
                binding?.tvMin?.text = weatherList.main.tempMin.toString()+" min"
                binding?.tvMax?.text = weatherList.main.tempMax.toString() + " max"
                binding?.tvSpeed?.text = weatherList.wind.speed.toString()
                binding?.tvName?.text = weatherList.name
                binding?.tvCountry?.text = weatherList.sys.country

                binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise)
                binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset)

                when(weatherList.weather[i].icon){
                    "01d" ->  binding?.ivMain?.setImageResource(R.drawable.sunny)
                    "02d" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03d" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04d" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04n" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10d" ->  binding?.ivMain?.setImageResource(R.drawable.rain)
                    "11d" ->  binding?.ivMain?.setImageResource(R.drawable.storm)
                    "13d" ->  binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    "01n" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "02n" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03n" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10n" ->  binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "11n" ->  binding?.ivMain?.setImageResource(R.drawable.rain)
                    "13n" ->  binding?.ivMain?.setImageResource(R.drawable.snowflake)


                }

            }
        }



    }


    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex:Long):String?{
        val date =Date(timex *1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)

    }
}