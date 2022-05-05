package com.dk.gattserver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

public class BluetoothUtils(private val mContext: Context) {



    val mBluetoothManager = mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val mBluetoothAdapter = mBluetoothManager.adapter
}