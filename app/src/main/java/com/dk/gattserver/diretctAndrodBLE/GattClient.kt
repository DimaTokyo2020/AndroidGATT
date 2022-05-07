package com.dk.gattserver.diretctAndrodBLE

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.dk.gattserver.BluetoothUtils
import com.dk.gattserver.Compressor
import com.dk.gattserver.Constance

public class GattClient(private val mContext: Context, private val mListener: ConnectListener) {

    private val TAG = GattClient::javaClass.name
    var characteristicRequest: BluetoothGattCharacteristic? = null
    var mUtils: BluetoothUtils = BluetoothUtils(mContext)
    lateinit var mGatt: BluetoothGatt
    val mHandler: Handler
    var mLastMsgIndex = -1
    val mMsgArr = arrayOf("0", "100", "200", "300", "400", "500")


    var chunksSize = 0
    var byteArrayMsg: ByteArray? = null


    interface ConnectListener {
        fun connected()
        fun disconnected()
    }


    init {
        val handlerThread = HandlerThread("Client_Gatt_Thread")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        mHandler.postDelayed({ connect() }, 100)
    }


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mGatt = gatt!!
//                runOnUiThread { gatt?.discoverServices() }
                gatt.discoverServices()
                Log.d(TAG, "successfully connected to the GATT Server")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "disconnected from the GATT Server")
                mListener.disconnected()
                Log.d(TAG, "Call connection again from the GATT Server")
                mHandler.postDelayed({ connect() }, 5000)
            } else {
                Log.d(TAG, "Call connection again from the GATT Server")
                mHandler.postDelayed({ connect() }, 5000)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "Discovered services callBack")
            gatt?.setCharacteristicNotification(
                gatt?.getService(Constance.SERIAL_SERVICE)
                    ?.getCharacteristic(Constance.SERIAL_MAIN), true
            )
            gatt?.setCharacteristicNotification(
                gatt?.getService(Constance.SERIAL_SERVICE)
                    ?.getCharacteristic(Constance.SERIAL_CHUNKS), true
            )

            characteristicRequest =
                gatt?.getService(Constance.SERIAL_SERVICE)
                    ?.getCharacteristic(Constance.SERIAL_REQUEST)

            mListener.connected()
            mHandler.postDelayed({ sendNextMsg() }, 1000)
        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.d(TAG, "onCharacteristicWrite")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {//max 512 byte
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d(TAG, "onCharacteristicChanged")



            if (characteristic?.uuid == Constance.SERIAL_MAIN) {

                val msgFromServer = characteristic?.value?.decodeToString()
                val splitedMsg = msgFromServer?.split(",")
                if(mMsgArr[mLastMsgIndex] == splitedMsg?.get(0)){
                    chunksSize = splitedMsg?.get(1).toInt()
                }else{
                    Log.w(TAG,"Wrong msg recieved!!")
                    mHandler.post { sendRequest(mMsgArr[mLastMsgIndex]) }
                }


            } else if (characteristic?.uuid == Constance.SERIAL_CHUNKS) {
                chunksSize--
                if (byteArrayMsg == null) {
                    byteArrayMsg = characteristic?.value
                } else {
                    byteArrayMsg =
                        Compressor.connectByteArrs(byteArrayMsg!!, characteristic?.value!!)
                }
                if (chunksSize == 0) {
                    Log.d(TAG, "From server: ${byteArrayMsg?.decodeToString()}")
                    byteArrayMsg = null
                    mHandler.postDelayed({sendNextMsg()},5000)
                }
            }

        }
    }

    private fun sendNextMsg() {
        mLastMsgIndex++
        if (mLastMsgIndex < mMsgArr.size ){
            sendRequest(mMsgArr[mLastMsgIndex])
        }
    }

    @SuppressLint("MissingPermission")
    fun sendRequest(str: String) {
        mHandler.post(Runnable {
            characteristicRequest?.setValue(str)
            mGatt.writeCharacteristic(characteristicRequest)
        })
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        val devices = mUtils.mBluetoothAdapter.bondedDevices.iterator()

        if (devices.hasNext()) {
            devices.next().connectGatt(mContext, false, bluetoothGattCallback)
        } else {
            mHandler.postDelayed({ connect() }, 5000)
        }
    }

}