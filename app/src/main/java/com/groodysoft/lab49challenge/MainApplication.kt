package com.groodysoft.lab49challenge

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class MainApplication : Application() {

    companion object {
        lateinit var context: MainApplication
        lateinit var gson: Gson
        lateinit var prefs: SharedPreferences

        // preferences is not utilized in this app

        lateinit var fontKarlaRegular: Typeface
        lateinit var fontKarlaBold: Typeface
    }

    override fun onCreate() {
        super.onCreate()

        context = this
        gson = GsonBuilder().create()

        context.apply {

            val prefsPackageName = "${packageName}.preferences"
            prefs = context.getSharedPreferences(prefsPackageName, Context.MODE_PRIVATE)

            fontKarlaRegular = Typeface.createFromAsset(assets, "fonts/Karla-Regular.ttf")
            fontKarlaBold = Typeface.createFromAsset(assets, "fonts/Karla-Bold.ttf")
        }
    }
}


