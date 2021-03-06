package edu.uw.chather.ui.location;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.uw.chather.R;
import edu.uw.chather.databinding.FragmentLocationBinding;
import edu.uw.chather.databinding.FragmentChangeThemeBinding;
import edu.uw.chather.databinding.FragmentLocationBinding;
import edu.uw.chather.utils.Utils;
//import edu.uw.chather.databinding.FragmentLocationBinding;

/**
 * A simple {@link Fragment} subclass.
 *
 * @author Charles Bryan, Duy Nguyen, Demarco Best, Alec Mac, Alejandro Olono, My Duyen Huynh
 */
public class LocationFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    /*
    View model for the location fragment.
     */
    private LocationViewModel mModel;

    //private FragmentChangeThemeBinding bindingTheme;

    /*
    The entry point for managing the underlying map features and data.
     */
    private GoogleMap mMap;

    /*
    Latitude and longitude coordinates
     */
    private LatLng mLatLng;

    public LocationFragment() {
        // Required empty public constructor
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        FragmentLocationBinding binding = FragmentLocationBinding.bind(getView());

        mMap = googleMap;
        LocationViewModel model = new ViewModelProvider(getActivity())
                .get(LocationViewModel.class);
        model.addLocationObserver(getViewLifecycleOwner(), location -> {
            if(location != null) {
                googleMap.getUiSettings().setZoomControlsEnabled(true);
                googleMap.setMyLocationEnabled(true);
                final LatLng c = new LatLng(location.getLatitude(), location.getLongitude());
                mLatLng = c;
                //Zoom levels are from 2.0f (zoomed out) to 21.f (zoomed in)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(c, 15.0f));
//                binding.textLatLong.setText("Latitude:" + Double.toString(c.latitude) +
//                        "\nLongitude:" + Double.toString(c.longitude));
            }
        });

        binding.btnChangeStyle.setOnClickListener(this::changeType);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyledark));
        mMap.setOnMapClickListener(this);

    }

    /**
     * Private helper method for changing styles of the map.
     * @param view
     */
    private void changeType(View view) {
        if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentLocationBinding binding = FragmentLocationBinding.bind(getView());
        mModel = new ViewModelProvider(getActivity())
                .get(LocationViewModel.class);

        binding.buttonSearch.setOnClickListener(this::searchLatLong);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        //add this fragment as the OnMapReadyCallback -> See onMapReady()
        mapFragment.getMapAsync(this);

    }

    /**
     * Private helper method for searching a location from longitude and latitude.
     * @param view
     */
    private void searchLatLong(View view) {
        LocationFragmentDirections.ActionLocationFragmentToWeatherFragment directions =
                LocationFragmentDirections.actionLocationFragmentToWeatherFragment();
        directions.setLat(Double.toString(mLatLng.latitude));
        directions.setLng(Double.toString(mLatLng.longitude));
        Navigation.findNavController(getView()).navigate(directions);
    }

    @Override
    public void onMapClick(LatLng latLng) {

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(latLng.latitude + " : " + latLng.longitude);

        // Remove all marker
        mMap.clear();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        latLng, mMap.getCameraPosition().zoom));
        //binding.textLatLong.setText("Latitude:" + Double.toString(latLng.latitude) +
        //        "\nLongitude:" + Double.toString(latLng.longitude));
        mMap.addMarker(markerOptions);
        mLatLng = latLng;
    }
}
