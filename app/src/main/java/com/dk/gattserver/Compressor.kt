package com.dk.gattserver

import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

object Compressor {


    val maxSize = 512


    public fun strToBytePackages(str: String): Array<ByteArray> {
        return splitByreArr(compress(str))!!
    }

    fun compress(str: String): ByteArray {
        return str.encodeToByteArray()
    }

    fun splitByreArr(byteArr: ByteArray): Array<ByteArray> {
        return divideArray(byteArr, maxSize)!!
    }

    fun split(input: ByteArray, maxSize: Int): List<ByteArray>? {
        val bb = ByteBuffer.wrap(input)
        val arrayList = ArrayList<ByteArray>()
        if (input.size < maxSize) {
            arrayList.add(input)
            return arrayList
        } else {
            var splitByteNum = 0
            while (splitByteNum < input.size) {
                var bytePackage = ByteArray(maxSize)

                if (input.size - splitByteNum < maxSize) {
                    bytePackage = ByteArray(input.size - splitByteNum)
                }
                bb.get(bytePackage, splitByteNum, bytePackage.size)
                arrayList.add(bytePackage)
                splitByteNum += bytePackage.size - 1
            }

            return arrayList
        }
    }


    public fun connectByteArrs(byteArr1: ByteArray, byteArr2: ByteArray): ByteArray {
        val allByteArray = ByteArray(byteArr1.size + byteArr2.size)

        val buff: ByteBuffer = ByteBuffer.wrap(allByteArray)
        buff.put(byteArr1)
        buff.put(byteArr2)

        return buff.array()
    }


    //decompress
    public fun decompress(byteArr: ByteArray): String {
        return byteArr.decodeToString()
    }


    fun divideArray(source: ByteArray, chunksize: Int): Array<ByteArray>? {
        val ret = Array(Math.ceil(source.size / chunksize.toDouble()).toInt()) { ByteArray(chunksize) }
        var start = 0
        for (i in ret.indices) {
            ret[i] = Arrays.copyOfRange(source, start, start + chunksize)
            start += chunksize
        }
        return ret
    }
}