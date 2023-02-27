package com.acurast.attested.executor.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QR {
    companion object {
        /**
         * Converts a given string to its QR code representation.
         */
        fun encodeAsBitmap(payload: String): Bitmap {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.TRANSPARENT)
                }
            }
            return bitmap
        }
    }
}