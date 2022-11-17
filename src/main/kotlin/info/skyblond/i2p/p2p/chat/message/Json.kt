package info.skyblond.i2p.p2p.chat.message

import kotlinx.serialization.modules.*

fun getSerializersModule(
    customMessagePayload: PolymorphicModuleBuilder<MessagePayload>.() -> Unit = {},
    customSerializersModule: SerializersModuleBuilder.() -> Unit = {},
): SerializersModule = SerializersModule {
    polymorphic(MessagePayload::class) {
        subclass(AuthRequest::class)
        subclass(TextMessageRequest::class)
        subclass(PEXRequest::class)
        subclass(ByeRequest::class)
        subclass(NoContentReply::class)
        subclass(AuthAcceptedReply::class)
        subclass(PEXReply::class)
        subclass(InvalidIncomingDataError::class)
        subclass(AuthenticationFailedError::class)
        subclass(UnauthorizedError::class)
        subclass(InvalidScopeError::class)
        subclass(UnsupportedMessageError::class)
        customMessagePayload()
    }
    customSerializersModule()
}
