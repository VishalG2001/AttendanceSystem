package com.example.recycleviewpractice.mapTracking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.databinding.ActivityPuneMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import android.provider.Settings
import com.google.maps.android.SphericalUtil

class PuneMap : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityPuneMapBinding
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var intermediatePoints = mutableListOf(
        LatLng(19.185846160060095, 73.18202679481483),
        LatLng(18.994297958979, 73.26940623459495)
    )
    private var highlightAreaLevels = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
    private var currentLocationMarker: Marker? = null
    private var totalRouteDistance: Double = 0.0
    private var currentDistanceAlongRoute: Double = 0.0
    private val highlightPolygons: MutableList<Polygon> = mutableListOf()
    private lateinit var markerPathPoints: MutableList<LatLng>
    private var markerPathPolyline: Polyline? = null
    private var circleOrigin: Circle? = null
    private var circleDestination: Circle? = null
    private var mainRoutePolyline: Polyline? = null
    private var currentLatLng: LatLng? = null
    private val  routePoints = mutableListOf<LatLng>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuneMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        markerPathPoints = mutableListOf()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setUpListener()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Check if location is enabled
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location not enabled, prompt user to turn it on
            Toast.makeText(this, "Please turn on location services", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationButtonClickListener {
            // Zoom to current location
            currentLatLng?.let { location ->
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
            // Return true to indicate that you have consumed the event and do not want the default behavior
            true
        }


        // Get last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    destinationLatLng = LatLng(18.523171462602583, 73.84081426524024)
                    originLatLng = currentLatLng
                    drawRoute(currentLatLng!!, destinationLatLng!!, intermediatePoints)

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 12f))
                } else {
                    // Location not available
                    Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error getting current location: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setUpListener() {
        binding.apply {
            sbHighlightSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val level = progress + 1
//                    mainRoutePolyline=null
//                    markLocations(originLatLng!!, destinationLatLng!!)
                    clearMap()
                    drawHighlightArea(
                        level.toDouble()
                    )
                    drawCircleOrigin(originLatLng!!, level.toDouble())
                    drawCircleDestination(destinationLatLng!!, level.toDouble())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            btnJump.setOnClickListener {
                moveMarkerAlongRoute(5.0)
            }
        }
    }

    private fun drawRoute(
        origin: LatLng,
        destination: LatLng,
        intermediatePoints: List<LatLng>,
        level: Double = 1.0
    ) {
        clearMap()
        markLocations(origin, destination)
        drawMainRoute(origin, destination, intermediatePoints)
        drawHighlightArea( level)
        drawCircleOrigin(origin, level)
        drawCircleDestination(destination, level)
    }

    private fun markLocations(origin: LatLng, destination: LatLng) {
        mMap.addMarker(MarkerOptions().position(origin).title("My Location"))
        mMap.addMarker(MarkerOptions().position(destination).title("Destination"))

    }

    private fun drawMainRoute(
        origin: LatLng,
        destination: LatLng,
        intermediatePoints: List<LatLng>
    ) {
        routePoints.clear()
        routePoints.add(origin)
        routePoints.addAll(intermediatePoints)
        routePoints.add(destination)

        if (mainRoutePolyline == null) {
            mainRoutePolyline = mMap.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .width(10f)
                    .color(Color.BLUE)
            )
        }
    }

    private fun drawCircleOrigin(center: LatLng, level: Double) {
        val radiusKm = highlightAreaLevels[(level - 1).toInt()]
        val radiusMeters = radiusKm * 1000

        circleOrigin = mMap.addCircle(
            CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeWidth(0f)
                .fillColor(ContextCompat.getColor(this@PuneMap, R.color.light_blue))
        )
    }

    private fun drawCircleDestination(center: LatLng, level: Double) {
        val radiusKm = highlightAreaLevels[(level - 1).toInt()]
        val radiusMeters = radiusKm * 1000

        circleDestination = mMap.addCircle(
            CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeWidth(0f)
                .fillColor(ContextCompat.getColor(this@PuneMap, R.color.light_blue))
        )
    }

    private fun drawHighlightArea(level: Double) {
        highlightPolygons.clear()

        val Points = mutableListOf<LatLng>()
        Points.add(originLatLng!!)
        Points.addAll(intermediatePoints)
        Points.add(destinationLatLng!!)

        // Iterate through the main route points
        for (i in 0 until Points.size - 1) {
            val point1 = Points[i]
            val point2 = Points[i + 1]

            // Create highlight area between consecutive points on the main route
            val polygon = createPolygonAlongMainRoute(point1, point2, level)
            highlightPolygons.add(polygon)
        }
    }

    private fun createPolygonAlongMainRoute(point1: LatLng, point2: LatLng, level: Double): Polygon {
        val radius = highlightAreaLevels[(level - 1).toInt()]
        val bearing = SphericalUtil.computeHeading(point1, point2)

        // Calculate points along the route using the bearing and radius
        val left = SphericalUtil.computeOffset(point1, radius * 1000, bearing - 90.0)
        val right = SphericalUtil.computeOffset(point1, radius * 1000, bearing + 90.0)
        val leftEnd = SphericalUtil.computeOffset(point2, radius * 1000, bearing - 90.0)
        val rightEnd = SphericalUtil.computeOffset(point2, radius * 1000, bearing + 90.0)

        return mMap.addPolygon(
            PolygonOptions()
                .add(left, leftEnd, rightEnd, right)
                .strokeWidth(0f)
                .fillColor(ContextCompat.getColor(baseContext, R.color.light_blue))
        )
    }


