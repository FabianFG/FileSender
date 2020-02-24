package me.fungames.filesender.frontend.ui.receive

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScanQrActivity : Activity(), ZXingScannerView.ResultHandler {

    companion object {
        const val SCANNED_RESULT = "me.fungames.filesender.frontend.ui.receive.ScanQrActivity.SCANNED_RESULT"
    }

    private lateinit var scannerView : ZXingScannerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scannerView = ZXingScannerView(this)
        setResult(RESULT_CANCELED)
        setContentView(scannerView)
    }

    override fun onResume() {
        super.onResume()
        scannerView.setResultHandler(this)
        scannerView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        scannerView.stopCamera()
    }

    override fun handleResult(result: Result) {
        val actResult = Intent()
        actResult.putExtra(SCANNED_RESULT, result.text)
        setResult(RESULT_OK, actResult)
        finish()
    }
}