package com.dk.gattserver.nordic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.dk.gattserver.BuildConfig
import com.dk.gattserver.Constance
import com.dk.gattserver.Constance.END_OF_MSG
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.DataMerger
import no.nordicsemi.android.ble.data.DataStream
import java.util.*

class BleClientMngr(context: Context) : BleManager(context) {

    val TAG = BleClientMngr::class.java.simpleName


    @SuppressLint("MissingPermission")
    fun addDevice(device: BluetoothDevice) {

        Log.d(TAG, "Adding device: ${device.name}")
        connect(device)
            .retry(3 /* times, with */, 100 /* ms interval */)
            .useAutoConnect(true)
            .timeout(15_000 /* ms */)
            .done { device -> Log.d("TAG", "Done") }
            .fail { device, status -> Log.d("TAG", "Failed $status") }
            .then { device -> Log.d("TAG", "then") }
            .enqueue()


    }


    override fun getGattCallback(): BleManagerGattCallback {
        return GattCallback()
    }


    override fun log(priority: Int, message: String) {
        Log.d(TAG, "log: $message")
        if (BuildConfig.DEBUG || priority == Log.ERROR) {
            Log.println(priority, " bleClint: TAG", message)
        }
    }


    private inner class GattCallback : BleManagerGattCallback() {
        private val TAG = GattCallback::class.simpleName
        private var myCharacteristic: BluetoothGattCharacteristic? = null


        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            Log.d(TAG, "isRequiredServiceSupported")
            val service = gatt.getService(Constance.MY_SERVICE_UUID)
            myCharacteristic =
                service?.getCharacteristic(Constance.MY_CHARACTERISTIC_UUID)
            val myCharacteristicProperties = myCharacteristic?.properties ?: 0
            return (myCharacteristicProperties and BluetoothGattCharacteristic.PROPERTY_READ != 0) &&
                    (myCharacteristicProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
        }

        override fun initialize() {
            Log.d(TAG, "initialize:")

            val dataMerger = object :DataMerger{
                override fun merge(
                    output: DataStream,
                    lastPacket: ByteArray?,
                    index: Int
                ): Boolean {
                    Log.d(TAG,"DataMerge value: ${lastPacket?.decodeToString()}")
                    val isLastMsg = lastPacket?.decodeToString()?.contains(END_OF_MSG)
                    if (isLastMsg != null && isLastMsg == false){
                        output.write(lastPacket)
                    }
                    return (isLastMsg != null && isLastMsg == true)
                }
            }

            setNotificationCallback(myCharacteristic)
                .merge(dataMerger)
                .with { _, data ->
                if (data.value != null) {
                    val value = String(data.value!!, Charsets.UTF_8)
                    Log.d(TAG, "Received: $value")
//                    defaultScope.launch {
//                        myCharacteristicChangedChannel?.send(value)
//                    }
                }
            }

            beginAtomicRequestQueue().add(requestMtu(/* SetMTU*/512))
                .add(enableNotifications(myCharacteristic)
                    .fail { _: BluetoothDevice?, status: Int ->
                        log(Log.ERROR, "Could not subscribe: $status")
                        disconnect().enqueue()
                    }
                )
                .done {
                    log(
                        Log.INFO,
                        "Successful subscribe for notifications for characteristics: ${myCharacteristic?.uuid}"
                    )
                }
                .enqueue()
        }


        override fun onServicesInvalidated() {
            Log.d(TAG,"onServicesInvalidated !!!!!")
            myCharacteristic = null
        }

    }


}