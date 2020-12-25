package com.groodysoft.lab49challenge.fragment

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
import com.groodysoft.lab49challenge.R
import com.groodysoft.lab49challenge.databinding.FragmentPlayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.format

const val ONE_SECOND_MS = 1000L
const val RESTART_DELAY_SECS = 5
const val GAME_DURATION_MS = 120 * ONE_SECOND_MS

class PlayFragment: Fragment() {

    private lateinit var binding: FragmentPlayBinding

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

    private fun startGame() {
        updateTimeDisplay(GAME_DURATION_MS)
        object : CountDownTimer(GAME_DURATION_MS + ONE_SECOND_MS, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val done = updateTimeDisplay(millisUntilFinished)
                if (done) {
                    cancel()
                    binding.timerBackground.setBackgroundColor(Color.RED)
                    endGame()
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

    private fun endGame() {

        val message = getString(R.string.times_up_spec, RESTART_DELAY_SECS)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().onBackPressed()
        }, RESTART_DELAY_SECS * ONE_SECOND_MS)
    }

}