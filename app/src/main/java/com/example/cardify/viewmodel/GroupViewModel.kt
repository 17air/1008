package com.example.cardify.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardify.api.GroupRepository
import com.example.cardify.data.Group
import com.example.cardify.data.PostResponse
import com.example.cardify.util.Geo
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException
import kotlin.random.Random

/**
 * Shared ViewModel that exposes the list of groups rendered on the map screen.
 */
class GroupViewModel(
    private val repository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _groups = MutableLiveData<List<Group>>(emptyList())
    val groups: LiveData<List<Group>> = _groups

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private var hasLoadedInitial = false

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun loadGroups(userLocation: LatLng) {
        if (_isLoading.value == true || hasLoadedInitial) {
            updateDistances(userLocation)
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val posts = repository.loadNearbyGroups(userLocation.latitude, userLocation.longitude)
                val groups = posts.take(MAX_GROUPS).mapIndexed { index, post ->
                    post.toGroup(userLocation, index)
                }
                _groups.postValue(groups)
                hasLoadedInitial = true
            } catch (unknownHost: UnknownHostException) {
                useFallbackData(userLocation)
                _toastMessage.postValue(NETWORK_ERROR_MESSAGE)
            } catch (io: IOException) {
                useFallbackData(userLocation)
                _toastMessage.postValue(NETWORK_ERROR_MESSAGE)
            } catch (exception: Exception) {
                useFallbackData(userLocation)
                _errorMessage.postValue(exception.localizedMessage ?: "")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun addGroup(group: Group, userLocation: LatLng) {
        val updated = _groups.value.orEmpty().toMutableList().apply {
            val withDistance = group.copy(
                distanceMeters = Geo.haversineDistance(
                    userLocation.latitude,
                    userLocation.longitude,
                    group.latitude,
                    group.longitude
                )
            )
            add(0, withDistance)
        }
        _groups.value = updated
    }

    fun updateDistances(userLocation: LatLng) {
        val updated = _groups.value.orEmpty().map { group ->
            group.copy(
                distanceMeters = Geo.haversineDistance(
                    userLocation.latitude,
                    userLocation.longitude,
                    group.latitude,
                    group.longitude
                )
            )
        }
        _groups.value = updated
    }

    private fun useFallbackData(userLocation: LatLng) {
        val groups = FALLBACK_POSTS.mapIndexed { index, post ->
            post.toGroup(userLocation, index)
        }
        _groups.postValue(groups)
        hasLoadedInitial = true
    }

    private fun PostResponse.toGroup(userLocation: LatLng, index: Int): Group {
        val baseLatLng = randomizeAroundBase(index)
        val distance = Geo.haversineDistance(
            userLocation.latitude,
            userLocation.longitude,
            baseLatLng.latitude,
            baseLatLng.longitude
        )
        val locationText = "서울 중심부 주변"
        val maxPeople = (MIN_GROUP_SIZE..MAX_GROUP_SIZE).random()
        return Group(
            title = title.ifBlank { DEFAULT_TITLE },
            description = body.ifBlank { DEFAULT_DESCRIPTION },
            location = locationText,
            maxPeople = maxPeople,
            latitude = baseLatLng.latitude,
            longitude = baseLatLng.longitude,
            distanceMeters = distance
        )
    }

    private fun PostResponse.randomizeAroundBase(index: Int): LatLng {
        val random = Random(id.takeIf { it != 0 } ?: (index + 1) * 7919)
        val latOffset = (random.nextDouble() - 0.5) * 2 * OFFSET_DEGREES
        val lngOffset = (random.nextDouble() - 0.5) * 2 * OFFSET_DEGREES
        return LatLng(BASE_LAT + latOffset, BASE_LNG + lngOffset)
    }

    companion object {
        private const val BASE_LAT = 37.566
        private const val BASE_LNG = 126.978
        private const val OFFSET_DEGREES = 0.01
        private const val MAX_GROUPS = 10
        private const val MIN_GROUP_SIZE = 5
        private const val MAX_GROUP_SIZE = 20
        private const val NETWORK_ERROR_MESSAGE = "서버에 연결할 수 없습니다. 네트워크를 확인해주세요."
        private const val DEFAULT_TITLE = "서울 소모임"
        private const val DEFAULT_DESCRIPTION = "도심에서 만나는 소모임입니다."

        private val FALLBACK_POSTS = listOf(
            PostResponse(0, 1, "서울 등산 소모임", "주말 아침 북악산을 함께 올라가요."),
            PostResponse(0, 2, "한강 러닝 크루", "매주 수요일 저녁 여의도 한강공원에서 함께 달립니다."),
            PostResponse(0, 3, "카페 스터디", "종로의 조용한 카페에서 함께 공부해요."),
            PostResponse(0, 4, "보드게임 나이트", "보드게임으로 친목을 다져봐요."),
            PostResponse(0, 5, "요가 클래스", "소규모 요가 수업에 참여해보세요.")
        )
    }
}
