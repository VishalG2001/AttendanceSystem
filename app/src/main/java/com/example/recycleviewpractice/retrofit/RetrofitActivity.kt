package com.example.recycleviewpractice.retrofit

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.recycleviewpractice.databinding.ActivityRetrofitBinding
import com.example.recycleviewpractice.retrofit.adapter.ChargingStationAdapter
import com.example.recycleviewpractice.retrofit.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RetrofitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRetrofitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRetrofitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
        setUpListener()

    }

    private fun setUpListener() {
        binding.apply {
            btnFetchChargingData.setOnClickListener {
                fetchChargingStationData()
            }
        }
    }

    private fun fetchChargingStationData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                binding.tvChargingStation.visibility=View.VISIBLE
                binding.tvChargingStation.text = "data fetching...."
                val chargingResponse=RetrofitInstance.api.getChargingStationData()
                withContext(Dispatchers.Main) {
                    if (chargingResponse.isSuccessful) {
                        binding.tvChargingStation.visibility=View.GONE
                        val chargingStationModel = chargingResponse.body()
                        chargingStationModel?.let { model ->
                            binding.btnFetchChargingData.text = "Fetch Data (${model.result.total_no_of_stations})"
                            val adapter = ChargingStationAdapter(model.result.stations)
                            binding.rvChargingStation.adapter = adapter
                        }
                    } else {
                        // Handle error
                        binding.tvChargingStation.text = "Error: ${chargingResponse.errorBody()?.string()}"
                    }
                }
                } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Handle network error
                    binding.tvChargingStation.text = " error: ${e.message}"
                }
            }
        }
    }

    private fun setUpView() {

    }
}