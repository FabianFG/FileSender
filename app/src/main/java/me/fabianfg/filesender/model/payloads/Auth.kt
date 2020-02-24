package me.fabianfg.filesender.model.payloads

import me.fabianfg.filesender.model.ServerInfo

class AuthPacket(val clientName : String, val clientVersion : String)

class AuthAcceptedPacket(val receivedClientId : Int, val clientName: String, val serverInfo: ServerInfo)
class AuthDeniedPacket(val code : Int, val message : String, val serverInfo: ServerInfo)