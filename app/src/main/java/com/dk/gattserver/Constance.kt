package com.dk.gattserver

import java.util.*

object Constance {
    val SERIAL_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    val SERIAL_MAIN = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    val SERIAL_CHUNKS = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
    val SERIAL_REQUEST = UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb")



    val MY_SERVICE_UUID: UUID = UUID.fromString("80323644-3588-4F0B-A53B-CF494ECEAAB3")
    val MY_CHARACTERISTIC_UUID: UUID = UUID.fromString("80323644-3537-466B-A53B-CF494ECEAAB3")


    const val END_OF_MSG = "@END@"
    const val NAX_ANDROID_MTU = 512
}