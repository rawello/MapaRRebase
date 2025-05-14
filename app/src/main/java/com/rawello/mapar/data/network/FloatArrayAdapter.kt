package com.rawello.mapar.data.network

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class FloatArrayAdapter : TypeAdapter<FloatArray>() {
    override fun write(out: JsonWriter, value: FloatArray?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.beginArray()
            for (float in value) {
                out.value(float)
            }
            out.endArray()
        }
    }

    override fun read(input: JsonReader): FloatArray {
        val floats = mutableListOf<Float>()
        input.beginArray()
        while (input.hasNext()) {
            floats.add(input.nextDouble().toFloat())
        }
        input.endArray()
        return floats.toFloatArray()
    }
}