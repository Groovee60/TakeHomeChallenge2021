package com.groodysoft.lab49challenge

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.Base64
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.groodysoft.lab49challenge.databinding.ViewTileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

enum class TileResultState {
    DEFAULT,
    VERIFY,
    SUCCESS,
    INCORRECT;

    val needsShade: Boolean
        get() {
            return this == VERIFY || this == INCORRECT
        }

    val needsProgress: Boolean
        get() {
            return this == VERIFY
        }
}

interface TileListener {
    fun onTapped(tile: TileView)
    fun onResultChanged(tile: TileView)
}

class TileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyle, defStyleRes) {

    private var binding: ViewTileBinding

    private lateinit var item: Lab49ServerItem
    private lateinit var resultState: TileResultState

    private lateinit var listener: TileListener

    init {

        val activity = context as Activity
        binding = ViewTileBinding.inflate(activity.layoutInflater, this, true)

        // in a more state-of-the-art implementation, typeface could be specifed in XML
        binding.title.typeface = MainApplication.fontKarlaRegular
    }

    fun configureItem(serverItem: Lab49ServerItem) {

        item = serverItem

        setResultState(TileResultState.DEFAULT)
        binding.title.text = item.name
    }

    fun configureListener(listener: TileListener) {

        this.listener = listener
        setOnClickListener {
            listener.onTapped(this)
        }
    }

    private fun setResultState(state: TileResultState) {

        resultState = state
        GlobalScope.launch(Dispatchers.Main) {
            binding.bgFrame.setImageResource(
                    when (resultState) {
                        TileResultState.DEFAULT -> R.drawable.tile_default
                        TileResultState.VERIFY -> R.drawable.tile_verify
                        TileResultState.SUCCESS -> R.drawable.tile_success
                        TileResultState.INCORRECT -> R.drawable.tile_incorrect
                    }
            )

            binding.shade.isVisible = resultState.needsShade
            binding.tileProgress.isVisible = resultState.needsProgress

            // if the image is successfully or unsucesfully matched - let the caller test the game result
            when (resultState) {
                TileResultState.SUCCESS, TileResultState.INCORRECT -> listener.onResultChanged(this@TileView)
                else -> {}
            }
        }
    }

    val isMatched: Boolean
        get() {
            return resultState == TileResultState.SUCCESS
        }

    fun processCapturedBitmap(activity: Activity, rawImagePath: String) {

        BitmapFactory.Options().apply {

            // decode just to get the dimensions
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(rawImagePath, this)
            inJustDecodeBounds = false
            // reverse the width and height because it's landscape view instead of the portrait we want
            inSampleSize = max(1, min(outWidth / binding.capturedImage.height, outHeight / binding.capturedImage.width))

            // decode to resample
            BitmapFactory.decodeFile(rawImagePath, this)?.also { resizedBitmap ->

                // rotate
                resizedBitmap.rotate(90f)?.let { rotatedBitmap ->
                    setBitmapCandidate(activity, rotatedBitmap)
                }
            }
        }
    }

    private fun setBitmapCandidate(activity: Activity, bitmap: Bitmap) {

        // display bitmap in verifying state
        binding.capturedImage.setImageBitmap(bitmap)
        setResultState(TileResultState.VERIFY)

        // encode image
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val base64Encoding = Base64.encodeToString(byteArray, Base64.DEFAULT)

        // send image to server
        val payload = ImagePayload(item.name, base64Encoding)
        GlobalScope.launch(Dispatchers.IO + activity.exceptionHandler) {

            // get phony image match result
            val result = Lab49Repository.postItem(payload)
            setResultState(
                    when (result.matched) {
                        true -> TileResultState.SUCCESS
                        false -> TileResultState.INCORRECT
                    }
            )
        }
    }
}