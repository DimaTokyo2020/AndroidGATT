package com.dk.gattserver.nordic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.dk.gattserver.Constance
import com.dk.gattserver.Constance.NAX_ANDROID_MTU
import no.nordicsemi.android.ble.BleManager
import java.util.*


private const val TAG = "BleClientMngr"


/**
 * Here we implement the nordic ble client mngr
 */
class BleClientMngr(
    context: Context,
    private val mListener : GattClientConnectionListener
    ) : BleManager(context) {





    interface GattClientConnectionListener{
        fun connectedToGATT(device: BluetoothDevice)
        fun failedConnectingToGatt(device: BluetoothDevice)
        fun successfullySubscribe(uuid: UUID)
        fun failedSubscribe(uuid: UUID)
        fun disconnected()
    }


    //region << implementation >>
    override fun getGattCallback(): BleManagerGattCallback {
        return GattCallback()
    }

//    override fun log(priority: Int, message: String) {
//        Log.d(TAG, "log: $message")
//        if (BuildConfig.DEBUG || priority == Log.ERROR) {
//            Log.println(priority, " bleClint: TAG", message)
//        }
//    }
    //endregion


    /**
     * Connecting to GATT server
     */
    @SuppressLint("MissingPermission")
    fun connectToDeviceGATT(device: BluetoothDevice) {

        Log.d(TAG, "Adding device: ${device.name}")
        connect(device)
            .retry(3 /* times, with */, 100 /* ms interval */)
            .useAutoConnect(true)
            .timeout(15_000 /* ms */)
            .fail { device, _ -> mListener.failedConnectingToGatt(device) }
            .then { device -> mListener.connectedToGATT(device) }
            .enqueue()


    }


    /**
     * Implements the GATT callback
     */
    private inner class GattCallback : BleManagerGattCallback() {
        private var myCharacteristic: BluetoothGattCharacteristic? = null


        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            Log.d(TAG, "isRequiredServiceSupported")
            // looking for our service in device
            val service = gatt.getService(Constance.MY_SERVICE_UUID)

            //looking for specific characteristics
            myCharacteristic =
                service?.getCharacteristic(Constance.MY_CHARACTERISTIC_UUID)

            //getting characteristics properties
            val myCharacteristicProperties = myCharacteristic?.properties ?: 0


            return (myCharacteristicProperties and BluetoothGattCharacteristic.PROPERTY_READ != 0) &&
                    (myCharacteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
        }


        /**
         * Before connecting to gatt we initializing notifications callback
         */
        override fun initialize() {
            Log.d(TAG, "initialize:")


            /* Here we will receive the data from GATT server*/
            setNotificationCallback(myCharacteristic)
                .with { _, data ->
                    if (data.value != null) {
                        val value = String(data.value!!, Charsets.UTF_8)
                        Log.d(TAG, "Received: $value")
                    }
                }


            /* Here we subscribing for notifications on Characteristics*/
            beginAtomicRequestQueue().add(requestMtu(/* set packages size */NAX_ANDROID_MTU))
                .add(enableNotifications(myCharacteristic)
                    .fail { _: BluetoothDevice?, status: Int ->
                        log(Log.ERROR, "Could not subscribe: $status")
                        disconnect().enqueue()
                        mListener.failedSubscribe(myCharacteristic?.uuid!!)
                    }
                )
                .done {
                    log(
                        Log.INFO,
                        "Successful subscribe for notifications for characteristics: ${myCharacteristic?.uuid}"
                    )
                    mListener.successfullySubscribe(myCharacteristic?.uuid!!)
                }
                .enqueue()
        }


        /**
         * GATT disconnected
         */
        override fun onServicesInvalidated() {
            Log.d(TAG, "onServicesInvalidated !!!!!")
            myCharacteristic = null
            mListener.disconnected()
        }

    }


}