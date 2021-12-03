package com.grandfatherpikhto.testviewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TestViewModelFactory(private val name:String): ViewModelProvider.NewInstanceFactory() {
    companion object {
        const val TAG:String = "TestViewModelFactory"
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "$name")
        return TestViewModel(name) as T
    }
}