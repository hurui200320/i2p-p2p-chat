package info.skyblond.i2p.p2p.chat.core

/**
 * A place to store session related data, like username, session status, etc.
 *
 * Accessing the context will be synchronized, so your implementation don't need
 * to be thread-safe.
 *
 * @see PeerSession.useContextSync
 * */
interface SessionContext {
    val sessionSource: SessionSource

    /**
     * The auth info related to this session
     * */
    var peerInfo: PeerInfo?

    /**
     * Some meaningful name.
     * You don't want debug with log full of b32 addresses, right?
     * */
    val nickname: String?

    /**
     * If authorized, they can send message to us.
     * */
    fun isAuthed(): Boolean = peerInfo != null

    /**
     * If accepted, we can send message to them.
     * */
    fun isAccepted(): Boolean
}
