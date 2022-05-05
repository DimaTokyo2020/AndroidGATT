package com.dk.gattserver.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dk.gattserver.GattClient
import com.dk.gattserver.R
import kotlinx.android.synthetic.main.activity_client.*

class ClientActivity : AppCompatActivity(), GattClient.ConnectListener {

    lateinit var mGattClient:GattClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        mGattClient = GattClient(this, this)
    }

    override fun connected() {
        connectionIV.setImageResource(R.drawable.ic_bt_connected)
    }

    override fun disconnected() {
        connectionIV.setImageResource(R.drawable.ic_bt_disconnected)
    }
}