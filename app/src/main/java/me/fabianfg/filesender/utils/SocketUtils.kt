package me.fabianfg.filesender.utils

import java.net.InetSocketAddress
import java.net.Socket

fun checkHostAvailable(address : String, port : Int, timeout : Int) : Boolean {
    val s = Socket()
    s.reuseAddress = true
    return try {
        s.connect(InetSocketAddress(address, port), timeout)
        s.close()
        true
    } catch (e : Exception) {
        false
    } finally {
        try {
            s.close()
        } catch (e : Exception) {}
    }
}