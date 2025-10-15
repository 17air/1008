package com.example.cardify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardify.api.GroupRepository
import com.example.cardify.data.Group
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for exposing nearby groups to the group map screen.
 */
class GroupMapViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _groups = MutableLiveData<List<Group>>()
    val groups: LiveData<List<Group>> = _groups

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadNearbyGroups(latitude: Double, longitude: Double) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.loadNearbyGroups(latitude, longitude)
            if (result.isSuccess) {
                _groups.postValue(result.getOrDefault(emptyList()))
            } else {
                _errorMessage.postValue(result.exceptionOrNull()?.localizedMessage)
            }
            _isLoading.postValue(false)
        }
    }
}
