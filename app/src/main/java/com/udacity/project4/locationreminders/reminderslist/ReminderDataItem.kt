package com.udacity.project4.locationreminders.reminderslist

import java.io.Serializable
import java.util.*


data class ReminderDataItem(
    val title: String?,
    val description: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radius: Float?,
    val id: String = UUID.randomUUID().toString()
) : Serializable {

    val printedLocation: String
        get() {
            if (location != null) {
                return location
            }

            return "Lat: $latitude Lon: $longitude"
        }
}


