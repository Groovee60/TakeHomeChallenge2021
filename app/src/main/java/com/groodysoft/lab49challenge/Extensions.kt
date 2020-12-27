package com.groodysoft.lab49challenge

import android.graphics.Bitmap
import android.graphics.Matrix

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