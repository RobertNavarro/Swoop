package com.example.swoop

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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_rider_map.*


class RiderMapActivity : AppCompatActivity(), OnMapReadyCallback//, LocationListener
{
    private lateinit var fusedLocationClient: FusedLocationProviderClient// = LocationServices.getFusedLocationProviderClient(this)
    private lateinit var mMap: GoogleMap
    private lateinit var mCurrentLocation: Location
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var mLogoutButton: Button
    private lateinit var mRequestButton: Button
    private var isLoggingOut: Boolean = false
    private lateinit var pickupLocation: LatLng
    private var radius = 0.621371
    private var driverFound = false
    private var requestBool = false
    private lateinit var driverFoundID : String
    private lateinit var driverMarker: Marker
    private lateinit var pickupMarker: Marker
    private lateinit var riderID: String
    val UserPrefrences = "UserPrefs"
    lateinit var sharedPreferences : SharedPreferences
    lateinit var geoQuery : GeoQuery
    private lateinit var driverLocationRef: DatabaseReference
    private lateinit var postListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(UserPrefrences, Context.MODE_PRIVATE)
        setContentView(R.layout.activity_rider_map)
        askForPermission()

        mLogoutButton = logoutRider
        mLogoutButton.setOnClickListener {
            isLoggingOut = true
            sharedPreferences.edit().putString("role", "").apply()
            disconnectRider()
            Toast.makeText(baseContext, "Logging out", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        mRequestButton= request
        mRequestButton.setOnClickListener {

            if(requestBool)
            {
                requestBool = false

                geoQuery.removeAllListeners()
                driverLocationRef.removeEventListener(postListener)

                val userID = getUserID()
                val ref = FirebaseDatabase.getInstance().getReference("RiderRequest")
                val geoFire = GeoFire(ref)
                geoFire.removeLocation(userID)

                if(driverFoundID != "")
                {
                    val driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID)
                    driverRef.setValue(true)
                    driverFoundID = ""
                }
                driverFound = false
                radius = 0.621371

                if(pickupMarker != null)
                {
                    pickupMarker.remove()
                }
                mRequestButton.setText("Request Ride")
            }
            else
            {
                requestBool = true
                var userID = getUserID()

                val ref = FirebaseDatabase.getInstance().getReference("RiderRequest")
                val geoFire = GeoFire(ref)
                geoFire.setLocation(userID, GeoLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))

                pickupLocation = LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())

                val bitmap = BitmapFactory.decodeResource(resources,R.drawable.pickuplocation)
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 90, false);
                pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)))

                mRequestButton.setText("Finding your ride")

                getClosestDriver()
            }
        }

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
            setUp()
    }

    private fun getUserID(): String
    {
        val currentUser = FirebaseAuth.getInstance().getCurrentUser()
        var userID = ""
        if(currentUser != null)
        {
            userID = currentUser.getUid()
        }
        return userID
    }

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
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15F))
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

                    val ref = FirebaseDatabase.getInstance().getReference("RiderRequest")
                    val geoFire = GeoFire(ref)


                    var userID = getUserID()


                    //geoFire.setLocation(userID, GeoLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))

                    var latLng: LatLng = LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15F))
                    //Toast.makeText(baseContext, "Before startLocationUpdates", Toast.LENGTH_SHORT).show()
                    //startLocationUpdates()
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

    private fun disconnectRider()
    {
        val ref = FirebaseDatabase.getInstance().getReference("RiderRequest")
        val geoFire = GeoFire(ref)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        var currentUser = FirebaseAuth.getInstance().getCurrentUser()
        lateinit var userID: String
        if(currentUser != null)
        {
            userID = currentUser.getUid()
        }

        geoFire.removeLocation(userID)
    }


    protected override fun onDestroy()//anytime the rider leaves the map activity their location is removed from the database
    {

        if(!isLoggingOut)
        {
            disconnectRider()
        }
        //fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0)
        {
            if ((grantResults.isNotEmpty()) && !grantResults.contains(PackageManager.PERMISSION_DENIED))
            {
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


    override fun onMapReady(googleMap: GoogleMap)
    {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return
        }
        mMap.setMyLocationEnabled(true)
    }



    private fun startLocationUpdates()
    {
        //Toast.makeText(baseContext, "Inside startLocationUpdates", Toast.LENGTH_SHORT).show()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(baseContext, "Permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun getClosestDriver()
    {
        Toast.makeText(baseContext, "Inside getClosestDriver()", Toast.LENGTH_SHORT).show()
        var driverLocation : DatabaseReference = FirebaseDatabase.getInstance().getReference().child("DriversAvailable")
        var geoFire = GeoFire(driverLocation)

        geoQuery= geoFire.queryAtLocation(GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius)
        geoQuery.removeAllListeners()

        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener
        {

             override fun onKeyEntered(key: String?, location: GeoLocation?)
            {
                Toast.makeText(baseContext, "Inside onKeyEntered()", Toast.LENGTH_SHORT).show()
                if(!driverFound && key != null && requestBool)
                {
                    driverFound = true
                    driverFoundID = key

                    var driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID)
                    var currentUser  = FirebaseAuth.getInstance().getCurrentUser()
                    if(currentUser != null)
                    {
                        riderID = currentUser.getUid()
                    }
                    val map = mapOf("riderRideID" to riderID)
                    driverRef.updateChildren(map)

                    getDriverLocation()
                    mRequestButton.setText("Locating your ride")
                }
            }

            override fun onKeyMoved(key: String?, location: GeoLocation?){
                Toast.makeText(baseContext, "Inside onKeyMoved()", Toast.LENGTH_SHORT).show()
            }

            override fun onKeyExited(key: String?){
                Toast.makeText(baseContext, "Inside onKeyExited()", Toast.LENGTH_SHORT).show()
            }
            override fun onGeoQueryError(error: DatabaseError?){
                Toast.makeText(baseContext, "Inside onGeoQueryError() " + error, Toast.LENGTH_SHORT).show()
            }

            override fun onGeoQueryReady()
            {
                Toast.makeText(baseContext, "Inside onGeoQueryReady()", Toast.LENGTH_SHORT).show()
                if(!driverFound)
                {
                    radius += 0.621371
                    Toast.makeText(baseContext, "Increasing search radius", Toast.LENGTH_SHORT).show()
                    getClosestDriver()
                }

            }

        })
    }


    private fun getDriverLocation()
    {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(driverFoundID).child("l")

        postListener = object : ValueEventListener
        {
            override fun onDataChange(dataSnapshot: DataSnapshot)
            {
                if(dataSnapshot.exists() && requestBool)
                {
                    var map  = dataSnapshot.getValue() as List<kotlin.Double>
                    var locationLat : Double = 0.0
                    var locationLng : Double = 0.0
                    mRequestButton.setText("Your ride has been found")
                    if(map.get(0) != null)
                    {
                        locationLat = map.get(0)

                    }
                    if(map.get(1) != null)
                    {
                        locationLng = map.get(1)

                    }

                    var driverLatLng = LatLng(locationLat, locationLng)
                    if(::driverMarker.isInitialized)
                    {
                        driverMarker.remove()
                   }
                    val bitmap = BitmapFactory.decodeResource(resources,R.drawable.driverlocation)
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 90, false);
                    driverMarker = mMap.addMarker(MarkerOptions().position(driverLatLng).title("Your Ride").icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap)))

                    var pickupLoc = Location("")
                    pickupLoc.setLatitude(pickupLocation.latitude)
                    pickupLoc.setLongitude(pickupLocation.longitude)

                    var driverLoc = Location("")
                    driverLoc.setLatitude(driverLatLng.latitude)
                    driverLoc.setLongitude(driverLatLng.longitude)

                    var milesAway = (pickupLoc.distanceTo(driverLoc)/1609)
                    var distanceBetween = "The Driver is "+ String.format("%.1f",milesAway) + " miles away"
                    if(milesAway < 0.0568182)
                    {
                        mRequestButton.setText("The Driver is here")
                    }
                    else
                    {
                        mRequestButton.setText(distanceBetween)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }

        driverLocationRef.addValueEventListener(postListener)
    }
    private fun resizeBitmap(drawableName: String, width: Int, height: Int): Bitmap
    {
        val imageBitmap: Bitmap = BitmapFactory.decodeResource(resources, resources.getIdentifier(drawableName, "drawable", packageName)
        )
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }
}
