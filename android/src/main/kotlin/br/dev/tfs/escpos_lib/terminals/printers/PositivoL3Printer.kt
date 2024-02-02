package br.dev.tfs.escpos_lib.terminals.printers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import br.dev.tfs.escpos_lib.terminals.getBitmapFromHtml
import br.dev.tfs.escpos_lib.terminals.interfaces.PrinterService
import br.dev.tfs.escpos_lib.terminals.processBitmapForPrint
import com.xcheng.printerservice.IPrinterCallback
import com.xcheng.printerservice.IPrinterService
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

internal class PositivoL3Printer(private val context: Context, binaryMessenger: BinaryMessenger) :
    PrinterService {
    var totalLen = 0L
    var currentLen = 0L
    var realTotalLen = 0.0
    var realCurrentLen = 0.0
    private val mListener: PrinterManagerListener? = null
    private var mCallback: IPrinterCallback? = null
    private var mPrinterService: IPrinterService? = null

    private var channel: MethodChannel = MethodChannel(binaryMessenger, CHANNEL)
    override fun start() {
        channel.setMethodCallHandler(this)

        if (isAvaliable()) {
            startPrinterService(null);
        }
    }

    interface PrinterManagerListener {
        fun onServiceConnected()
        fun onServiceDisconnected()
    }

    private val mConnectionService: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            this@PositivoL3Printer.mPrinterService = null
            this@PositivoL3Printer.mListener?.onServiceDisconnected()
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this@PositivoL3Printer.mPrinterService = IPrinterService.Stub.asInterface(service)
            this@PositivoL3Printer.mListener?.onServiceConnected()
        }
    }

    override fun startPrinterService(result: MethodChannel.Result?) {
        this.mCallback = object : IPrinterCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onException(code: Int, msg: String) {
                Log.w(TAG, "onException($code,$msg)")
            }

            @Throws(RemoteException::class)
            override fun onLength(current: Long, total: Long) {
                Log.w(TAG, "onLength: start")
                this@PositivoL3Printer.currentLen = current
                this@PositivoL3Printer.totalLen = total
                Log.w(TAG, "onLength: end")
            }

            override fun onComplete() {
                Log.i(TAG, "onComplete: start")
            }

            @Throws(RemoteException::class)
            override fun onRealLength(realCurrent: Double, realTotal: Double) {
                Log.i(TAG, "onReal Length: start")
                this@PositivoL3Printer.realCurrentLen = realCurrent
                this@PositivoL3Printer.realTotalLen = realTotal
                Log.i(TAG, "realCurrent=$realCurrent, realTotal=$realTotal")
                Log.i(TAG, "onReal Length: end")
            }
        }
        val intent = Intent()
        intent.setPackage("com.xcheng.printerservice")
        intent.action = "com.xcheng.printerservice.IPrinterService"
        this.context.startService(intent)
        this.context.bindService(intent, mConnectionService, Context.BIND_AUTO_CREATE)
    }

    override fun printBitmap(bitmap: Bitmap, result: MethodChannel.Result?) {
        mPrinterService!!.printerInit(mCallback)
        processBitmapForPrint(bitmap) {
            mPrinterService!!.printBitmap(it, mCallback)
        }
        mPrinterService!!.printerPaper(mCallback)
    }

    override fun printWrapPaper(n: Int, result: MethodChannel.Result?) {
        mPrinterService!!.printWrapPaper(n, mCallback)
    }

    override fun initPrinter(result: MethodChannel.Result?) {
        mPrinterService!!.printerInit(mCallback)
    }

    override fun printHtml(htmlContent: String, result: MethodChannel.Result?) {
        mPrinterService!!.printerInit(mCallback)
        getBitmapFromHtml(htmlContent, context, {
            mPrinterService!!.printBitmap(it, mCallback)
        }, {})
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
                    result.success(isAvaliable())
                } catch (e: Exception) {
                    result.error("sdkError", e.message, e.stackTrace);
                }
            }
            else -> result.notImplemented()
        }
    }

    companion object {
        fun isAvaliable(): Boolean {
            return Build.MANUFACTURER == "Positivo" && Build.MODEL == "L3"
        }

        private const val TAG: String = "PositivoL3Printer"
        private const val CHANNEL = "br.dev.tfs.escpos_lib/terminals/positivo"
    }
}