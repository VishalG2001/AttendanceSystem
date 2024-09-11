package com.example.recycleviewpractice.bluetoothConnection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.recycleviewpractice.R

class BluetoothDevicesActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDevices: MutableList<BluetoothDevice>
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_LOCATION_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_devices)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothDevices = mutableListOf()
        progressBar = findViewById(R.id.progress_bar)

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        checkLocationPermission()
        setupListView()
        startDeviceDiscovery()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION)
        } else {
            startDeviceDiscovery()
        }
    }

    private fun setupListView() {
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        val listView: ListView = findViewById(R.id.device_list)
        listView.adapter = deviceListAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val device = bluetoothDevices[position]
            pairDevice(device)
            showDeviceDetails(device)
        }
    }

    private fun startDeviceDiscovery() {
        progressBar.visibility = ProgressBar.VISIBLE
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter.startDiscovery()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        val deviceClass = it.bluetoothClass.deviceClass
                        if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                            if (!bluetoothDevices.contains(it)) {
                                bluetoothDevices.add(it)
                                val deviceInfo = "${it.name} - ${it.address}"
                                deviceListAdapter.add(deviceInfo)
                                deviceListAdapter.notifyDataSetChanged()
                                progressBar.visibility = View.GONE
                            }
                        }
                    } else {
                        Toast.makeText(context, "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun pairDevice(device: BluetoothDevice) {
        try {
            val method = device.javaClass.getMethod("createBond")
            method.invoke(device)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDeviceDetails(device: BluetoothDevice) {
        val deviceName = device.name ?: "Unknown Device"
        val deviceAddress = device.address
        val deviceType = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        } else {

        }
        when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
            else -> "Unknown Type"
        }
        val details = "Name: $deviceName\nAddress: $deviceAddress\nType: $deviceType"
        Toast.makeText(this, details, Toast.LENGTH_LONG).show()
        Log.d("BTActivity ", "device details : $details")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDeviceDiscovery()
                } else {
                    Toast.makeText(this, "Location permission is required to discover Bluetooth devices", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
