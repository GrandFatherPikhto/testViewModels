package com.grandfatherpikhto.testviewmodels

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.testviewmodels.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val testViewModel:TestAndroidViewModel by viewModels {
        TestAndroidViewModelFactory(requireActivity().application, "Second")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            val regimes = arrayOf<String>("Off", "Tag", "Tail", "Water", "Blink")
            npRegimeSecond.displayedValues = regimes
            npRegimeSecond.minValue = 0
            npRegimeSecond.maxValue = regimes.size - 1
            npRegimeSecond.value = testViewModel.regime.value!!
            npRegimeSecond.setOnValueChangedListener { numberPicker, previous, value ->
                Log.d(FirstFragment.TAG, "Regime: $value")
                if(testViewModel.regime.value != value) {
                    testViewModel.changeRegime(value)
                }
            }
            testViewModel.regime.observe(viewLifecycleOwner, { value ->
                if(value != npRegimeSecond.value) {
                    npRegimeSecond.value = value
                }
            })
            binding.buttonSecond.setOnClickListener {
                findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        testViewModel.store()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}