@file:Suppress("unused")

package com.groodysoft.lab49challenge

import android.graphics.Bitmap
import android.graphics.Matrix

object BitmapScaler {

    // converted from Java class @ https://gist.github.com/nesquena/3885707fd3773c09f1bb

    // scale and keep aspect ratio
    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }

    // scale and keep aspect ratio
    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
    }

    // scale and keep aspect ratio
    fun scaleToFill(b: Bitmap, width: Int, height: Int): Bitmap {
        val factorH = height / b.width.toFloat()
        val factorW = width / b.width.toFloat()
        val factorToUse = if (factorH > factorW) factorW else factorH
        return Bitmap.createScaledBitmap(
            b, (b.width * factorToUse).toInt(),
            (b.height * factorToUse).toInt(), true
        )
    }

    // scale and don't keep aspect ratio
    fun strechToFill(b: Bitmap, width: Int, height: Int): Bitmap {
        val factorH = height / b.height.toFloat()
        val factorW = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(
            b, (b.width * factorW).toInt(),
            (b.height * factorH).toInt(), true
        )
    }
}

// extension function to rotate bitmap
fun Bitmap.rotate(degrees:Float = 90f):Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(degrees)

    return Bitmap.createBitmap(
        this, // source bitmap
        0, // x coordinate of the first pixel in source
        0, // y coordinate of the first pixel in source
        width, // The number of pixels in each row
        height, // The number of rows
        matrix, // Optional matrix to be applied to the pixels
        false // true if the source should be filtered
    )
}