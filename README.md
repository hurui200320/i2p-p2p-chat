# i2p-p2p-chat
A lib for P2P chat using I2P.

## What is it?

It's a separate lib for operating I2P in a P2P way. Mainly for [hurui200320/MinecraftChatAlternative](https://github.com/hurui200320/MinecraftChatAlternative),
a Minecraft mod that replace the whole in-game chat with a I2P based P2P chat system.

This lib can be used outside the Minecraft mod world. It uses kotlinx serialization,
kotlin reflection, bencode and I2P things. 

## How to use it?

See [jitpack](https://jitpack.io/#info.skyblond/i2p-p2p-chat) for including this lib
into your project.

See [I2P's tutorial](https://geti2p.net/en/get-involved/develop/applications) for
general ideas of I2P application developing. You need to know the basics of I2P's
streaming library, which is the core of this project.

See [examples](https://github.com/hurui200320/i2p-p2p-chat/tree/master/src/test/kotlin/info/skyblond/i2p/p2p/chat/example)
on how to use this lib. It shows you how to create a peer, send message, handle
incoming messages, and PEX (like the BitTorrent's PEX, which peers can exchange their known peers).

I'm too lazy to write a document, and there are plenty comments in the code. If there
are something unclear, just open an issue, you don't need GitHub pro to open issues, LOL.

About the license: I always use AGPLv3, but if you want/need a special license (like MIT, or something else),
open an issue. Unless you're going to use it for commercial (like earning thousands dollars per month from it),
it should be free :)
