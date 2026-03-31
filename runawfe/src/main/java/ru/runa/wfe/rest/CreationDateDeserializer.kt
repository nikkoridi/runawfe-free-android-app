package ru.runa.wfe.rest

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CreationDateDeserializer: StdDeserializer<OffsetDateTime>(OffsetDateTime::class.java)  {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneOffset.UTC)

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): OffsetDateTime {
        return LocalDateTime.parse(p?.text, dateFormatter)
                .atOffset(OffsetDateTime.now().offset)
    }
}