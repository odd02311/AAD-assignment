package com.example.matzip;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.matzip.entity.ShopItem;
import com.example.matzip.util.GlobalVariable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Objects;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static String TAG = MapActivity.class.getSimpleName();

    private GoogleMap googleMap;


    private double latitude, longitude;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        Intent intent = getIntent();
        this.latitude = intent.getDoubleExtra("latitude", Double.MIN_VALUE);
        this.longitude = intent.getDoubleExtra("longitude", Double.MIN_VALUE);


        setTitle(R.string.activity_title_map);


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;


        googleMap.setOnInfoWindowClickListener(marker -> {

            if (marker.getTag() != null) {
                ShopItem item = (ShopItem) marker.getTag();

                Intent intent = new Intent(this, ShopInfoActivity.class);
                intent.putExtra("shop_doc_id", item.id);
                intent.putExtra("shop_name", item.shop.getName());
                intent.putExtra("distance", item.distance);
                startActivity(intent);
            }
        });


        LatLng latLng = new LatLng(this.latitude, this.longitude);


        Marker marker = createMarker(latLng, "me", "", BitmapDescriptorFactory.HUE_CYAN);

        marker.showInfoWindow();


        markShop();

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));
    }


    private void markShop() {
        if (GlobalVariable.shopItems == null) {
            return;
        }

        for (ShopItem item : GlobalVariable.shopItems) {

            LatLng latLng = new LatLng(item.shop.getLatitude(), item.shop.getLongitude());


            Marker marker = createMarker(latLng, item.shop.getName(), item.shop.getFoodCategory(), BitmapDescriptorFactory.HUE_RED);

            marker.setTag(item);
        }
    }


    private Marker createMarker(LatLng latLng, String markerTitle, String markerSnippet, float icon) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(markerTitle);
        if (!TextUtils.isEmpty(markerSnippet)) {
            markerOptions.snippet(markerSnippet);
        }
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(icon));

        return this.googleMap.addMarker(markerOptions);
    }

}
