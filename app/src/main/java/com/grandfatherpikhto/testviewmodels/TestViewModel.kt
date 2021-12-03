package com.grandfatherpikhto.testviewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestViewModel(private val name:String): ViewModel() {
    companion object {
        const val TAG:String = "TestViewModel"
    }
    private val _regime = MutableLiveData<Int>(0)
    val regime:LiveData<Int> = _regime

    fun changeRegime(value:Int) {
        _regime.postValue(value)
        Log.d(TAG, "Regime: $value")
    }

    init {
        Log.d(TAG, "Init: $name")
    }
}