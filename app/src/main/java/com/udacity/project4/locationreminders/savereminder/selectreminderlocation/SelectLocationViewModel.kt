package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.locationreminders.geofence.GeofenceConstants

class SelectLocationViewModel : ViewModel() {

    private val _isRadiusSelectorOpeneded = MutableLiveData(false)
    private val _thetheSelectedLocation = MutableLiveData<PointOfInterest>()

    var zoomValue = 14.5f

    val isRadiusSelectorOpened: LiveData<Boolean>
        get() = _isRadiusSelectorOpeneded

    val theSelectedLocation: LiveData<PointOfInterest>
        get() = _thetheSelectedLocation

    val radius = MutableLiveData(GeofenceConstants.DEFAULT_RADIUS_IN_METRES)

    fun toggleRadiusSelector() {
        _isRadiusSelectorOpeneded.postValue(!(_isRadiusSelectorOpeneded.value ?: false))
    }

    fun settheSelectedLocation(pointOfInterest: PointOfInterest) {
        _thetheSelectedLocation.postValue(pointOfInterest)
    }

    fun settheSelectedLocation(latLng: LatLng) {
        settheSelectedLocation(PointOfInterest(latLng, null, null))
    }

    fun closeRadiusSelector() {
        _isRadiusSelectorOpeneded.postValue(false)
    }
}