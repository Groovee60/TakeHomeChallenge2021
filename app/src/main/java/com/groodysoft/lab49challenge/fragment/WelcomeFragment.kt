package com.groodysoft.lab49challenge.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.groodysoft.lab49challenge.MainApplication
import com.groodysoft.lab49challenge.R
import com.groodysoft.lab49challenge.databinding.FragmentWelcomeBinding
import com.groodysoft.lab49challenge.showProgress

class WelcomeFragment: Fragment() {

    private lateinit var binding: FragmentWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        //findNavController().navigate(R.id.action_WelcomeFragment_to_PlayFragment)
    }
}