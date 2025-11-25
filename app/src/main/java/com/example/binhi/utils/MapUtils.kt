package com.example.binhi.utils

import com.google.android.gms.maps.model.LatLng

fun isPointInsidePolygon(point: LatLng, polygon: List<LatLng>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        if ((polygon[i].longitude > point.longitude) != (polygon[j].longitude > point.longitude) &&
            point.latitude < (polygon[j].latitude - polygon[i].latitude) *
            (point.longitude - polygon[i].longitude) /
            (polygon[j].longitude - polygon[i].longitude) + polygon[i].latitude
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}
