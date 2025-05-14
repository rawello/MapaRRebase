package com.rawello.mapar.data.network

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class StringToFloatArrayAdapter : TypeAdapter<FloatArray>() {
    override fun write(out: JsonWriter, value: FloatArray?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.joinToString(","))
        }
    }

    override fun read(input: JsonReader): FloatArray {
        val string = input.nextString()
        return string.split(",").map { it.toFloat() }.toFloatArray()
    }
}