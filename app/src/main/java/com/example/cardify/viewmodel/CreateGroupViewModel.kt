package com.example.cardify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardify.R
import com.example.cardify.api.GroupRepository
import com.example.cardify.data.CreateGroupRequest
import kotlinx.coroutines.launch

/**
 * Handles validation and submission logic for creating groups.
 */
class CreateGroupViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _creationSuccess = MutableLiveData<Unit>()
    val creationSuccess: LiveData<Unit> = _creationSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun validateInput(
        title: String,
        description: String,
        dateTime: String,
        location: String,
        maxPeopleText: String
    ): Int? {
        if (title.isBlank()) return R.string.error_group_title_required
        if (description.isBlank()) return R.string.error_group_description_required
        if (dateTime.isBlank()) return R.string.error_group_datetime_required
        if (location.isBlank()) return R.string.error_group_location_required
        val maxPeople = maxPeopleText.toIntOrNull()
            ?: return R.string.error_group_max_people_number
        if (maxPeople <= 0) return R.string.error_group_max_people_positive
        return null
    }

    fun submitGroup(request: CreateGroupRequest) {
        if (_isSubmitting.value == true) return
        _isSubmitting.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.createGroup(request)
            if (result.isSuccess) {
                _creationSuccess.postValue(Unit)
            } else {
                _errorMessage.postValue(
                    result.exceptionOrNull()?.localizedMessage
                        ?: ""
                )
            }
            _isSubmitting.postValue(false)
        }
    }
}
