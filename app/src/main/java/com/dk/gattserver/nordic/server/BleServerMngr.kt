package com.dk.gattserver.nordic.server

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.dk.gattserver.BuildConfig
import com.dk.gattserver.Constance
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleServerManager
import no.nordicsemi.android.ble.data.DataSplitter
import no.nordicsemi.android.ble.observer.ServerObserver
import java.nio.charset.StandardCharsets
import java.util.*

/*
    * Manages the entire GATT service, declaring the services and characteristics on offer
    */
public class BleServerMngr(val context: Context) : BleServerManager(context), ServerObserver {

    companion object {
        val TAG = BleServerMngr::class.java.simpleName

        private val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID =
            Constance.MY_SERVICE_UUID
    }

    private val myGattCharacteristic = sharedCharacteristic(
        // UUID:
        Constance.MY_CHARACTERISTIC_UUID,
        // Properties:
        BluetoothGattCharacteristic.PROPERTY_READ
                or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        // Permissions:
        BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM,
        // Descriptors:
        // cccd() - this could have been used called, had no encryption been used.
        // Instead, let's define CCCD with custom permissions:
        descriptor(
            CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM
                    or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM,
            byteArrayOf(0, 0)
        ),
        description("A characteristic to be read", false) // descriptors
    )


    private val myGattService = service(
        // UUID:
        Constance.MY_SERVICE_UUID,
        // Characteristics (just one in this case):
        myGattCharacteristic
    )

    private val myGattServices = Collections.singletonList(myGattService)


    private val serverConnections = mutableMapOf<String, ServerConnection>()


    fun setMyCharacteristicValue(value: String) {
        Log.d(TAG, "setMyCharacteristicValue: $value")
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        myGattCharacteristic.value = bytes
        serverConnections.values.forEach { serverConnection ->
            serverConnection.sendNotificationForMyGattCharacteristic(bytes)
        }
    }

    override fun log(priority: Int, message: String) {
        Log.d(TAG, "log: $message")
        if (BuildConfig.DEBUG || priority == Log.ERROR) {
            Log.println(priority, "ble clint TAG", message)
        }
    }

    override fun initializeServer(): List<BluetoothGattService> {
        Log.d(TAG, "initializeServer: ")
        setServerObserver(this)

        return myGattServices
    }

    override fun onServerReady() {
        Log.d(TAG, "Gatt server ready")
    }

    override fun onDeviceConnectedToServer(device: BluetoothDevice) {
        Log.d(TAG, "Device connected ${device.address}")

        // A new device connected to the phone. Connect back to it, so it could be used
        // both as server and client. Even if client mode will not be used, currently this is
        // required for the server-only use.
        serverConnections[device.address] = ServerConnection().apply {
            useServer(this@BleServerMngr)
            connect(device).enqueue()
        }
    }

    override fun onDeviceDisconnectedFromServer(device: BluetoothDevice) {
        Log.d(TAG, "Device disconnected ${device.address}")

        // The device has disconnected. Forget it and close.
        serverConnections.remove(device.address)?.close()
    }

    /*
     * Manages the state of an individual server connection (there can be many of these)
     */
    inner class ServerConnection : BleManager(context) {

        private val TAG = ServerConnection::class.java.simpleName
        private var gattCallback: GattCallback? = null

        fun sendNotificationForMyGattCharacteristic(value: ByteArray) {
            Log.d(TAG, "setMyCharacteristicValue: $value")
//            writeCharacteristic(myGattCharacteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE).split().enqueue()
            sendNotification(myGattCharacteristic, value).split().enqueue()
        }

        override fun log(priority: Int, message: String) {
            Log.d(TAG, "setMyCharacteristicValue: $message")
            this@BleServerMngr.log(priority, message)
        }

        override fun getGattCallback(): BleManagerGattCallback {
            Log.d(TAG, "getGattCallback: ")
            gattCallback = GattCallback()
            return gattCallback!!
        }



        private inner class GattCallback() : BleManagerGattCallback() {

            // There are no services that we need from the connecting device, but
            // if there were, we could specify them here.
            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                Log.d(TAG, "isRequiredServiceSupported()")
                return true
            }

            override fun onServicesInvalidated() {
                // This is the place to nullify characteristics obtained above.
                Log.d(TAG, "onServicesInvalidated()")
            }

        }
    }
}
