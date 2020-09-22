package com.example.swoop

//import com.google.android.gms:play-services-location:15.0.1'

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_driver_map.*
import kotlinx.android.synthetic.main.activity_main.*


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback//, LocationListener
{
    private lateinit var fusedLocationClient: FusedLocationProviderClient// = LocationServices.getFusedLocationProviderClient(this)
    private lateinit var mMap: GoogleMap
    private lateinit var mCurrentLocation: Location
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var mLogoutButton: Button
    private var isLoggingOut: Boolean = false
    private var riderID = ""
    val UserPrefrences = "UserPrefs"
    lateinit var sharedPreferences : SharedPreferences
    private var availableToDrive = true
    var count = 0
    private lateinit var pickupMarker: Marker
    private lateinit var assignedRiderPickupLocationRef: DatabaseReference
    private lateinit var postListenerRider: ValueEventListener



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(UserPrefrences, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_driver_map)
        askForPermission()

        mLogoutButton = logout
        mLogoutButton.setOnClickListener {
            isLoggingOut = true
            sharedPreferences.edit().putString("role", "").apply()
            disconnectDriver()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        getAssignedRider()
    }


    private fun getAssignedRider()
    {
        var currentDriver  = FirebaseAuth.getInstance().getCurrentUser()
        lateinit var driverID: String
        if(currentDriver != null)
        {
            driverID = currentDriver.getUid()
        }
        var assignedRiderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("riderRideID")
        Toast.makeText(baseContext, "Inside getAssignedRider", Toast.LENGTH_LONG).show()
        postListenerRider = object : ValueEventListener
        {
            override fun onDataChange(dataSnapshot: DataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    riderID = dataSnapshot.getValue().toString()
                    Toast.makeText(baseContext, "getting the pickup location", Toast.LENGTH_SHORT).show()
                    getAssignedRiderPickupLocation()
                }
                else
                {
                    riderID = ""
                    if(::pickupMarker.isInitialized)
                    {
                        Toast.makeText(baseContext, "The pickupMarker is: " + ::pickupMarker.isInitialized, Toast.LENGTH_SHORT).show()
                        pickupMarker.remove()
                    }
                    if(::postListenerRider.isInitialized && ::assignedRiderPickupLocationRef.isInitialized)
                    {
                        Toast.makeText(baseContext, "The postListenerCustomer is: " + ::postListenerRider.isInitialized, Toast.LENGTH_SHORT).show()
                        assignedRiderPickupLocationRef.removeEventListener(postListenerRider)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }

        assignedRiderRef.addValueEventListener(postListenerRider)
    }

    private fun getAssignedRiderPickupLocation()
    {
        Toast.makeText(baseContext, "Inside getAssignedRiderPickupLocation: " + riderID, Toast.LENGTH_SHORT).show()
        assignedRiderPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("RiderRequest").child(riderID).child("l")
        var postListener = object : ValueEventListener
        {
            override fun onDataChange(dataSnapshot: DataSnapshot)
            {
                Toast.makeText(baseContext, "Inside the onDataChange() of getAssignedRiderPickupLocation()" , Toast.LENGTH_SHORT).show()
                if(dataSnapshot.exists() && riderID != "")
                {
                    var map  = dataSnapshot.getValue() as List<kotlin.Double>
                    var locationLat : Double = 0.0
                    var locationLng : Double = 0.0
                    //mRequestButton.setText("Your ride has been found")
                    if(map.get(0) != null)
                    {
                        locationLat = map.get(0)//.toString().toDouble()

                    }
                    if(map.get(1) != null)
                    {
                        locationLng = map.get(1)//.toString().toDouble()

                    }

                    var riderLatLng = LatLng(locationLat, locationLng)
                    val bitmap = BitmapFactory.decodeResource(resources,R.drawable.pickuplocation)
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 90, false)
                    pickupMarker = mMap.addMarker(MarkerOptions().position(riderLatLng).title("Pickup Location").icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)))
                    Toast.makeText(baseContext, "The lat is: " + locationLat, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }

        assignedRiderPickupLocationRef.addValueEventListener(postListener)
    }

    private fun askForPermission()
    {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(baseContext, "Asking for permission", Toast.LENGTH_SHORT).show()
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this, permissions,0)
        }
        else
        {
            getAssignedRider()
            setUp()
        }
    }

//    override fun onResume()
//    {
//        super.onResume()
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
//        {
//                askForPermission()
//        }
//    }

    private fun setUp()
    {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationRequest = LocationRequest()
        mLocationRequest.setInterval(1000)
        mLocationRequest.setFastestInterval(1000)
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(baseContext, "First enable LOCATION ACCESS in settings.", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            if(location == null)
            {
                Toast.makeText(baseContext, "Error: the location is null", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(baseContext, "Success: the location has been found", Toast.LENGTH_SHORT).show()
                mCurrentLocation = location
                var latLng: LatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11F))
            }
        }

        locationCallback = object : LocationCallback()
        {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                if (locationResult?.lastLocation != null)
                {
                    mCurrentLocation = locationResult.lastLocation

                    //Toast.makeText(baseContext, mCurrentLocation.toString(), Toast.LENGTH_SHORT).show()

                    var ref = FirebaseDatabase.getInstance().getReference("DriversAvailable")
                    var geoFire = GeoFire(ref)
                    val driversWorkingRef = FirebaseDatabase.getInstance().getReference("DriversWorking")
                    //var geoFireAvailable = GeoFire(ref)//Drivers Available
                    var geoFireWorking = GeoFire(driversWorkingRef)
                    var currentUser = FirebaseAuth.getInstance().getCurrentUser()
                    lateinit var userID: String
                    if(currentUser != null)
                    {
                        userID = currentUser.getUid()
                        //Toast.makeText(baseContext, "The user is not null: " + userID, Toast.LENGTH_SHORT).show()
                    }
                    if(availableToDrive)
                    {
                        geoFire.setLocation(userID, GeoLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                    }

                    var latLng = LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11F))
                    //Toast.makeText(baseContext, "Before startLocationUpdates", Toast.LENGTH_SHORT).show()
                    when(riderID){
                        "" ->
                        {
                            geoFireWorking.removeLocation(userID)
                            geoFire.setLocation(userID, GeoLocation(mCurrentLocation.latitude, mCurrentLocation.longitude))
                            //Toast.makeText(baseContext, "Inside the blank one", Toast.LENGTH_SHORT).show()

                        }

                        else ->
                        {
                            if(availableToDrive)
                            {

                            geoFireWorking.setLocation(userID, GeoLocation(mCurrentLocation.latitude, mCurrentLocation.longitude))
                            geoFire.removeLocation(userID)

                            Toast.makeText(baseContext, "Inside the else", Toast.LENGTH_SHORT).show()
                            availableToDrive = false
                            count++
                            }
                        }
                    }

                    startLocationUpdates()
                }
                else
                {
                    Toast.makeText(baseContext, "Error: the location is null in LocationCallback", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper())

    }



    private fun disconnectDriver()
    {
        val driversAvailableRef = FirebaseDatabase.getInstance().getReference("DriversAvailable")
        val geoFire = GeoFire(driversAvailableRef)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        var currentUser = FirebaseAuth.getInstance().getCurrentUser()
        lateinit var userID: String
        if(currentUser != null)
        {
            userID = currentUser.getUid()
        }

        val driversRef = FirebaseDatabase.getInstance().getReference("DriversAvailable")
        geoFire.removeLocation(userID)
        driversRef.child(userID).removeValue()
    }


    protected override fun onStop()//anytime the driver leaves the map activity their location is removed from the database
    {
        super.onStop()

        if(!isLoggingOut)
        {
            disconnectDriver()
        }

    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0)
        {
            if ((grantResults.isNotEmpty()) && !grantResults.contains(PackageManager.PERMISSION_DENIED))
            {
                getAssignedRider()
                setUp()
            }
            else
            {
                Toast.makeText(baseContext, "Please activate location permission to continue using the app.", Toast.LENGTH_LONG).show()
                //startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                finish()
            }

        }

    }

//    fun stopLocationUpdates() {
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//    }
//    override fun onDestroy()
//    {
//        super.onDestroy()
//        stopLocationUpdates()
//    }
    override fun onMapReady(googleMap: GoogleMap)
    {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return
        }
        mMap.setMyLocationEnabled(true)
    }


//    override fun onResume() {
//      super.onResume()
//     if (requestingLocationUpdates()) startLocationUpdates()
//    }

    private fun startLocationUpdates()
    {
        //Toast.makeText(baseContext, "Inside startLocationUpdates", Toast.LENGTH_SHORT).show()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(baseContext, "Permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
//        else
//        {
//            fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper())
//        }
    }

   /*fun onConnected(bundle: Bundle?)
    {
        mLocationRequest = LocationRequest()
        mLocationRequest.setInterval(1000)
        mLocationRequest.setFastestInterval(1000)
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(baseContext, "Permission not granted",
                Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                if(location != null)
                {
                    Toast.makeText(baseContext, "Location is not null",
                        Toast.LENGTH_SHORT).show()
                    fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.getMainLooper())

                }
                else
                {
                    Toast.makeText(baseContext, "Error: location is null: $location",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }*/
}
