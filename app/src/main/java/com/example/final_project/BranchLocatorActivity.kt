package com.example.final_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

data class Branch(val name: String, val address: String, val location: LatLng)

class BranchLocatorActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentUserLocation: LatLng? = null
    private var userMarker: Marker? = null
    private var isFirstLocationUpdate = true

    // Danh sách lưu trữ marker chi nhánh
    private val branchMarkers = mutableListOf<Marker>()

    private val mockBranches = listOf(
        Branch("TTK Bank - Chi nhánh Thủ Đức", "123 Võ Văn Ngân, Thủ Đức", LatLng(10.8499, 106.7523)),
        Branch("TTK Bank - Chi nhánh Quận 9", "456 Lê Văn Việt, Quận 9", LatLng(10.8440, 106.7997)),
        Branch("TTK Bank - Chi nhánh Dĩ An", "789 Nguyễn An Ninh, Dĩ An", LatLng(10.9080, 106.7937)),
        Branch("TTK Bank - Chi nhánh Quận 2", "101 Trần Não, Quận 2", LatLng(10.7915, 106.7311)),
        Branch("TTK Bank - Chi nhánh Bình Thạnh", "212 Xô Viết Nghệ Tĩnh, Bình Thạnh", LatLng(10.8038, 106.7068))
    )

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private val TDTU_LOCATION = LatLng(10.732431, 106.699256)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branch_locator)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        showStaticBranches()

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(TDTU_LOCATION, 12f))

        mMap.setOnInfoWindowClickListener { marker ->
            val branch = mockBranches.find { it.name == marker.title }
            if (branch != null) {
                val startLocation = currentUserLocation ?: TDTU_LOCATION
                launchGoogleMapsDirections(startLocation, branch)
            }
        }

        checkLocationPermission()
    }

    private fun showStaticBranches() {
        mMap.clear()
        branchMarkers.clear()
        userMarker = null
        
        mockBranches.forEach { branch ->
            val marker = mMap.addMarker(MarkerOptions()
                .position(branch.location)
                .title(branch.name)
                .snippet(branch.address)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
            
            marker?.let { branchMarkers.add(it) }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        mMap.isMyLocationEnabled = true 

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val userLocation = LatLng(location.latitude, location.longitude)
                    updateUserLocationOnMap(userLocation)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    private fun updateUserLocationOnMap(location: LatLng) {
        var userLocation = location
        
        if (isFarFromVietnam(userLocation)) {
            userLocation = TDTU_LOCATION
            if (isFirstLocationUpdate) {
                Toast.makeText(this, "Máy ảo (Nước ngoài) -> Đã gán vị trí về ĐH Tôn Đức Thắng", Toast.LENGTH_LONG).show()
            }
        }

        currentUserLocation = userLocation
        
        if (userMarker == null) {
            userMarker = mMap.addMarker(MarkerOptions()
                .position(userLocation)
                .title("Bạn đang ở đây (TDTU)")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        } else {
            userMarker?.position = userLocation
            if (userLocation == TDTU_LOCATION) {
                userMarker?.title = "Bạn đang ở đây (TDTU)"
            }
        }

        if (isFirstLocationUpdate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13f))
            
            val nearestBranch = findNearestBranch(userLocation)
            nearestBranch?.let { branch ->
                Toast.makeText(this@BranchLocatorActivity, "Gần nhất: ${branch.name}", Toast.LENGTH_LONG).show()
                val marker = branchMarkers.find { it.title == branch.name }
                marker?.showInfoWindow()
            }

            isFirstLocationUpdate = false
        }
    }
    
    private fun isFarFromVietnam(location: LatLng): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, TDTU_LOCATION.latitude, TDTU_LOCATION.longitude, results)
        return results[0] > 1000000 
    }
    
    private fun isLocationClose(loc1: LatLng, loc2: LatLng): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
        return results[0] < 100
    }

    private fun findNearestBranch(userLocation: LatLng): Branch? {
        return mockBranches.minByOrNull { branch ->
            val results = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, branch.location.latitude, branch.location.longitude, results)
            results[0]
        }
    }

    private fun launchGoogleMapsDirections(origin: LatLng, destinationBranch: Branch) {
        val builder = Uri.parse("https://www.google.com/maps/dir/?api=1").buildUpon()
        
        if (isLocationClose(origin, TDTU_LOCATION)) {
            builder.appendQueryParameter("origin", "Đại học Tôn Đức Thắng, 19 Nguyễn Hữu Thọ, Quận 7, Hồ Chí Minh")
        } else {
            builder.appendQueryParameter("origin", "${origin.latitude},${origin.longitude}")
        }
        
        builder.appendQueryParameter("destination", "${destinationBranch.address}, Hồ Chí Minh")
        builder.appendQueryParameter("travelmode", "walking")
        
        val uri = builder.build()
        
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng Google Maps", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun launchGoogleMapsDirectionsToDestination(destination: LatLng) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Không tìm thấy ứng dụng Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
}
