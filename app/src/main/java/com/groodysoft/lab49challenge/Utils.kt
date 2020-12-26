package com.groodysoft.lab49challenge

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream


object Utils {

    fun getBitmapFromAssets(fileName: String): Bitmap? {
        val assetManager = MainApplication.context.assets
        val istr: InputStream = assetManager.open(fileName)
        val bitmap = BitmapFactory.decodeStream(istr)
        istr.close()
        return bitmap
    }

}