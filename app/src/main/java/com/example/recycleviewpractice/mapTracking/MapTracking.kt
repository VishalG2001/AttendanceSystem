package com.example.recycleviewpractice.mapTracking

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.recycleviewpractice.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.recycleviewpractice.databinding.ActivityMapTrackingBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.PolygonOptions
import android.util.Log
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Polygon
import java.lang.StrictMath.toRadians
import kotlin.math.cos
import kotlin.math.sqrt

class MapTracking : AppCompatActivity(), OnMapReadyCallback {
    private var destinationLatLng: LatLng? = null
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMapTrackingBinding
    private var originLatLng: LatLng? = null
    private var originCircle: Circle? = null
    private var destinationCircle: Circle? = null
    private var highlightLatLng: Polygon? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setUpListener()


    }

    private fun setUpListener() {
        binding.apply {
            sbHighlightSize.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
//                mMap.clear()
                    originCircle?.remove()
                    destinationCircle?.remove()
                    highlightLatLng?.remove()
                    // Update the map based on the SeekBar progress (1 to 5)
                    val level =
                        progress + 1
//                    markLocations(originLatLng!!, destinationLatLng!!)
                    drawMainRoute(originLatLng!!, destinationLatLng!!)
                    drawHighlightArea(originLatLng!!, destinationLatLng!!, level.toDouble())
                    drawCircleOrigin(originLatLng!!, level.toDouble())
                    drawCircleDestination(destinationLatLng!!, level.toDouble())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Handle touch event if needed
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Handle touch event if needed
                }
            })
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
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
        // Enable My Location
        mMap.isMyLocationEnabled = true

        // Get current location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    Log.d("MapTracking", "Current location: $currentLatLng")

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                    // Set destination (for example, on button click)
                    destinationLatLng =
                        LatLng(19.218330, 72.978088) // Replace with your destination LatLng
                    originLatLng = currentLatLng

                    drawRoute(currentLatLng, destinationLatLng!!)
                } else {
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


    private fun drawRoute(origin: LatLng, destination: LatLng) {
        // Clear previous routes
        mMap.clear()

        markLocations(origin, destination)

        // Draw the main route line (dark blue)
        drawMainRoute(origin, destination)

        // Draw the highlighted area
        drawHighlightArea(origin, destination, 1.0)
        // Draw a circle with a radius of 5 kilometers around the current location
        drawCircleOrigin(origin)
        drawCircleDestination(destination, 1.0)
    }

    private fun markLocations(origin: LatLng, destination: LatLng) {
        mMap.addMarker(MarkerOptions().position(origin).title("My Location"))
        mMap.addMarker(MarkerOptions().position(destination).title("Destination"))
    }

    private fun drawMainRoute(origin: LatLng, destination: LatLng) {
        mMap.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .width(10f) // Width of the main route line
                .color(Color.BLUE)
        )
    }
    // Function to draw the semicircle based on SeekBar value


    private fun drawCircleOrigin(center: LatLng, level: Double = 1.0) {
        val radiusKm =
            highlightAreaLevels[(level - 1).toInt()] // Get the radius in kilometers based on the level
        val radiusMeters = radiusKm * 1000 // Convert radius to meters


        // Draw the new circle and keep a reference to it
        originCircle = mMap.addCircle(
            CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeWidth(0f)
                .fillColor(ContextCompat.getColor(this@MapTracking, R.color.light_blue))
        )
    }

    private fun drawCircleDestination(center: LatLng, level: Double) {
        val radiusKm =
            highlightAreaLevels[(level - 1).toInt()] // Get the radius in kilometers based on the level
        val radiusMeters = radiusKm * 1000 // Convert radius to meters


        // Draw the new circle and keep a reference to it
        destinationCircle = mMap.addCircle(
            CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeWidth(0f)
                .fillColor(ContextCompat.getColor(this@MapTracking, R.color.light_blue))
        )
    }


    private var highlightAreaLevels = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)

    // Function to draw the highlighted area based on SeekBar value
    private fun drawHighlightArea(origin: LatLng, destination: LatLng, level: Double) {
        val widthKm =
            highlightAreaLevels[(level - 1).toInt()] // Get the width in kilometers based on the level

        val points = createHighlightPolygon(origin, destination, widthKm)

        // Draw the polygon on the map
        highlightLatLng = mMap.addPolygon(
            PolygonOptions()
                .addAll(points)
                .fillColor(
                    ContextCompat.getColor(
                        this@MapTracking,
                        R.color.light_blue
                    )
                ) // Light blue color with transparency
                .strokeWidth(0f) // No border for the polygon
        )
    }

    private fun createHighlightPolygon(
        origin: LatLng,
        destination: LatLng,
        widthKm: Double
    ): List<LatLng> {
        val width = widthKm * 1000 // Convert km to meters

        // Calculate the perpendicular distance from the line
        val dx = destination.longitude - origin.longitude
        val dy = destination.latitude - origin.latitude
        val length = sqrt(dx * dx + dy * dy)
        val unitDx = dx / length
        val unitDy = dy / length

        val offsetX = width * unitDy
        val offsetY = -width * unitDx

        // Create the points of the polygon
        val points = mutableListOf<LatLng>()
        points.add(
            LatLng(
                origin.latitude + offsetY / 111320,
                origin.longitude + offsetX / (111320 * cos(toRadians(origin.latitude)))
            )
        )
        points.add(
            LatLng(
                destination.latitude + offsetY / 111320,
                destination.longitude + offsetX / (111320 * cos(toRadians(destination.latitude)))
            )
        )
        points.add(
            LatLng(
                destination.latitude - offsetY / 111320,
                destination.longitude - offsetX / (111320 * cos(toRadians(destination.latitude)))
            )
        )
        points.add(
            LatLng(
                origin.latitude - offsetY / 111320,
                origin.longitude - offsetX / (111320 * cos(toRadians(origin.latitude)))
            )
        )
        return points
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}
