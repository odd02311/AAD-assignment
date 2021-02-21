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

    // 나의 GPS (위도/경도)
    private double latitude, longitude;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 현재 위치
        Intent intent = getIntent();
        this.latitude = intent.getDoubleExtra("latitude", Double.MIN_VALUE);
        this.longitude = intent.getDoubleExtra("longitude", Double.MIN_VALUE);

        // 제목
        setTitle(R.string.activity_title_map);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 구글 지도 표시
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

        // 마커 타이틀 선택 리스너 설정
        googleMap.setOnInfoWindowClickListener(marker -> {
            // 마커 타이틀 선택시 이벤트 발생
            if (marker.getTag() != null) {
                ShopItem item = (ShopItem) marker.getTag();
                // 정보 보기
                Intent intent = new Intent(this, ShopInfoActivity.class);
                intent.putExtra("shop_doc_id", item.id);
                intent.putExtra("shop_name", item.shop.getName());
                intent.putExtra("distance", item.distance);
                startActivity(intent);
            }
        });

        // 나의 위치
        LatLng latLng = new LatLng(this.latitude, this.longitude);

        // 마커 생성
        Marker marker = createMarker(latLng, "나", "", BitmapDescriptorFactory.HUE_CYAN);
        // 마크 타이틀 항상 표시
        marker.showInfoWindow();

        // 음식점 표시
        markShop();

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));            // 나의 위치로 지도 이동
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(14));                // zoom 설정
    }

    /* 음식점 표시 */
    private void markShop() {
        if (GlobalVariable.shopItems == null) {
            return;
        }

        for (ShopItem item : GlobalVariable.shopItems) {
            // 마커 위치
            LatLng latLng = new LatLng(item.shop.getLatitude(), item.shop.getLongitude());

            // 마커 생성
            Marker marker = createMarker(latLng, item.shop.getName(), item.shop.getFoodCategory(), BitmapDescriptorFactory.HUE_RED);
            // 음식점 객체 설정 (마커 타이틀 선택시 정보보기 화면으로 이동하기 위함)
            marker.setTag(item);
        }
    }

    /* 마커 생성 */
    private Marker createMarker(LatLng latLng, String markerTitle, String markerSnippet, float icon) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);                                     // 마커 위치
        markerOptions.title(markerTitle);                                   // 마커 타이틀
        if (!TextUtils.isEmpty(markerSnippet)) {
            markerOptions.snippet(markerSnippet);                           // 마커 snippet (서브 타이틀)
        }
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(icon));    // 마커 아이콘

        return this.googleMap.addMarker(markerOptions);
    }

}
