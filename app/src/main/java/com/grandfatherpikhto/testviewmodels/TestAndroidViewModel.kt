package com.grandfatherpikhto.testviewmodels

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class TestAndroidViewModel(application: Application, private val name:String): AndroidViewModel(application) {
    companion object {
        const val TAG:String    = "TestAndroidViewModel"
        const val REGIME:String = "Regime"
    }

    /** */
    private val preferences: SharedPreferences by lazy {
        Log.d(TAG, "Application: ${getApplication<Application>()}")
        PreferenceManager.getDefaultSharedPreferences(getApplication())
    }

    /** */
    private val _regime = MutableLiveData<Int>(preferences.getInt(name + REGIME, 0))
    val regime: LiveData<Int> = _regime

    /** */
    init {
        Log.d(TAG, "Init $name")
    }

    /** */
    fun changeRegime(value:Int) {
        Log.d(TAG, "Regime: $value")
        _regime.postValue(value)
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared()")
        super.onCleared()
    }

    fun store() {
        preferences.edit {
            _regime.value?.let { putInt(name + REGIME, it) }
        }
    }
}