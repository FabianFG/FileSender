package me.fabianfg.filesender.frontend.ui.send

interface HotspotInfoHolder {
    fun close()
}

class FakeHotspotInfoHolder : HotspotInfoHolder {
    override fun close() {
        //Do nothing
    }
}