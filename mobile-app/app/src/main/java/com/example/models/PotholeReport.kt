package com.example.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonNull) return null
            if (element is JsonPrimitive) {
                return element.content
            }
            return element.toString()
        } else {
            return if (decoder.decodeNotNullMark()) decoder.decodeString() else null
        }
    }
}

object FlexibleFloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleFloatSerializer", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat(value)
    }

    override fun deserialize(decoder: Decoder): Float {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                return element.content.toFloatOrNull() ?: 0f
            }
        }
        return try {
            decoder.decodeFloat()
        } catch (e: Exception) {
            0f
        }
    }
}

@Serializable
data class PotholeReport(

    val id: String? = null,

    @SerialName("reporter_id")
    @Serializable(with = FlexibleStringSerializer::class)
    val reporterId: String? = null,

    // Database values: new, in_progress, fixed
    val status: String = "new",

    // Database values: LOW, MEDIUM, HIGH
    val severity: String = "MEDIUM",

    @Serializable(with = FlexibleStringSerializer::class)
    val address: String? = null,

    @Serializable(with = FlexibleStringSerializer::class)
    val ward: String? = null,

    @SerialName("ward_no")
    @Serializable(with = FlexibleStringSerializer::class)
    val wardNo: String? = null,

    @Serializable(with = FlexibleStringSerializer::class)
    val location: String? = null,

    @Serializable(with = FlexibleStringSerializer::class)
    val zone: String? = null,

    @SerialName("pothole_code")
    @Serializable(with = FlexibleStringSerializer::class)
    val potholeCode: String? = null,

    @SerialName("created_at")
    @Serializable(with = FlexibleStringSerializer::class)
    val createdAt: String? = null,

    @SerialName("confidence_score")
    @Serializable(with = FlexibleFloatSerializer::class)
    val confidenceScore: Float = 0f,

    @SerialName("user_confirmed")
    val userConfirmed: Boolean? = null,

    @SerialName("lat")
    val lat: Double = 0.0,

    @SerialName("lng")
    val lng: Double = 0.0,

    @SerialName("photo_url")
    @Serializable(with = FlexibleStringSerializer::class)
    val photoUrl: String? = null,

    // Local app only (not inserted into DB)
    @Serializable(with = FlexibleStringSerializer::class)
    val description: String? = null,

    // Database values: citizen, auto
    val source: String = "citizen"
) {
    val latitude: Double
        get() = lat

    val longitude: Double
        get() = lng

    val detectionMethod: String
        get() = source
}
