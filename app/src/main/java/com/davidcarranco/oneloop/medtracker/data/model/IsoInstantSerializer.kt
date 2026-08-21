package com.davidcarranco.oneloop.medtracker.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Matches iOS `JSONEncoder.dateEncodingStrategy = .iso8601`.
 */
object IsoInstantSerializer : KSerializer<Instant> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IsoInstant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        val raw = decoder.decodeString()
        return try {
            Instant.parse(raw)
        } catch (_: DateTimeParseException) {
            Instant.ofEpochMilli(raw.toLongOrNull() ?: Instant.now().toEpochMilli())
        }
    }
}
