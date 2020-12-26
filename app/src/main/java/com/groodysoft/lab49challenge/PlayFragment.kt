package com.groodysoft.lab49challenge

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.groodysoft.lab49challenge.databinding.FragmentPlayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.format
import java.util.*

const val ONE_SECOND_MS = 1000L
const val RESTART_DELAY_SECS = 5
const val GAME_DURATION_MS = 120 * ONE_SECOND_MS

class PlayFragment: Fragment(), CameraItemListener {

    private lateinit var binding: FragmentPlayBinding

    private val tiles = mutableListOf<CameraItemView>()

    private var gameIsStarted = false
    private var gameIsOver = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // change the activity/window status bar color
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.bg_gray)

        binding = FragmentPlayBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = Lab49Repository.currentItemsToSnap
        tiles.add(binding.tileA)
        tiles.add(binding.tileB)
        tiles.add(binding.tileC)
        tiles.add(binding.tileD)

        for ((index, tile) in tiles.withIndex()) {
            tile.configure(index, items[index], this)
        }

        val startPrompts = resources.getStringArray(R.array.start_prompts)
        if (startPrompts.size == 3) {
            GlobalScope.launch(Dispatchers.Main) {
                binding.timer.text = startPrompts[0]    // Ready?
                delay(ONE_SECOND_MS)
                binding.timer.text = startPrompts[1]    // Set...
                delay(ONE_SECOND_MS)
                binding.timer.text = startPrompts[2]    // Go!
                delay(ONE_SECOND_MS)
                startGame()
            }
        }
    }

    private lateinit var timer: CountDownTimer
    private fun startGame() {
        gameIsStarted = true
        updateTimeDisplay(GAME_DURATION_MS)
        timer = object : CountDownTimer(GAME_DURATION_MS + ONE_SECOND_MS, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val done = updateTimeDisplay(millisUntilFinished)
                if (done) {
                    endGame(false)
                }
            }
            override fun onFinish() {
            }
        }.start()
    }

    private fun updateTimeDisplay(millisUntilFinished: Long): Boolean {

        val minutes: Long = millisUntilFinished / 1000 / 60
        val seconds = (millisUntilFinished / 1000 % 60)
        binding.timer.text = format("%d:%02d", minutes, seconds)
        return minutes == 0L && seconds == 0L
    }

    private fun endGame(won: Boolean) {

        gameIsOver = true
        timer.cancel()

        val color = if (won) Color.GREEN else Color.RED
        binding.timerBackground.setBackgroundColor(color)

        val resId = if (won) R.string.winner_toast_spec else R.string.loser_toast_spec
        val message = getString(resId, RESTART_DELAY_SECS)

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().onBackPressed()
        }, RESTART_DELAY_SECS * ONE_SECOND_MS)
    }

    private val Int.tileAtIndex: CameraItemView?
        get() {
            return when (this) {
                0 -> binding.tileA
                1 -> binding.tileB
                2 -> binding.tileC
                3 -> binding.tileD
                else -> null
            }
        }

    override fun onTapped(index: Int) {

        if (gameIsStarted && !gameIsOver) {
            index.tileAtIndex?.let { tile ->
                if (!tile.isMatched) {
                    handleTileTap(tile)
                }
            }
        }
    }

    override fun onResultChanged(index: Int) {

        val successCt = tiles.filter { it.isMatched }.size
        if (successCt == 4) {
            endGame(true)
        }
    }

    private fun handleTileTap(tile: CameraItemView) {

        // launch camera and capture image

        // TODO

        val filename = "images/${tile.item.name.toLowerCase(Locale.US)}.jpg"
        Utils.getBitmapFromAssets(filename)?.let { bitmap ->
            onImageCaptured(tile.index, bitmap)
        }
    }

    private fun onImageCaptured(index: Int, bitmap: Bitmap) {

        index.tileAtIndex?.let { tile ->

            tile.setCapturedImage(bitmap)
            tile.setResultState(TileResultState.VERIFY)

            // send image to server
            val payload = ImagePayload(tile.item.name, "junkfdjsklfewio")

            GlobalScope.launch(Dispatchers.IO + requireActivity().exceptionHandler) {

                // get phony image match result
                val result = Lab49Repository.postItem(payload)
                tile.setResultState(
                    when(result.matched) {
                        true -> TileResultState.SUCCESS
                        false -> TileResultState.INCORRECT
                    }
                )
            }
        }
    }

}