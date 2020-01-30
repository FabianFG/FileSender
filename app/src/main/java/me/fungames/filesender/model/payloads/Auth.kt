package me.fungames.filesender.model.payloads

import me.fungames.filesender.model.ServerInfo

class AuthPacket(val clientName : String, val clientVersion : String)

class AuthAcceptedPacket(val receivedClientId : Int, val clientName: String, val serverInfo: ServerInfo)
class AuthDeniedPacket(val code : Int, val message : String, val serverInfo: ServerInfo)