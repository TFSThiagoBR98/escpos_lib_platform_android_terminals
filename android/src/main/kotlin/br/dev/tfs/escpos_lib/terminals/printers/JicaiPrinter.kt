package br.dev.tfs.escpos_lib.terminals.printers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import br.dev.tfs.escpos_lib.terminals.getBitmapFromHtml
import br.dev.tfs.escpos_lib.terminals.interfaces.PrinterService
import br.dev.tfs.escpos_lib.terminals.processBitmapForPrint
import com.iposprinter.iposprinterservice.IPosPrinterService
import com.iposprinter.iposprinterservice.IPosPrinterCallback
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

internal class JicaiPrinter(private val applicationContext: Context, binaryMessenger: BinaryMessenger): PrinterService {
    private var mPrinterService: IPosPrinterService? = null
    private var mCallback: IPosPrinterCallback? = null

    private var channel: MethodChannel = MethodChannel(binaryMessenger, CHANNEL)
    override fun start() {
        channel.setMethodCallHandler(this)
        if (isAvaliable(applicationContext)) {
            Log.w(TAG, "Jicai System is Avaliable: Enabling")
            startPrinterService(null);
        } else {
            Log.w(TAG, "Jicai System is NOT Avaliable: Disabling")
        }
    }

    override fun startPrinterService(result: MethodChannel.Result?) {
        Log.i(TAG, "Starting Jicai Service")
        this.mCallback = object : IPosPrinterCallback.Stub() {
            override fun onReturnString(result: String) {
                Log.w(TAG, "onReturnString($result)")
            }

            override fun onRunResult(isSuccess: Boolean) {
                Log.i(TAG, "onComplete: $isSuccess")
            }
        }
        val intent = Intent()
        intent.setPackage("com.iposprinter.iposprinterservice")
        intent.action = "com.iposprinter.iposprinterservice.IPosPrintService"

        applicationContext.startService(intent)
        applicationContext.bindService(intent, object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
                this@JicaiPrinter.mPrinterService = null
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                this@JicaiPrinter.mPrinterService = IPosPrinterService.Stub.asInterface(service)
            }
        }, Context.BIND_AUTO_CREATE)
    }

    override fun printBitmap(bitmap: Bitmap, result: MethodChannel.Result?) {
        mPrinterService!!.printerInit(mCallback)
        processBitmapForPrint(bitmap) {
            mPrinterService!!.printBitmap(1, 16, it, mCallback)
        }
        mPrinterService!!.printerPerformPrint(1, mCallback);
    }

    override fun printWrapPaper(n: Int, result: MethodChannel.Result?) {
        mPrinterService!!.printerFeedLines(n, mCallback)
    }

    override fun initPrinter(result: MethodChannel.Result?) {
        mPrinterService!!.printerInit(mCallback)
    }

    override fun printHtml(htmlContent: String, result: MethodChannel.Result?) {
        Log.i(TAG, "Printing...")
        mPrinterService!!.printerInit(mCallback)
        mPrinterService!!.printText("Teste de impressao", mCallback)
        mPrinterService!!.printerFeedLines(300, mCallback)
        getBitmapFromHtml(htmlContent, applicationContext, {
            Log.i(TAG, "Printing image...")
            mPrinterService!!.printBitmap(1, 10, it, mCallback)
        }, {
            mPrinterService!!.printerPerformPrint(160, mCallback);
        })
        Log.i(TAG, "Printing done")
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
                    result.success(isAvaliable(applicationContext))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to run ${call.method}", e);
                    result.error("sdkError", e.message, e.stackTrace.toString());
                }
            }
            else -> result.notImplemented()
        }
    }

    companion object {
        fun isAvaliable(context: Context): Boolean {
            return try {
                val packageManager: PackageManager = context.packageManager

                packageManager.getPackageInfo(
                    iPosServicePkg, 0
                )

                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        private const val iPosServicePkg: String = "com.iposprinter.iposprinterservice"
        private const val TAG: String = "JicaiPrinter"

        private const val CHANNEL = "br.dev.tfs.escpos_lib/terminals/jicai"
    }
}