package com.dk.gattserver.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.dk.gattserver.diretctAndrodBLE.GattServer
import com.dk.gattserver.R
import kotlinx.android.synthetic.main.activity_server.*

class ServerActivity : AppCompatActivity(), GattServer.ConnectionListener {

    private val TAG = ServerActivity::javaClass.name
    lateinit var mGattServer: GattServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        mGattServer = GattServer(this, this)
    }


    @SuppressLint("MissingPermission")
    override fun deviceConnected(device: BluetoothDevice) {
        Log.d(TAG,"${device.name} connected")
        connectedDevicesTV.text =  "${device.name} connected"
    }

    @SuppressLint("MissingPermission")
    override fun deviceDisconnected(device: BluetoothDevice) {
        Log.d(TAG,"${device.name} disconnected")
        connectedDevicesTV.text =  "${device.name} disconnected"
    }



}