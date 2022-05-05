package com.dk.gattserver.nordic.server

interface DeviceAPI {
    /**
     * Change the value of the GATT characteristic that we're publishing
     */
    fun setMyCharacteristicValue(value: String)
}