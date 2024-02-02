package br.dev.tfs.escpos_lib.terminals

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import kotlin.math.absoluteValue
import kotlin.math.ceil

fun WebView.toBitmap(offsetWidth: Double, offsetHeight: Double): Bitmap? {
    if (offsetHeight > 0 && offsetWidth > 0) {
        val width = (offsetWidth).absoluteValue.toInt()
        val height = (offsetHeight).absoluteValue.toInt()
        this.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        return bitmap
    }
    return null
}

fun processBitmapForPrint(bitmap: Bitmap, onPrintBitmap: (result: Bitmap) -> Unit) {
    var currentY = 0
    var currentBlock = 1
    val blockCount = ceil(bitmap.height / 595.00)

    while (currentBlock <= blockCount) {
        val targetHeight = if (currentY + 595 > bitmap.height) {
            bitmap.height - currentY
        } else {
            595
        }

        onPrintBitmap(Bitmap.createBitmap(bitmap, 0, currentY, bitmap.width, targetHeight))

        currentY = if (currentY + 595 > bitmap.height) {
            bitmap.height - currentY
        } else {
            currentY + 595
        }

        currentBlock++
    }
}

fun getBitmapFromHtml(htmlContent: String, context: Context, onPrintBitmap: (result: Bitmap) -> Unit, onComplete: () -> Unit) {
    val webView = WebView(context)

    val dwidth = 380
    val dheight = 595

    webView.layout(0, 0, dwidth, dheight)
    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    webView.setInitialScale(100)
    webView.isVerticalScrollBarEnabled = false
    webView.settings.javaScriptEnabled = true
    webView.settings.useWideViewPort = true
    webView.settings.javaScriptCanOpenWindowsAutomatically = true
    webView.settings.loadWithOverviewMode = true
    WebView.enableSlowWholeDocumentDraw()

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            val _duration = (dheight / 1000 ).toInt() * 200 ; /// delay 300 ms for every dheight 2000

            Handler(Looper.getMainLooper()).postDelayed({
                webView.evaluateJavascript("(function() { return [document.body.offsetWidth, document.body.offsetHeight]; })();"){it
                    val xy = JSONArray(it)
                    val offsetWidth = xy[0].toString();
                    var offsetHeight = xy[1].toString();
                    if (offsetHeight.toInt() < 1000) {
                        offsetHeight = (xy[1].toString().toInt() + 20).toString();
                    }
                    val computedBitmap = webView.toBitmap(offsetWidth.toDouble(), offsetHeight.toDouble())
                    if (computedBitmap != null) {
                        var currentY = 0
                        var currentBlock = 1
                        val blockCount = ceil(computedBitmap.height / 595.00)

                        while (currentBlock <= blockCount) {
                            val targetHeight = if (currentY + 595 > computedBitmap.height) {
                                computedBitmap.height - currentY
                            } else {
                                595
                            }

                            onPrintBitmap(Bitmap.createBitmap(computedBitmap, 0, currentY, computedBitmap.width, targetHeight))

                            currentY = if (currentY + 595 > computedBitmap.height) {
                                computedBitmap.height - currentY
                            } else {
                                currentY + 595
                            }

                            currentBlock++
                        }
                    } else {
                        throw Exception("Failed to generate WebView bitmaps")
                    }
                }
            }, _duration.toLong())
        }
    }
}