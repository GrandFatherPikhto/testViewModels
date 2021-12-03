package com.grandfatherpikhto.testviewmodels

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.testviewmodels.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    companion object {
        const val TAG:String = "FirstFragment"
    }

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // private val testViewModel:TestViewModel by viewModels()
    // private lateinit var testViewModel: TestViewModel
    private val testViewModel:TestAndroidViewModel by viewModels<TestAndroidViewModel> {
        TestAndroidViewModelFactory(requireActivity().application, "First")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // testViewModel = ViewModelProvider(this).get(TestViewModel::class.java)
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            buttonFirst.setOnClickListener {
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            }
            val regimes = arrayOf<String>("Off", "Tag", "Tail", "Water", "Blink")
            npRegimeFirst.displayedValues = regimes
            npRegimeFirst.minValue = 0
            npRegimeFirst.maxValue = regimes.size - 1
            npRegimeFirst.value = testViewModel.regime.value!!
            npRegimeFirst.setOnValueChangedListener { numberPicker, previous, value ->
                Log.d(TAG, "Regime: $value")
                if(testViewModel.regime.value != value) {
                    testViewModel.changeRegime(value)
                }
            }
            testViewModel.regime.observe(viewLifecycleOwner, { value ->
                if(value != npRegimeFirst.value) {
                    npRegimeFirst.value = value
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView()")
        testViewModel.store()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }

    override fun onStop() {
        super.onStop()
        testViewModel.store()
        Log.d(TAG, "onStop()")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
    }
}