package com.example.mirrormirrorandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class MirrorMapsFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapsFragment";

    private GoogleMap mMap;
    private Button btnSearchDestination;
    private TextView infoCard;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;

    // Request permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) startLocationUpdatesAndMoveCamera();
                            else Toast.makeText(requireContext(),
                                    "Location permission required to calculate routes",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    public MirrorMapsFragment() {
        super(R.layout.fragment_mirror_maps);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSearchDestination = view.findViewById(R.id.btnSearchDestination);
        infoCard = view.findViewById(R.id.infoCard);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment); // Update if using <MapView> instead
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnSearchDestination.setOnClickListener(v -> openDestinationDialog());

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startLocationUpdatesAndMoveCamera();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdatesAndMoveCamera() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLocation = location;
                        if (mMap != null) {
                            LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 13f));
                        }
                    }
                });
    }

    private void openDestinationDialog() {
        FloatingInputDialog dialog = new FloatingInputDialog(
                (destination, unused) -> {
                    if (destination == null || destination.trim().isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter a destination", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    geocodeAndShowDestination(destination.trim());
                },
                R.layout.floating_input_maps
        );

        dialog.show(getParentFragmentManager(), "floating_input_map");

    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Jump to current location if known
        if (lastLocation != null) {
            LatLng here = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 13f));
        }
    }

    private void geocodeAndShowDestination(String destinationName) {
        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(), Locale.getDefault());
                List<android.location.Address> addresses = geocoder.getFromLocationName(destinationName, 1);
                if (addresses == null || addresses.isEmpty()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                android.location.Address address = addresses.get(0);
                final double lat = address.getLatitude();
                final double lng = address.getLongitude();

                requireActivity().runOnUiThread(() -> {
                    if (mMap != null) {
                        mMap.clear();
                        LatLng dest = new LatLng(lat, lng);
                        mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                                .position(dest)
                                .title(destinationName)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 14f));
                    }
                });

                getRouteWithTraffic(lat, lng, destinationName);

            } catch (Exception e) {
                Log.e(TAG, "geocodeAndShowDestination: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Geocoding failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String getReadableLocationName(double lat, double lng) {
        try {
            android.location.Geocoder geocoder =
                    new android.location.Geocoder(requireContext(), Locale.getDefault());

            List<android.location.Address> addresses =
                    geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address addr = addresses.get(0);

                // Best human-friendly order
                if (addr.getSubLocality() != null)
                    return addr.getSubLocality();

                if (addr.getLocality() != null)
                    return addr.getLocality();

                if (addr.getFeatureName() != null)
                    return addr.getFeatureName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Reverse geocoding failed", e);
        }

        return "Current Location";
    }


    private void getRouteWithTraffic(double destLat, double destLng, String destinationName) {
        new Thread(() -> {
            try {
                double originLat = lastLocation != null ? lastLocation.getLatitude() : 0;
                double originLng = lastLocation != null ? lastLocation.getLongitude() : 0;

                if (originLat == 0 && originLng == 0) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Current location unknown. Allow location or try again.", Toast.LENGTH_LONG).show());
                    return;
                }

                String apiKey = getString(R.string.google_maps_key);
                String urlStr = "https://maps.googleapis.com/maps/api/directions/json?"
                        + "origin=" + originLat + "," + originLng
                        + "&destination=" + destLat + "," + destLng
                        + "&departure_time=now"
                        + "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) return;

                Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                scanner.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray routes = json.optJSONArray("routes");
                if (routes == null || routes.length() == 0) return;

                JSONObject route = routes.getJSONObject(0);
                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                String durationTrafficText = leg.has("duration_in_traffic")
                        ? leg.getJSONObject("duration_in_traffic").optString("text")
                        : leg.getJSONObject("duration").optString("text", "N/A");
                String distanceText = leg.getJSONObject("distance").optString("text", "N/A");
                String encodedPolyline = route.optJSONObject("overview_polyline").optString("points", null);

                requireActivity().runOnUiThread(() -> {
                    infoCard.setText(
                            String.format(Locale.getDefault(), "Distance: %s\nTravel time: %s", distanceText, durationTrafficText)
                    );

                    if (encodedPolyline != null && !encodedPolyline.isEmpty() && mMap != null) {
                        List<LatLng> poly = decodePoly(encodedPolyline);
                        mMap.addPolyline(new PolylineOptions().addAll(poly).width(10f));
                    }

                    sendRouteToServer(distanceText, durationTrafficText, destinationName);
                });

            } catch (Exception e) {
                Log.e(TAG, "getRouteWithTraffic error: " + e.getMessage(), e);
            }
        }).start();
    }

    private static List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    private void sendRouteToServer(String distanceText, String travelTimeText, String destinationName) {
        String mirrorIp = requireActivity()
                .getSharedPreferences("MirrorPrefs", Context.MODE_PRIVATE)
                .getString("selectedMirrorIp", null);

        if (mirrorIp == null) return;

        JSONObject payload = new JSONObject();
        try {
            payload.put("distance", distanceText);
            payload.put("travelTime", travelTimeText);
            // Add the new origin-destination field
            String originStr = "Current Location";

            if (lastLocation != null) {
                originStr = getReadableLocationName(
                        lastLocation.getLatitude(),
                        lastLocation.getLongitude()
                );
            }

            payload.put("route", originStr + " -> " + destinationName);
            payload.put("lastUpdate", System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + mirrorIp + ":8081/route");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                conn.getOutputStream().write(payload.toString().getBytes());
                conn.getOutputStream().flush();
                conn.getOutputStream().close();

                int responseCode = conn.getResponseCode();
                System.out.println("Route POST Response Code: " + responseCode);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
