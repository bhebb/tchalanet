package com.tchalanet.mobile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import woyou.aidlservice.jiuiv5.ICallback
import woyou.aidlservice.jiuiv5.IWoyouService

class MainActivity : FlutterActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sunmiService: IWoyouService? = null
    private var sunmiConnection: ServiceConnection? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            SUNMI_PRINTER_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "isAvailable" -> withSunmiService(
                    result = result,
                    unavailableAsFalse = true,
                ) { result.success(true) }

                "printRaw" -> {
                    val bytes = call.argument<ByteArray>("bytes")
                    if (bytes == null || bytes.isEmpty()) {
                        result.error("sunmi_empty_payload", "Print payload is empty.", null)
                        return@setMethodCallHandler
                    }
                    withSunmiService(result = result) { service ->
                        try {
                            warmUpSunmiPrinter(service)
                            val sunmiBytes = removeEscPosCodePageSelection(bytes)
                            printEscPosWithSunmiCompatibility(service, sunmiBytes)
                            service.lineWrap(3, sunmiCallback("lineWrap"))
                            Log.i(SUNMI_LOG_TAG, "Rendered ${sunmiBytes.size} ESC/POS bytes through Sunmi API")
                            result.success(null)
                        } catch (error: Exception) {
                            Log.e(SUNMI_LOG_TAG, "Sunmi print failed", error)
                            result.error(
                                "sunmi_print_failed",
                                error.message ?: "Sunmi print failed.",
                                null,
                            )
                        }
                    }
                }

                else -> result.notImplemented()
            }
        }
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            RAWBT_PRINTER_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "isAvailable" -> result.success(isPackageInstalled(RAWBT_PACKAGE))
                "printRaw" -> {
                    val bytes = call.argument<ByteArray>("bytes")
                    if (bytes == null || bytes.isEmpty()) {
                        result.error("rawbt_empty_payload", "Print payload is empty.", null)
                        return@setMethodCallHandler
                    }
                    try {
                        printWithRawBt(bytes)
                        result.success(null)
                    } catch (error: Exception) {
                        Log.e(RAWBT_LOG_TAG, "RawBT print failed", error)
                        result.error(
                            "rawbt_print_failed",
                            error.message ?: "RawBT print failed.",
                            null,
                        )
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun printWithRawBt(bytes: ByteArray) {
        if (!isPackageInstalled(RAWBT_PACKAGE)) {
            throw IllegalStateException("RawBT is not installed.")
        }
        val file = File(cacheDir, "tchalanet-rawbt-ticket.bin")
        file.writeBytes(bytes)
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            setPackage(RAWBT_PACKAGE)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
        Log.i(RAWBT_LOG_TAG, "Sent ${bytes.size} ESC/POS bytes to RawBT")
    }

    private fun removeEscPosCodePageSelection(bytes: ByteArray): ByteArray {
        val output = ArrayList<Byte>(bytes.size)
        var index = 0
        var removed = 0
        while (index < bytes.size) {
            if (
                index + 2 < bytes.size &&
                bytes[index] == ESC &&
                bytes[index + 1] == SELECT_CODE_PAGE
            ) {
                removed++
                index += 3
                continue
            }
            output.add(bytes[index])
            index++
        }
        if (removed > 0) {
            Log.i(SUNMI_LOG_TAG, "Removed $removed unsupported ESC/POS code page command(s)")
        }
        return ByteArray(output.size) { output[it] }
    }

    private fun warmUpSunmiPrinter(service: IWoyouService) {
        service.printerInit(sunmiCallback("warmup.printerInit"))
        service.setAlignment(0, sunmiCallback("warmup.setAlignment"))
        service.setFontSize(SUNMI_TEXT_SIZE, sunmiCallback("warmup.setFontSize"))
        Thread.sleep(SUNMI_WARMUP_DELAY_MS)
    }

    private fun printEscPosWithSunmiCompatibility(service: IWoyouService, bytes: ByteArray) {
        val rawSegment = ArrayList<Byte>()
        var index = 0
        var qrCount = 0
        while (index < bytes.size) {
            if (isEscPosQrCommand(bytes, index)) {
                flushTextSegment(service, rawSegment)
                val commandLength = escPosQrCommandLength(bytes, index)
                val payload = escPosQrPayload(bytes, index)
                if (!payload.isNullOrBlank()) {
                    qrCount++
                    service.printQRCode(
                        payload,
                        SUNMI_QR_MODULE_SIZE,
                        SUNMI_QR_ERROR_LEVEL,
                        sunmiCallback("printQRCode"),
                    )
                    Thread.sleep(SUNMI_RAW_CHUNK_DELAY_MS)
                }
                index += commandLength
                continue
            }
            rawSegment.add(bytes[index])
            index++
        }
        flushTextSegment(service, rawSegment)
        if (qrCount > 0) {
            Log.i(SUNMI_LOG_TAG, "Rendered $qrCount ESC/POS QR command(s) through Sunmi API")
        }
    }

    private fun flushTextSegment(service: IWoyouService, segment: ArrayList<Byte>) {
        if (segment.isEmpty()) return
        printSunmiTextSegment(service, ByteArray(segment.size) { segment[it] })
        segment.clear()
    }

    private fun printSunmiTextSegment(service: IWoyouService, bytes: ByteArray) {
        val text = StringBuilder()
        var index = 0
        while (index < bytes.size) {
            when {
                index + 1 < bytes.size &&
                    bytes[index] == ESC &&
                    bytes[index + 1] == INIT -> {
                    flushSunmiText(service, text)
                    index += 2
                }

                index + 2 < bytes.size &&
                    bytes[index] == ESC &&
                    bytes[index + 1] == ALIGN -> {
                    flushSunmiText(service, text)
                    service.setAlignment(unsigned(bytes[index + 2]), sunmiCallback("setAlignment"))
                    index += 3
                }

                index + 2 < bytes.size &&
                    bytes[index] == ESC &&
                    bytes[index + 1] == BOLD -> {
                    index += 3
                }

                index + 2 < bytes.size &&
                    bytes[index] == ESC &&
                    bytes[index + 1] == FEED_LINES -> {
                    flushSunmiText(service, text)
                    service.lineWrap(unsigned(bytes[index + 2]).coerceIn(0, 10), sunmiCallback("lineWrap"))
                    index += 3
                }

                bytes[index] == LF -> {
                    text.append('\n')
                    index++
                }

                index + 1 < bytes.size && bytes[index] == ESC -> {
                    flushSunmiText(service, text)
                    index += 2
                }

                index + 1 < bytes.size && bytes[index] == GS -> {
                    flushSunmiText(service, text)
                    index += if (bytes[index + 1] == CUT && index + 3 < bytes.size) 4 else 2
                }

                else -> {
                    val start = index
                    while (
                        index < bytes.size &&
                        bytes[index] != ESC &&
                        bytes[index] != GS &&
                        bytes[index] != LF
                    ) {
                        index++
                    }
                    text.append(decodeEscPosText(bytes.copyOfRange(start, index)))
                }
            }
        }
        flushSunmiText(service, text)
    }

    private fun flushSunmiText(service: IWoyouService, text: StringBuilder) {
        if (text.isEmpty()) return
        service.printText(text.toString(), sunmiCallback("printText"))
        Thread.sleep(SUNMI_RAW_CHUNK_DELAY_MS)
        text.clear()
    }

    private fun decodeEscPosText(bytes: ByteArray): String =
        try {
            String(bytes, Charset.forName("CP850"))
        } catch (_: Exception) {
            String(bytes, Charsets.UTF_8)
        }

    private fun isEscPosQrCommand(bytes: ByteArray, index: Int): Boolean =
        index + 7 < bytes.size &&
            bytes[index] == GS &&
            bytes[index + 1] == LEFT_PARENTHESIS &&
            bytes[index + 2] == QR_COMMAND

    private fun escPosQrCommandLength(bytes: ByteArray, index: Int): Int {
        val payloadLength = unsigned(bytes[index + 3]) + (unsigned(bytes[index + 4]) * 256)
        return 5 + payloadLength
    }

    private fun escPosQrPayload(bytes: ByteArray, index: Int): String? {
        val payloadLength = unsigned(bytes[index + 3]) + (unsigned(bytes[index + 4]) * 256)
        if (payloadLength < QR_STORE_HEADER_SIZE) return null
        if (index + 5 + payloadLength > bytes.size) return null
        if (
            bytes[index + 5] != QR_NAMESPACE ||
            bytes[index + 6] != QR_STORE ||
            bytes[index + 7] != QR_STORE_MODE
        ) {
            return null
        }
        val dataStart = index + 8
        val dataLength = payloadLength - QR_STORE_HEADER_SIZE
        return bytes.copyOfRange(dataStart, dataStart + dataLength).toString(Charsets.UTF_8)
    }

    private fun unsigned(byte: Byte): Int = byte.toInt() and 0xFF

    private fun withSunmiService(
        result: MethodChannel.Result,
        unavailableAsFalse: Boolean = false,
        onReady: (IWoyouService) -> Unit,
    ) {
        sunmiService?.let {
            onReady(it)
            return
        }

        val completed = AtomicBoolean(false)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (!completed.compareAndSet(false, true)) return
                Log.i(SUNMI_LOG_TAG, "Sunmi printer service connected")
                sunmiService = IWoyouService.Stub.asInterface(service)
                sunmiConnection = this
                sunmiService?.let(onReady) ?: reportUnavailable(result, unavailableAsFalse)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w(SUNMI_LOG_TAG, "Sunmi printer service disconnected")
                sunmiService = null
                sunmiConnection = null
            }
        }
        sunmiConnection = connection

        val intent = Intent(SUNMI_SERVICE_ACTION).apply {
            setPackage(SUNMI_SERVICE_PACKAGE)
        }
        val bound = try {
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (_: Exception) {
            false
        }
        if (!bound) {
            Log.w(SUNMI_LOG_TAG, "Sunmi printer service bind failed")
            sunmiConnection = null
            reportUnavailable(result, unavailableAsFalse)
            return
        }

        mainHandler.postDelayed({
            if (!completed.compareAndSet(false, true)) return@postDelayed
            runCatching { unbindService(connection) }
            sunmiConnection = null
            reportUnavailable(result, unavailableAsFalse)
        }, SUNMI_BIND_TIMEOUT_MS)
    }

    private fun reportUnavailable(
        result: MethodChannel.Result,
        unavailableAsFalse: Boolean,
    ) {
        Log.w(SUNMI_LOG_TAG, "Sunmi internal printer unavailable")
        if (unavailableAsFalse) {
            result.success(false)
        } else {
            result.error(
                "sunmi_printer_unavailable",
                "Sunmi internal printer service is unavailable.",
                null,
            )
        }
    }

    private fun sunmiCallback(operation: String): ICallback =
        object : ICallback.Stub() {
            override fun onRunResult(isSuccess: Boolean) {
                Log.i(SUNMI_LOG_TAG, "$operation onRunResult=$isSuccess")
            }

            override fun onReturnString(result: String?) {
                Log.i(SUNMI_LOG_TAG, "$operation onReturnString=${result.orEmpty()}")
            }

            override fun onRaiseException(code: Int, msg: String?) {
                Log.e(SUNMI_LOG_TAG, "$operation onRaiseException code=$code msg=${msg.orEmpty()}")
            }

            override fun onPrintResult(code: Int, msg: String?) {
                Log.i(SUNMI_LOG_TAG, "$operation onPrintResult code=$code msg=${msg.orEmpty()}")
            }
        }

    override fun onDestroy() {
        sunmiConnection?.let { connection -> runCatching { unbindService(connection) } }
        sunmiConnection = null
        sunmiService = null
        super.onDestroy()
    }

    private companion object {
        const val SUNMI_PRINTER_CHANNEL = "com.tchalanet.mobile/sunmi_printer"
        const val RAWBT_PRINTER_CHANNEL = "com.tchalanet.mobile/rawbt_printer"
        const val SUNMI_LOG_TAG = "TchSunmiPrinter"
        const val RAWBT_LOG_TAG = "TchRawBtPrinter"
        const val RAWBT_PACKAGE = "ru.a402d.rawbtprinter"
        const val SUNMI_SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
        const val SUNMI_SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
        const val SUNMI_BIND_TIMEOUT_MS = 1500L
        const val SUNMI_RAW_CHUNK_DELAY_MS = 20L
        const val SUNMI_WARMUP_DELAY_MS = 180L
        const val SUNMI_TEXT_SIZE = 20.0f
        const val SUNMI_QR_MODULE_SIZE = 5
        const val SUNMI_QR_ERROR_LEVEL = 1
        const val ESC = 0x1B.toByte()
        const val GS = 0x1D.toByte()
        const val LF = 0x0A.toByte()
        const val INIT = 0x40.toByte()
        const val ALIGN = 0x61.toByte()
        const val BOLD = 0x45.toByte()
        const val FEED_LINES = 0x64.toByte()
        const val CUT = 0x56.toByte()
        const val SELECT_CODE_PAGE = 0x74.toByte()
        const val LEFT_PARENTHESIS = 0x28.toByte()
        const val QR_COMMAND = 0x6B.toByte()
        const val QR_NAMESPACE = 0x31.toByte()
        const val QR_STORE = 0x50.toByte()
        const val QR_STORE_MODE = 0x30.toByte()
        const val QR_STORE_HEADER_SIZE = 3
    }
}
