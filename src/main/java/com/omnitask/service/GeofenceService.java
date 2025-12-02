package com.omnitask.service;

import com.omnitask.model.Location;

public class GeofenceService {

    // Office
    private static final double OFFICE_LATITUDE = 3.5952;
    private static final double OFFICE_LONGITUDE = 98.6722;
    // 0.05 = 50 meter
    // 0.10 = 100 meter
    // 0.50 = 500 meter
    private static final double GEOFENCE_RADIUS_KM = 0.1;


    public boolean isWithinGeofence(Location userLocation) {
        if (userLocation == null) {
            System.err.println("GEOFENCE: Lokasi user tidak terdeteksi (NULL).");
            return false;
        }

        // Hitung jarak
        double distanceKm = calculateDistance(
                userLocation.getLatitude(),
                userLocation.getLongitude(),
                OFFICE_LATITUDE,
                OFFICE_LONGITUDE
        );

        // Konversi ke meter
        double distanceMeters = distanceKm * 1000;

        System.out.println("==========================================");
        System.out.println("   CEK LOKASI ABSENSI");
        System.out.println("==========================================");
        System.out.println("Lokasi Kantor : " + OFFICE_LATITUDE + ", " + OFFICE_LONGITUDE);
        System.out.println("Lokasi User   : " + userLocation.getLatitude() + ", " + userLocation.getLongitude());
        System.out.println(String.format("Jarak         : %.2f meter (%.4f km)", distanceMeters, distanceKm));
        System.out.println("Batas Radius  : " + (GEOFENCE_RADIUS_KM * 1000) + " meter");

        boolean isAllowed = distanceKm <= GEOFENCE_RADIUS_KM;

        if (isAllowed) {
            System.out.println("STATUS        : [DI DALAM AREA] - Absen Diizinkan.");
        } else {
            System.out.println("STATUS        : [DI LUAR AREA] - Terlalu jauh!");
        }
        System.out.println("==========================================");

        return isAllowed;
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius bumi dalam km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    public Location getOfficeLocation() {
        return new Location(OFFICE_LATITUDE, OFFICE_LONGITUDE, "Kantor Pusat - Medan");
    }

    public double getGeofenceRadius() {
        return GEOFENCE_RADIUS_KM;
    }
}