package com.example.recycleviewpractice.trackingSummary


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.databinding.ActivityTrackingSummaryBinding
import com.example.recycleviewpractice.mapTracking.MapTracking
import com.example.recycleviewpractice.mapTracking.PuneMap

class TrackingSummary : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingSummaryBinding
    private lateinit var adapter: TrackingSummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate the layout using view binding
        binding = ActivityTrackingSummaryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        //Toolbar
        binding.TrackingSummaryHeader.tvToolbarTitle.text=getString(R.string.Tracking)
        // Setup RecyclerView
        val items = createDummyData()
        adapter = TrackingSummaryAdapter(items)
        binding.rvTrackingSummary.adapter = adapter
        binding.btnStartTracking.setOnClickListener{
            val i = Intent(this, PuneMap::class.java)
            startActivity(i)
        }
    }

    private fun createDummyData(): List<ModelTrackingSummary> {
        val items = listOf(
            ModelTrackingSummary("loream Lorem ipsum dolor sit amet, consectetur adipiscin","loream Lorem ipsum dolor sit amet, consectetur adipiscin","06 June, 2024", "Ongoing", "10:00AM", "12:00PM"),
            ModelTrackingSummary("loream Lorem ipsum dolor sit amet, consectetur adipiscin","loream Lorem ipsum dolor sit amet, consectetur adipiscin","07 June, 2024", "Completed", "11:00AM", "01:00PM"),
            ModelTrackingSummary("loream Lorem ipsum dolor sit amet, consectetur adipiscin","loream Lorem ipsum dolor sit amet, consectetur adipiscin","08 June, 2024", "Cancelled", "09:00AM", "11:00AM")
            // Add more items as needed
        )
        Log.d("TrackingSummary", "Dummy Data: $items") // Log data to check if it's correct
        return items
    }


}
