package br.dev.tfs.escpos_lib.terminals

import android.graphics.Bitmap
import io.flutter.plugin.common.MethodChannel

interface PrinterService: MethodChannel.MethodCallHandler {
    fun start();
    fun startPrinterService(result: MethodChannel.Result?);
    fun initPrinter(result: MethodChannel.Result?)
    fun printWrapPaper(n: Int, result: MethodChannel.Result?)
    fun printBitmap(bitmap: Bitmap, result: MethodChannel.Result?)
    fun printHtml(htmlContent: String, result: MethodChannel.Result?)
}