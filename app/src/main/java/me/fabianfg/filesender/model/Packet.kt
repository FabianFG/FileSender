package me.fabianfg.filesender.model

import com.google.gson.JsonElement

data class Packet(var className : String, var payload : JsonElement)