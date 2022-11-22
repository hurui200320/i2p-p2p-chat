package info.skyblond.i2p.p2p.chat.json

import com.dampcake.bencode.Bencode
import com.dampcake.bencode.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*
import kotlin.reflect.jvm.jvmName

/**
 * Treat [UUID] as string: `12345678-90ab-cdef-1234-567890abcdef`
 * */
object UUIDAsStringSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID =
        UUID.fromString(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeString(value.toString())
}

/**
 * Treat [Map]<String, String> as bencode.
 * */
object MapAsBencodeSerializer : KSerializer<Map<String, String>> {
    override val descriptor = PrimitiveSerialDescriptor("Map<String, *>", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Map<String, String> =
        Bencode().decode(decoder.decodeString().encodeToByteArray(), Type.DICTIONARY)
            .mapValues {
                if (it.value !is String) {
                    throw IllegalArgumentException("Expecting String as value, but got ${it.value::class.jvmName}")
                }
                it.value as String
            }

    override fun serialize(encoder: Encoder, value: Map<String, String>) =
        encoder.encodeString(Bencode().encode(value).decodeToString())
}


