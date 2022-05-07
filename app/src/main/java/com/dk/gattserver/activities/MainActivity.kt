package com.dk.gattserver.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dk.gattserver.BluetoothUtils
import com.dk.gattserver.Constance
import com.dk.gattserver.R
import com.dk.gattserver.nordic.BleClientMngr
import com.dk.gattserver.nordic.server.BleAdvertiser
import com.dk.gattserver.nordic.server.BleServerMngr
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {


    private val          TAG:                         String = MainActivity::javaClass.name
    private var          mBleMngr:                    BleClientMngr? = null
    private lateinit var mUtils:                      BluetoothUtils
    private lateinit var serverManager:               BleServerMngr
    private var          bleAdvertiseCallback:        BleAdvertiser.Callback? = null





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

        mBleMngr = BleClientMngr(this, object:BleClientMngr.GattClientConnectionListener{

            override fun connectedToGATT(device: BluetoothDevice) {
                Log.i(TAG, "Connected to gatt: ${device.name}" )
            }

            override fun failedConnectingToGatt(device: BluetoothDevice) {
                Log.i(TAG, "Failed connecting to gatt: ${device.name}" )
            }

            override fun successfullySubscribe(uuid: UUID) {
                Log.i(TAG, "successfully subscribe to : $uuid" )
            }

            override fun failedSubscribe(uuid: UUID) {
                Log.i(TAG, "filed subscribe to : $uuid" )
            }

            override fun disconnected() {
                Log.i(TAG, "Disconnected" )
            }

        }

            )
        BluetoothUtils(this).mBluetoothAdapter?.bondedDevices
            ?.forEach { device ->
                Log.d(TAG, "Found connected device: ${device.name}")
                mBleMngr?.connectToDeviceGATT(device)
            }

    }

    @SuppressLint("MissingPermission")
    fun testServerNordic() {
        serverManager = BleServerMngr(this)
        serverManager.open()

        bleAdvertiseCallback = BleAdvertiser.Callback()//TODO implement

        mUtils.mBluetoothAdapter.bluetoothLeAdvertiser?.startAdvertising(
            BleAdvertiser.settings(),
            BleAdvertiser.advertiseData(),
            bleAdvertiseCallback!!
        )

    }


    var msg = "My message___1"
    private fun testSend(){
        msg +=msg
        serverManager.setMyCharacteristicValue(msg)
        serverManager.setMyCharacteristicValue(Constance.END_OF_MSG)
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