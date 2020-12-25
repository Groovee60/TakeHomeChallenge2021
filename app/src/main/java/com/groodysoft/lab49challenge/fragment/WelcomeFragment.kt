package com.groodysoft.lab49challenge.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.groodysoft.lab49challenge.*
import com.groodysoft.lab49challenge.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WelcomeFragment: Fragment() {

    private lateinit var binding: FragmentWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // change the activity/window status bar color
        requireActivity().window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.gradient_top))

        binding = FragmentWelcomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.welcomeButton.typeface = MainApplication.fontKarlaBold
        binding.welcomeButton.setOnClickListener {

            loadDataAndPlay()
        }
    }

    private fun loadDataAndPlay() {

        requireActivity().showProgress()
        GlobalScope.launch(Dispatchers.IO) {

            Lab49Repository.currentItemsToSnap = Lab49Repository.getItems()
            findNavController().navigate(R.id.action_WelcomeFragment_to_PlayFragment)
            requireActivity().hideProgress()
        }
    }
}