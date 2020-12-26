package com.groodysoft.lab49challenge

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.groodysoft.lab49challenge.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun showProgress() {

        GlobalScope.launch(Dispatchers.Main) {
            binding.progress.layout.isVisible = true
        }
    }

    fun hideProgress() {

        GlobalScope.launch(Dispatchers.Main) {
            binding.progress.layout.isVisible = false
        }
    }
}

fun Activity.showProgress() {

    (this as MainActivity).showProgress()
}

fun Activity.hideProgress() {
    (this as MainActivity).hideProgress()
}

fun Activity.showFatalAlert(messageId: Int) {
    showFatalAlert(getString(messageId))
}

fun Activity.showFatalAlert(message: String) {

    val activity = this
    GlobalScope.launch(Dispatchers.Main) {

        hideProgress()

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.title_fatal_error)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            finish()
        }
        builder.show()
    }
}

val Activity.exceptionHandler: CoroutineExceptionHandler
    get() {
        return CoroutineExceptionHandler { _, e ->
                val msg = e.message ?: "Unspecified Error"
                showFatalAlert(msg) // handles offthreading and hides progress indicator
            }
        }