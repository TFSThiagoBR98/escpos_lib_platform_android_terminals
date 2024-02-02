package br.dev.tfs.escpos_lib.terminals.printers

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import br.dev.tfs.escpos_lib.terminals.getBitmapFromHtml
import br.dev.tfs.escpos_lib.terminals.interfaces.PrinterService
import br.dev.tfs.escpos_lib.terminals.processBitmapForPrint
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.InnerResultCallback
import com.sunmi.peripheral.printer.SunmiPrinterService
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

internal class SunmiPrinter(
    private val applicationContext: Context, binaryMessenger: BinaryMessenger
) : PrinterService {
    private var channel: MethodChannel = MethodChannel(binaryMessenger, CHANNEL)

    override fun start() {
        channel.setMethodCallHandler(this)
        if (isAvaliable(applicationContext)) {
            startPrinterService(null);
        }
    }

    private var mPrinterService: SunmiPrinterService? = null
    private val mConnectionService: InnerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            mPrinterService = service
            checkSunmiPrinterService(service)
        }

        override fun onDisconnected() {
            mPrinterService = null
            sunmiPrinter = LostSunmiPrinter
        }
    }

    private val mListener: InnerResultCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            Log.i(TAG, "onRunResult($isSuccess)")
        }

        override fun onReturnString(result: String?) {
            Log.i(TAG, "onRunResult($result)")
        }

        override fun onRaiseException(code: Int, msg: String?) {
            Log.e(TAG, "onRaiseException($code, $msg)")
        }

        override fun onPrintResult(code: Int, msg: String?) {
            Log.i(TAG, "onPrintResult($code, $msg)")
        }
    }

    override fun startPrinterService(result: MethodChannel.Result?) {
        try {
            val ret = InnerPrinterManager.getInstance().bindService(
                applicationContext, mConnectionService
            )
            if (!ret) {
                sunmiPrinter = NoSunmiPrinter
            }
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }
    }

    private fun checkSunmiPrinterService(service: SunmiPrinterService) {
        var ret = false
        try {
            ret = InnerPrinterManager.getInstance().hasPrinter(service)
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }
        sunmiPrinter = if (ret) FoundSunmiPrinter else NoSunmiPrinter
    }

    override fun initPrinter(result: MethodChannel.Result?) {
        if (mPrinterService == null) {
            startPrinterService(result)
        }

        mPrinterService!!.printerInit(mListener)
    }

    override fun printWrapPaper(n: Int, result: MethodChannel.Result?) {
        if (mPrinterService == null) {
            startPrinterService(result)
        }

        mPrinterService!!.lineWrap(1, mListener)
    }

    override fun printBitmap(bitmap: Bitmap, result: MethodChannel.Result?) {
        if (mPrinterService == null) {
            startPrinterService(result)
        }
        mPrinterService!!.printerInit(mListener)
        processBitmapForPrint(bitmap) {
            mPrinterService!!.printBitmap(it, mListener)
        }
        mPrinterService!!.lineWrap(3, mListener)
        mPrinterService!!.commitPrinterBufferWithCallback(mListener)
    }

    override fun printHtml(htmlContent: String, result: MethodChannel.Result?) {
        if (mPrinterService == null) {
            startPrinterService(result)
        }
        mPrinterService!!.printerInit(mListener)
        getBitmapFromHtml(htmlContent, applicationContext, {
            Log.i(TAG, "Printing image...")
            mPrinterService!!.printBitmap( it, mListener)
            mPrinterService!!.commitPrinterBuffer()
        }, {
            mPrinterService!!.commitPrinterBuffer()
        })
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startPrinterService" -> {
                try {
                    startPrinterService(result)
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            "printBitmap" -> {
                try {
                    val bytes = call.argument<ByteArray>("image")!!
                    val computedBitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    printBitmap(computedBitmap, result)
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            "printWrapPaper" -> {
                try {
                    val lines = call.argument<Int>("lines")!!
                    printWrapPaper(lines, result)
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            "printHtml" -> {
                try {
                    val html = call.argument<String>("htmlContent")!!
                    printHtml(html, result)
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            "initPrinter" -> {
                try {
                    initPrinter(result)
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            "isAvaliable" -> {
                try {
                    result.success(isAvaliable(applicationContext))
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            else -> result.notImplemented()
        }
    }

    companion object {
        var NoSunmiPrinter = 0x00000000
        var CheckSunmiPrinter = 0x00000001
        var FoundSunmiPrinter = 0x00000002
        var LostSunmiPrinter = 0x00000003

        var sunmiPrinter = CheckSunmiPrinter

        fun isAvaliable(context: Context): Boolean {
            return try {
                val packageManager: PackageManager = context.packageManager

                packageManager.getPackageInfo(
                    "woyou.aidlservice.jiuiv5", 0
                )

                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        private const val TAG: String = "SunmiPrinter"
        private const val CHANNEL = "br.dev.tfs.escpos_lib/terminals/sunmi"
    }
}