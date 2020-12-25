package com.groodysoft.lab49challenge

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.groodysoft.lab49challenge.databinding.ActivityMainBinding
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