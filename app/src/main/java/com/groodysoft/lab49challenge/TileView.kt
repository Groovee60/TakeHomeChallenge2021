package com.groodysoft.lab49challenge

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.groodysoft.lab49challenge.databinding.ViewTileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
    fun onTapped(index: Int)
    fun onResultChanged(index: Int)
}

class TileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyle, defStyleRes) {

    private var binding: ViewTileBinding

    lateinit var item: Lab49ServerItem
    private lateinit var resultState: TileResultState
    private var index: Int = -1

    private lateinit var listener: TileListener

    init {

        val activity = context as Activity
        binding = ViewTileBinding.inflate(activity.layoutInflater, this, true)

        // in a more state-of-the-art implementation, typeface could be specifed in XML
        binding.title.typeface = MainApplication.fontKarlaRegular
    }

    fun configureItem(index: Int, serverItem: Lab49ServerItem) {

        this.index = index
        item = serverItem

        setResultState(TileResultState.DEFAULT)
        binding.title.text = item.name
    }

    fun configureListener(listener: TileListener) {

        this.listener = listener
        setOnClickListener {
            listener.onTapped(index)
        }
    }

    fun setCapturedImage(bitmap: Bitmap) {
        binding.capturedImage.setImageBitmap(bitmap)
    }

    fun setResultState(state: TileResultState) {

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

            when (resultState) {
                TileResultState.SUCCESS, TileResultState.INCORRECT -> listener.onResultChanged(index)
                else -> {}
            }

        }
    }

    val isMatched: Boolean
        get() {
            return resultState == TileResultState.SUCCESS
        }

    val imageWidth: Int
        get() {
            return binding.capturedImage.width
        }

    val imageHeight: Int
        get() {
            return binding.capturedImage.height
        }
}