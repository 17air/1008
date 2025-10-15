package com.example.cardify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardify.api.GroupRepository
import com.example.cardify.data.PostResponse
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

/**
 * ViewModel responsible for exposing nearby groups to the group map screen.
 */
class GroupMapViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _posts = MutableLiveData<List<PostResponse>>()
    val posts: LiveData<List<PostResponse>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun loadNearbyGroups(latitude: Double, longitude: Double) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val posts = repository.loadNearbyGroups(latitude, longitude).take(MAX_POSTS)
                if (posts.isEmpty()) {
                    _posts.postValue(createFallbackPosts())
                } else {
                    _posts.postValue(posts)
                }
            } catch (unknownHost: UnknownHostException) {
                handleNetworkFailure()
            } catch (ioException: IOException) {
                handleNetworkFailure()
            } catch (exception: Exception) {
                _posts.postValue(createFallbackPosts())
                _errorMessage.postValue(exception.localizedMessage ?: "")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun handleNetworkFailure() {
        _posts.postValue(createFallbackPosts())
        _toastMessage.postValue(NETWORK_ERROR_MESSAGE)
    }

    private fun createFallbackPosts(): List<PostResponse> = listOf(
        PostResponse(
            userId = 0,
            id = 10_001,
            title = "서울 등산 소모임",
            body = "주말 아침 북악산을 함께 올라가요. 가벼운 산책 후 브런치까지!"
        ),
        PostResponse(
            userId = 0,
            id = 10_002,
            title = "한강 러닝 크루",
            body = "매주 수요일 저녁 여의도 한강공원에서 5km 러닝을 진행합니다."
        ),
        PostResponse(
            userId = 0,
            id = 10_003,
            title = "카페 스터디 모임",
            body = "종로 근처 조용한 카페에서 토요일 오후에 스터디를 운영해요."
        )
    )

    companion object {
        private const val MAX_POSTS = 10
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 네트워크를 확인해주세요."
    }
}
