package com.omnitask.service;

import com.omnitask.model.Location;

public class GeofenceService {

    // Office location (example coordinates - replace with actual office location)
    private static final double OFFICE_LATITUDE = 3.5952;  // Medan, North Sumatra
    private static final double OFFICE_LONGITUDE = 98.6722;
    private static final double GEOFENCE_RADIUS_KM = 0.5; // 500 meters radius

    /**
     * Checks if the given location is within the office geofence
     * @param location Location to check
     * @return true if within geofence, false otherwise
     */
    public boolean isWithinGeofence(Location location) {
        if (location == null) {
            return false;
        }

        double distance = calculateDistance(
                location.getLatitude(),
                location.getLongitude(),
                OFFICE_LATITUDE,
                OFFICE_LONGITUDE
        );

        System.out.println("Distance from office: " + distance + " km");
        return distance <= GEOFENCE_RADIUS_KM;
    }

    /**
     * Calculates distance between two coordinates using Haversine formula
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Gets the office location
     * @return Office location
     */
    public Location getOfficeLocation() {
        return new Location(OFFICE_LATITUDE, OFFICE_LONGITUDE, "Office - Medan, North Sumatra");
    }

    /**
     * Gets the geofence radius in kilometers
     * @return Radius in km
     */
    public double getGeofenceRadius() {
        return GEOFENCE_RADIUS_KM;
    }
}