//    private fun drawHighlightArea(level: Double) {
//        highlightPolygons.clear()
//
//        // Create a path to cover the entire route
//        val routePath = mutableListOf<LatLng>()
//        routePath.add(originLatLng!!)
//        routePath.addAll(intermediatePoints)
//        routePath.add(destinationLatLng!!)
//
//        // Draw a single highlight polygon along the entire route path
//        val polygon = createPolygonAlongRoutePath(routePath, level)
//        highlightPolygons.add(polygon)
//    }
//
//    private fun createPolygonAlongRoutePath(routePath: List<LatLng>, level: Double): Polygon {
//        val radius = highlightAreaLevels.getOrElse((level - 1).toInt()) { 1.0 } * 1000  // Width in meters
//        val leftVertices = mutableListOf<LatLng>()
//        val rightVertices = mutableListOf<LatLng>()
//
//        // Calculate left and right offsets for the entire route path
//        for (i in 0 until routePath.size - 1) {
//            val point1 = routePath[i]
//            val point2 = routePath[i + 1]
//
//            // Calculate intermediate points for smoother polygon
//            val numIntermediatePoints = 50  // Increased for finer interpolation
//            for (j in 0..numIntermediatePoints) {
//                val fraction = j / numIntermediatePoints.toDouble()
//                val intermediatePoint = interpolate(point1, point2, fraction)
//                val (left, right) = calculateOffsets(point1, point2, intermediatePoint, radius)
//                leftVertices.add(left)
//                rightVertices.add(right)
//            }
//        }
//
//        // Add the right vertices in reverse order to close the polygon
//        rightVertices.reverse()
//        val polygonVertices = leftVertices + rightVertices
//
//        val polygonOptions = PolygonOptions()
//            .addAll(polygonVertices)
//            .strokeWidth(0f)
//            .fillColor(ContextCompat.getColor(this@PuneMap, R.color.light_blue))
//
//        return mMap.addPolygon(polygonOptions)
//    }
//
//    private fun calculateOffsets(start: LatLng, end: LatLng, point: LatLng, radius: Double): Pair<LatLng, LatLng> {
//        val bearing = SphericalUtil.computeHeading(start, end)
//        val left = SphericalUtil.computeOffset(point, radius, bearing - 90.0)
//        val right = SphericalUtil.computeOffset(point, radius, bearing + 90.0)
//        return Pair(left, right)
//    }
//
//    private fun interpolate(start: LatLng, end: LatLng, fraction: Double): LatLng {
//        val lat = (end.latitude - start.latitude) * fraction + start.latitude
//        val lng = (end.longitude - start.longitude) * fraction + start.longitude
//        return LatLng(lat, lng)
//    }


    private fun clearMap() {
        for (polygon in highlightPolygons) {
            polygon.remove()
        }
        highlightPolygons.clear()
        circleOrigin?.remove()
        circleDestination?.remove()

    }

    private fun moveMarkerAlongRoute(distanceKm: Double) {
        originLatLng ?: return
        destinationLatLng ?: return

        val routePoints = mutableListOf<LatLng>()
        routePoints.add(originLatLng!!)
        routePoints.addAll(intermediatePoints)
        routePoints.add(destinationLatLng!!)

        totalRouteDistance = computeTotalDistance(routePoints)

        currentDistanceAlongRoute += distanceKm * 1000

        if (currentDistanceAlongRoute >= totalRouteDistance) {
            Toast.makeText(this, "Reached end of route", Toast.LENGTH_SHORT).show()
            return
        }

        var accumulatedDistance = 0.0
        var newMarkerPosition: LatLng? = null

        for (i in 0 until routePoints.size - 1) {
            val segmentStart = routePoints[i]
            val segmentEnd = routePoints[i + 1]
            val segmentDistance = SphericalUtil.computeDistanceBetween(segmentStart, segmentEnd)

            if (currentDistanceAlongRoute <= accumulatedDistance + segmentDistance) {
                val fraction = (currentDistanceAlongRoute - accumulatedDistance) / segmentDistance
                newMarkerPosition = SphericalUtil.interpolate(segmentStart, segmentEnd, fraction)
                break
            } else {
                accumulatedDistance += segmentDistance
            }
        }

        newMarkerPosition?.let {
            markerPathPoints.add(it)

            if (markerPathPolyline == null) {
                markerPathPoints.add(originLatLng!!)
                markerPathPolyline = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(markerPathPoints)
                        .width(10f)
                        .color(Color.RED)
                        .zIndex(1f)
                )
            } else {
                markerPathPolyline?.points = markerPathPoints
            }

            if (currentLocationMarker == null) {
                currentLocationMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(it)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .title("Current Location")
                )
                currentLatLng = it
                Log.e("currentLocation : ", currentLatLng.toString())
            } else {
                currentLatLng = it
                Log.e("currentLocation : ", currentLatLng.toString())
                currentLocationMarker?.position = it
            }

//        drawMainRoute(originLatLng!!, destinationLatLng!!, intermediatePoints)  // Redraw main route here

        } ?: run {
            Toast.makeText(this, "Error calculating marker position", Toast.LENGTH_SHORT).show()
        }
    }

    private fun computeTotalDistance(points: List<LatLng>): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += SphericalUtil.computeDistanceBetween(points[i], points[i + 1])
        }
        return totalDistance
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}
