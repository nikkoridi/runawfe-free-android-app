package ru.runa.wfe.rest.dto

import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class CustomLongDeserializer(private val formatter:
                       DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME):
    JsonDeserializer<Long>() {

        init {
            Log.i(this.javaClass.toString(), "Deserializer enabled")
        }

    @Throws(IOException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Long {
        val value = p.readValueAsTree<JsonNode>()
        Log.i(this.javaClass.toString(), "Deserializer works")
        if (value.isLong) value.longValue()
        else if (value.isTextual) {
                try {
                    val date = OffsetDateTime.parse(value.toString(), formatter)
                    return date.toInstant().toEpochMilli()
                }
                catch (ex: DateTimeParseException) {
                    throw JsonSyntaxException(ex)
                }
        }

        return (ctxt!!.handleWeirdStringValue(
                Long::class.java, value.toString(),
                "not a valid `java.lang.Long` value"
            ) as Long)
    }
}