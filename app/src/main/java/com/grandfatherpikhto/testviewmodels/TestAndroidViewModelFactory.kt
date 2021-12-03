package com.grandfatherpikhto.testviewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TestAndroidViewModelFactory(private val application: Application, private val name:String): ViewModelProvider.NewInstanceFactory() {
    companion object {
        const val TAG:String = "TestAndroidViewModelFactory"
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "Create model instance: $name")
        return TestAndroidViewModel(application, name) as T
    }
}