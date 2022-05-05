package com.dk.gattserver

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelUuid
import android.util.Log
import com.dk.gattserver.Constance.SERIAL_CHUNKS
import com.dk.gattserver.Constance.SERIAL_MAIN

class GattServer(private val mContext: Context, private val mListener: ConnectionListener) {


    private val TAG = GattServer::javaClass.name
    private val mUtils = BluetoothUtils(mContext)

    lateinit var service: BluetoothGattService
    lateinit var mBluetoothGattServer: BluetoothGattServer
    var mConnectedDevice: BluetoothDevice? = null
    val responseQueue = arrayOf("1", "2", "3", "4", "5", "6","7")
    private var lastResponseIndex = -1
    private val mHandler: Handler


    init {
        val handlerThread = HandlerThread("GattServer_Thread")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        initGattServer()
    }

    interface ConnectionListener {
        fun deviceConnected(device: BluetoothDevice)
        fun deviceDisconnected(device: BluetoothDevice)
    }


    //I would like to quickly mention, that most of the Android devices in use don't support the BluetoothGattServer.
// However newer models have that capability more and more.
//
//First of all you probably want to advertise your service, so that other devices know about the server.
// For that we need to create the AdvertiseSettings, the AdvertiseData and the AdvertiseData for ScanResponses.
    @SuppressLint("MissingPermission")
    private fun initGattServer() {
        createAdvertise()
        openGattServer()
        initServiceAndCharacteristics()
    }

    @SuppressLint("MissingPermission")
    private fun initServiceAndCharacteristics() {
        service = BluetoothGattService(
            Constance.SERIAL_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

//add a read characteristic.
        val characteristic = BluetoothGattCharacteristic(
            Constance.SERIAL_MAIN,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristic)

        val characteristicChunks = BluetoothGattCharacteristic(
            Constance.SERIAL_CHUNKS,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristicChunks)


        val characteristicRequest = BluetoothGattCharacteristic(
            Constance.SERIAL_REQUEST,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicRequest)
        mBluetoothGattServer.addService(service)
    }


    @SuppressLint("MissingPermission")
    private fun openGattServer() {
        //For the BluetoothGattServer we need first to create a BluetoothGattServerCallback:
        val callback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice?,
                status: Int,
                newState: Int
            ) {

                super.onConnectionStateChange(device, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mConnectedDevice = device
                    Log.d(
                        TAG,
                        "onConnectionStateChange device: [ ${device?.name} ] -> [ connected ]"
                    )
                    mListener.deviceConnected(device!!)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mConnectedDevice = null
                    Log.d(
                        TAG,
                        "onConnectionStateChange device: [ ${device?.name} ] -> [ disconnected ]"
                    )
                    mListener.deviceDisconnected(device!!)
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                super.onServiceAdded(status, service)
                Log.d(TAG, "onServiceAdded")
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                Log.d(TAG, "onCharacteristicReadRequest")
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Log.d(TAG, "onCharacteristicWriteRequest")
                Log.d(TAG, "Received: ${value?.decodeToString()}")




                mBluetoothGattServer.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS, 0, "dimoon".encodeToByteArray()
                )

                val response = getNextResponse()
                val responseChunks = Compressor.compress(response)

                mBluetoothGattServer.getService(Constance.SERIAL_SERVICE).getCharacteristic(
                    SERIAL_MAIN
                ).setValue("${value?.decodeToString()},${responseChunks.size}")
                Log.d(TAG,"Send msg: ${value?.decodeToString()},${responseChunks.size}")
                mBluetoothGattServer.notifyCharacteristicChanged(
                    mConnectedDevice,
                    mBluetoothGattServer.getService(Constance.SERIAL_SERVICE).getCharacteristic(
                        Constance.SERIAL_MAIN
                    ),
                    true
                )
                mHandler.postDelayed({sendChunkChunks(response)},100)
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Log.d(TAG, "onDescriptorWriteRequest")
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor?
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)
                Log.d(TAG, "onDescriptorReadRequest")
            }

            override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
                super.onNotificationSent(device, status)
                Log.d(TAG, "onNotificationSent")
            }

            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                super.onMtuChanged(device, mtu)
                Log.d(TAG, "onMtuChanged")
            }

            override fun onExecuteWrite(
                device: BluetoothDevice?,
                requestId: Int,
                execute: Boolean
            ) {
                super.onExecuteWrite(device, requestId, execute)
                Log.d(TAG, "onExecuteWrite")
            }
        }

        //You don't need to implement all of those methods, only those you are interested in.
        // For example you could implement the onCharacteristicReadRequest(...) method to return
        // data to a device reading the characteristic on your BluetoothGattServer.
        //
        //After that you can open a GattServer, create your service and add the service to the server:
        mBluetoothGattServer = mUtils.mBluetoothManager.openGattServer(mContext, callback)
    }

    private fun getNextResponse(): String {
        lastResponseIndex++
        return responseQueue[lastResponseIndex]
    }


    @SuppressLint("MissingPermission")
    private fun createAdvertise() {
        val settings: AdvertiseSettings = AdvertiseSettings.Builder()
            .setConnectable(true)
            .build()
        val advertiseData: AdvertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .build()
        val scanResponseData: AdvertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(Constance.SERIAL_SERVICE))
            .setIncludeTxPowerLevel(true)
            .build()


//After that you need to create a callback for the advertising status:
        val callback: AdvertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "BLE advertisement added successfully")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Failed to add BLE advertisement, reason: $errorCode")
            }
        }
//        Now you can start advertising your service:
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser =
            mUtils.mBluetoothAdapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback)

    }


    @SuppressLint("MissingPermission")
    fun sendChunkChunks(msg: String) {
        val bytePackages = Compressor.strToBytePackages(msg)
        for (bytePackage in bytePackages) {
            updateCharacteristicChunks(bytePackage)
        }
    }


    fun updateCharacteristicMain(byteArray: ByteArray) {
        mHandler.post(object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                service.getCharacteristic(SERIAL_MAIN).setValue(byteArray)
                mBluetoothGattServer.notifyCharacteristicChanged(
                    mConnectedDevice,
                    service.getCharacteristic(SERIAL_MAIN),
                    true
                )//onNotificationSent
            }
        })

    }


    fun updateCharacteristicChunks(byteArray: ByteArray) {
        mHandler.post(object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                service.getCharacteristic(SERIAL_CHUNKS).setValue(byteArray)
                mBluetoothGattServer.notifyCharacteristicChanged(
                    mConnectedDevice,
                    service.getCharacteristic(SERIAL_CHUNKS),
                    true
                )//onNotificationSent
            }
        })

    }
}