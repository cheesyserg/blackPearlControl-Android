package com.cheslabs.blackpearl

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors


class MainActivity : Activity() {

    private lateinit var usbManager: UsbManager
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var endpoint: UsbEndpoint? = null
    private lateinit var buttonAmpTopologyClassAB: Button
    private lateinit var buttonAmpTopologyClassH: Button
    private lateinit var buttonGainLow: Button
    private lateinit var buttonGainHigh: Button
    private lateinit var buttonFilterFastLL: Button
    private lateinit var buttonFilterFastPC: Button
    private lateinit var buttonFilterSlowLL: Button
    private lateinit var buttonFilterSlowPC: Button
    private lateinit var buttonFilterNOS: Button
    private val ACTION_USB_PERMISSION = "com.cheslabs.blackpearl.USB_PERMISSION"
    private lateinit var permissionIntent: PendingIntent
    private lateinit var usbReceiver: BroadcastReceiver
    private var dataToSend: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_USB_PERMISSION) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            this@MainActivity.device = device
                            openDeviceAndSendData()
                        }
                    } else {
                        Log.d("USB", "Permission denied")
                    }
                }
            }
        }
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)

        buttonAmpTopologyClassAB = findViewById(R.id.buttonAmpTopologyClassAB)
        buttonAmpTopologyClassH = findViewById(R.id.buttonAmpTopologyClassH)
        buttonGainLow = findViewById(R.id.buttonGainLow)
        buttonGainHigh = findViewById(R.id.buttonGainHigh)
        buttonFilterFastLL = findViewById(R.id.buttonFilterFastLL)
        buttonFilterFastPC = findViewById(R.id.buttonFilterFastPC)
        buttonFilterSlowLL = findViewById(R.id.buttonFilterSlowLL)
        buttonFilterSlowPC = findViewById(R.id.buttonFilterSlowPC)
        buttonFilterNOS = findViewById(R.id.buttonFilterNOS)

        buttonAmpTopologyClassAB.setOnClickListener {
            findDeviceAndSendData("4b011d01000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonAmpTopologyClassH.setOnClickListener {
            findDeviceAndSendData("4b011d01010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonGainLow.setOnClickListener {
            findDeviceAndSendData("4b011901000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonGainHigh.setOnClickListener {
            findDeviceAndSendData("4b011901010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonFilterFastLL.setOnClickListener {
            findDeviceAndSendData("4b011101010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonFilterFastPC.setOnClickListener {
            findDeviceAndSendData("4b011101020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonFilterSlowLL.setOnClickListener {
            findDeviceAndSendData("4b011101030000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonFilterSlowPC.setOnClickListener {
            findDeviceAndSendData("4b011101040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
        buttonFilterNOS.setOnClickListener {
            findDeviceAndSendData("4b011101050000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        }
    }

    private fun findDeviceAndSendData(data: String) {
        val deviceList = usbManager.deviceList
        for ((_, dev) in deviceList) {
            if (dev.vendorId == 0x3302 && dev.productId == 0x43E8) {
                if (usbManager.hasPermission(dev)) {
                    device = dev
                    openDeviceAndSendData(data)
                } else {
                    this.dataToSend = data
                    usbManager.requestPermission(dev, permissionIntent)
                }
                break
            }
        }
    }

    private fun openDeviceAndSendData() {
        openDeviceAndSendData(dataToSend ?: return)
    }

    private fun openDeviceAndSendData(data: String) {
        // Open the device and claim the interface
        val conn = usbManager.openDevice(device)
        if (conn != null) {
            connection = conn
            Log.d("USB", "Device opened")
            val usbInterface = device!!.getInterface(0)
            connection!!.claimInterface(usbInterface, true)
            for (i in 0 until usbInterface.endpointCount) {
                val ep = usbInterface.getEndpoint(i)
                if (ep.address.toInt() == 0x05) {
                    endpoint = ep
                    break
                }
            }
            sendData(data)
        } else {
            Log.e("USB", "Failed to open device")
            Toast.makeText(this, "Failed to open device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendData(data: String) {
        if (connection != null && endpoint != null) {
            val byteData = data.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val bytesTransferred = connection!!.bulkTransfer(endpoint, byteData, byteData.size, 1000)
            Log.d("USB", "Bytes transferred: $bytesTransferred")
            Toast.makeText(this, "Bytes transferred: $bytesTransferred", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("USB", "No connection or endpoint found")
            Toast.makeText(this, "No connection or endpoint found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}