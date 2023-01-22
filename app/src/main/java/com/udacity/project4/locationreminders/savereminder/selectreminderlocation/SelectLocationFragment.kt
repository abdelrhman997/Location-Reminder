package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.slider.Slider
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionsResultEvent
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.toLatLng
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
private const val PERMISSION_CODE_LOCATION_REQUEST = 1

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val viewModel: SaveReminderViewModel by inject()
    private val selectLocationViewModel: SelectLocationViewModel by viewModel()

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var theSelectedLocationMarker: Marker
    private lateinit var theSelectedLocationCircle: Circle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_select_location, container, false
        )

        binding.lifecycleOwner = this
        binding.onSaveButtonClicked = View.OnClickListener { onLocationSelected() }
        binding.viewModel = selectLocationViewModel

        binding.radiusSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                selectLocationViewModel.closeRadiusSelector()
            }
        })

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        setupGoogleMap()

        return binding.root
    }

    private fun setupGoogleMap() {
        val mapFragment = childFragmentManager
            .findFragmentByTag(getString(R.string.map_fragment)) as? SupportMapFragment
            ?: return

        selectLocationViewModel.radius.observe(viewLifecycleOwner) {
            if (!::theSelectedLocationCircle.isInitialized) {
                return@observe
            }

            theSelectedLocationCircle.radius =
                it?.toDouble() ?: GeofenceConstants.DEFAULT_RADIUS_IN_METRES.toDouble()
        }

        selectLocationViewModel.theSelectedLocation.observe(viewLifecycleOwner) {
            theSelectedLocationMarker.position = it.latLng
            theSelectedLocationCircle.center = it.latLng
            setCameraTo(it.latLng)
        }

        mapFragment.getMapAsync(this)
    }

    private fun onLocationSelected() {
        selectLocationViewModel.closeRadiusSelector()
        viewModel.settheSelectedLocation(selectLocationViewModel.theSelectedLocation.value!!)
        viewModel.setSelectedRadius(selectLocationViewModel.radius.value!!)
        viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        selectLocationViewModel.closeRadiusSelector()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun setMapType(mapType: Int): Boolean {
            map.mapType = mapType
            return true
        }

        return when (item.itemId) {
            R.id.normal_map -> setMapType(GoogleMap.MAP_TYPE_NORMAL)
            R.id.hybrid_map -> setMapType(GoogleMap.MAP_TYPE_HYBRID)
            R.id.terrain_map -> setMapType(GoogleMap.MAP_TYPE_TERRAIN)
            R.id.satellite_map -> setMapType(GoogleMap.MAP_TYPE_SATELLITE)

            else -> false
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))

        val markerOptions = MarkerOptions().position(map.cameraPosition.target).title(getString(R.string.dropped_pin)).draggable(true)

        theSelectedLocationMarker = map.addMarker(markerOptions)

        val circleOptions = CircleOptions().center(map.cameraPosition.target).fillColor(ResourcesCompat.getColor(resources, R.color.map_radius_fill_color, null)).strokeColor(ResourcesCompat.getColor(resources, R.color.map_radius_stroke_color, null)).strokeWidth(4f).radius(GeofenceConstants.DEFAULT_RADIUS_IN_METRES.toDouble())

        theSelectedLocationCircle = map.addCircle(circleOptions)

        viewModel.selectedPlaceOfInterest.value.let {
            selectLocationViewModel.settheSelectedLocation(
                it ?: PointOfInterest(map.cameraPosition.target, null, null)
            )

            if (it == null) {
                startAtCurrentLocation()
            }
        }

        map.setOnMapClickListener {
            if (selectLocationViewModel.isRadiusSelectorOpened.value == true) {
                selectLocationViewModel.closeRadiusSelector()
            } else {
                selectLocationViewModel.settheSelectedLocation(it)
            }
        }

        map.setOnPoiClickListener {
            if (selectLocationViewModel.isRadiusSelectorOpened.value == true) {
                selectLocationViewModel.closeRadiusSelector()
            } else {
                selectLocationViewModel.settheSelectedLocation(it)
            }
        }

        map.setOnCameraMoveListener {
            selectLocationViewModel.zoomValue = map.cameraPosition.zoom
        }
    }

    private fun locationPermissionHandler(event: PermissionsResultEvent, handler: () -> Unit) {
        if (event.areAllGranted) {
            handler()
            return
        }

        if (event.shouldShowRequestRationale) {
            viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAtCurrentLocation() {
        if (isPermissionGranted()) {
            fun resetToCurrentLocation() =
                LocationUtils.requestSingleUpdate {
                    selectLocationViewModel.settheSelectedLocation(it.toLatLng())
                }

            map.isMyLocationEnabled = true

            resetToCurrentLocation()
        } else {
            requestLocationPermission()
        }


    }
    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true

        }

        else {
            this.requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_CODE_LOCATION_REQUEST
            )
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            startAtCurrentLocation()
        } else {
            showRationale()
        }
    }

    private fun showRationale() {
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.location_permission)
                .setMessage(R.string.permission_denied_explanation)
                .setPositiveButton("OK"){ _, _, ->
                    requestLocationPermission()
                }
                .create()
                .show()

        } else {
            requestLocationPermission()
        }
    }

    private fun setCameraTo(latLng: LatLng) {
        val cameraPosition =
            CameraPosition.fromLatLngZoom(latLng, selectLocationViewModel.zoomValue)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

        map.animateCamera(cameraUpdate)
    }
}
