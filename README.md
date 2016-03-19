# IRCBridge
Minecraft Mod. Serverside IRC Bridge for Minecraft Forge; successor to the server version of EiraIRC

## Goals
* Simple and fast setup via the /ircbridge command
* Highly configurable through the config file
* Server-side *only* IRC Bridge
* Lightweight and backwards compatible: no external dependencies

## Useful Links
* Latest Builds on my Jenkins
* @BlayTheNinth on Twitter

## Afterword
We all know EiraIRC has become crazy bloated to the point where it simply wasn't fun developing it anymore.
It started out as a server-side mod for my Minecraft server back in Minecraft 1.5.2 (year 2013).
Then, client-side support was added (because why not? it wasn't hard to do, after all!).
Then, Twitch support was added (I mean, it does provide an IRC interface...).
Then, screenshot management was added (just...because it's cool).
Then... yeah. I think you get the point.

Making this mod server-side only brings a bunch of benefits. First of all, and IRC operators will probably like this,
it is impossible for people to configure their client-side IRC badly and spam channels with their death messages.
It also allows concentrating on things that are beneficial for a server-side environment in a clean code base that does
exactly, and only, what it should be doing. There's no need to spend time designing GUIs that are only ever used once.
The setup process can be designed under the knowledge that, with this mod installed, the user is most likely trying to
link an IRC channel to this server's chat.

That said, a separate client-side IRC mod is coming soonish - and at that point, we can erase the way too overly hated
EiraIRC from all our minds.