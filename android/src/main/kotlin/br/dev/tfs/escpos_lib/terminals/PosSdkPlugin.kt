package br.dev.tfs.escpos_lib.terminals

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import br.dev.tfs.escpos_lib.terminals.printers.JicaiPrinter
import br.dev.tfs.escpos_lib.terminals.printers.PaxPrinter
import br.dev.tfs.escpos_lib.terminals.printers.PositivoL3Printer
import br.dev.tfs.escpos_lib.terminals.printers.SunmiPrinter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger

class PosSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var binaryMessenger: BinaryMessenger? = null
    private var activity: Activity? = null

    private var printerJicai: JicaiPrinter? = null;
    private var printerPositivo: PositivoL3Printer? = null;
    private var printerPax: PaxPrinter? = null;
    private var printerSunmi: SunmiPrinter? = null;

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext

        binaryMessenger = flutterPluginBinding.binaryMessenger
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        result.notImplemented()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        binaryMessenger = null
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    
        // Printers
        printerJicai = JicaiPrinter(context, binaryMessenger!!)
        printerJicai?.start()
        printerPositivo = PositivoL3Printer(context, binaryMessenger!!)
        printerPositivo?.start()
        printerSunmi = SunmiPrinter(context, binaryMessenger!!)
        printerSunmi?.start()
        printerPax = PaxPrinter(context, binaryMessenger!!)
        printerPax?.start()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activity = null
        printerJicai = null
        printerPositivo = null
        printerSunmi = null
    }

    companion object {
        private const val TAG: String = "PosPlugin"
        private const val CHANNEL = "br.dev.tfs.escpos_lib/terminals"
    }
}
