package com.dk.gattserver.activities

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dk.gattserver.BluetoothUtils
import com.dk.gattserver.BuildConfig
import com.dk.gattserver.Constance
import com.dk.gattserver.Constance.MY_SERVICE_UUID
import com.dk.gattserver.R
import com.dk.gattserver.nordic.BleClientMngr
import com.dk.gattserver.nordic.server.BleAdvertiser
import com.dk.gattserver.nordic.server.BleServerMngr
import com.dk.gattserver.nordic.server.DeviceAPI
import kotlinx.android.synthetic.main.activity_main.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.observer.ServerObserver
import java.nio.charset.StandardCharsets
import java.util.*


class MainActivity : AppCompatActivity() {


    private val          TAG:                         String = MainActivity::javaClass.name
    private var          mBleMngr:                    BleClientMngr? = null
    private lateinit var mUtils:                     BluetoothUtils
    private var          serverManager:               BleServerMngr? = null
    private var          bleAdvertiseCallback:        BleAdvertiser.Callback? = null
    private lateinit var mSender:                     DataPlane





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testClient.setOnClickListener { testClientNordic() }
        testServer.setOnClickListener { testServerNordic() }
        testSendBTN.setOnClickListener { testSend() }

        mUtils = BluetoothUtils(baseContext)
    }



    @SuppressLint("MissingPermission")
    fun testClientNordic() {

        mBleMngr = BleClientMngr(this)
        BluetoothUtils(this).mBluetoothAdapter?.bondedDevices
            ?.forEach { device ->
                Log.d(TAG, "Found connected device: ${device.name}")
                mBleMngr?.addDevice(device)
            }

    }

    @SuppressLint("MissingPermission")
    fun testServerNordic() {
        serverManager = BleServerMngr(this)
        serverManager!!.open()

        bleAdvertiseCallback = BleAdvertiser.Callback()

        mUtils.mBluetoothAdapter.bluetoothLeAdvertiser?.startAdvertising(
            BleAdvertiser.settings(),
            BleAdvertiser.advertiseData(),
            bleAdvertiseCallback!!
        )

        mSender = DataPlane()
    }


    var msg = "My message___1"
    private fun testSend(){
        msg +=msg
        mSender.setMyCharacteristicValue(msg)
    }


    private inner class DataPlane : DeviceAPI {

        override fun setMyCharacteristicValue(value: String) {
            serverManager?.setMyCharacteristicValue(value)
            serverManager?.setMyCharacteristicValue(Constance.END_OF_MSG)
        }

    }













    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chooseClientBTN.setOnClickListener { startClientActivity() }
        chooseServerBTN.setOnClickListener { startServerActivity() }
    }


    fun startClientActivity(){
        val intent = Intent(this, ClientActivity::class.java)
        startActivity(intent)
    }


    fun startServerActivity(){
        val intent = Intent(this, ServerActivity::class.java)
        startActivity(intent)
    }




//
//
//
//
//
//
//
//
//    var msgNum = 1
//    var msg =
//        "dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-" +
//                "dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-" +
//                "diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-dima-diam-diam-diam-"
//
//



 */
}