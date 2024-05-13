package br.dev.tfs.escpos_lib.terminals.printers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import br.dev.tfs.escpos_lib.terminals.getBitmapFromHtml
import br.dev.tfs.escpos_lib.terminals.interfaces.PrinterService
import br.dev.tfs.escpos_lib.terminals.processBitmapForPrint
import com.pax.dal.IDAL
import com.pax.dal.IPrinter
import com.pax.neptunelite.api.NeptuneLiteUser
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

internal class PaxPrinter(private val applicationContext: Context, binaryMessenger: BinaryMessenger): PrinterService {
    private var dal: IDAL? = null;
    private var printer: IPrinter? = null;

    private var channel: MethodChannel = MethodChannel(binaryMessenger, CHANNEL)
    override fun start() {
        channel.setMethodCallHandler(this)
        if (isAvaliable()) {
            Log.w(TAG, "Pax System is Avaliable: Enabling")
            startPrinterService(null);
        } else {
            Log.w(TAG, "Pax System is NOT Avaliable: Disabling")
        }
    }

    override fun startPrinterService(result: MethodChannel.Result?) {
        Log.i(TAG, "Starting Pax Service")
        try {
            dal = NeptuneLiteUser.getInstance().getDal(applicationContext)
            printer = dal!!.printer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun printBitmap(bitmap: Bitmap, result: MethodChannel.Result?) {
        printer!!.init()
        processBitmapForPrint(bitmap) {
            printer!!.printBitmap(it)
        }
        printer!!.start()
    }

    override fun printWrapPaper(n: Int, result: MethodChannel.Result?) {
        for (i in 1..n) {
            printer!!.printStr("\n", null);
        }
    }

    override fun initPrinter(result: MethodChannel.Result?) {
        printer!!.init()
    }

    override fun printHtml(htmlContent: String, result: MethodChannel.Result?) {
        if (printer == null) {
            startPrinterService(result)
        }
        printer!!.init()
        getBitmapFromHtml(htmlContent, applicationContext, {
            Log.i(TAG, "Printing image...")
            printer!!.printBitmap(it)
            printer!!.start()
        }, {
            printer!!.start()
        })
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startPrinterService" -> {
                try {
                    startPrinterService(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError", e.message, e.stackTrace.toString());
                }
            }
            "printBitmap" -> {
                try {
                    val bytes = call.argument<ByteArray>("image")!!
                    val computedBitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    printBitmap(computedBitmap, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError", e.message, e.stackTrace.toString());
                }
            }
            "printWrapPaper" -> {
                try {
                    val lines = call.argument<Int>("lines")!!
                    printWrapPaper(lines, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError", e.message, e.stackTrace.toString());
                }
            }
            "printHtml" -> {
                try {
                    val html = call.argument<String>("htmlContent")!!
                    printHtml(html, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError $e", e.message, null);
                }
            }
            "initPrinter" -> {
                try {
                    initPrinter(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError", e.message, e.stackTrace.toString());
                }
            }
            "isAvaliable" -> {
                try {
                    result.success(isAvaliable())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError", e.message, e.stackTrace.toString());
                }
            }
            else -> result.notImplemented()
        }
    }

    companion object {
        fun isAvaliable(): Boolean {
            return Build.MANUFACTURER == "PAX"
        }

        private const val TAG: String = "PaxPrinter"

        private const val CHANNEL = "br.dev.tfs.escpos_lib/terminals/pax"
    }
}