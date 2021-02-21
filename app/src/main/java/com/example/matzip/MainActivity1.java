package com.example.matzip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.example.matzip.adapter.ShopAdapter;
import com.example.matzip.entity.Point;
import com.example.matzip.entity.Shop;
import com.example.matzip.entity.ShopItem;
import com.example.matzip.listener.IAdapterOnClickListener;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.example.matzip.util.SharedPreferencesUtils;
import com.example.matzip.util.Utils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity1 extends AppCompatActivity implements LocationListener {
    private static final String TAG = MainActivity1.class.getSimpleName();

    private BackPressHandler backPressHandler;

    private boolean executed = false;

    // 로딩 레이아웃, 데이터 없을때 표시할 레이아웃
    private LinearLayout layLoading, layNoData;

    private RecyclerView recyclerView;
    private ShopAdapter adapter;
    private ArrayList<ShopItem> items;

    private Spinner spFoodCategory;
    private EditText editKeyword;
    private TextView txtAddress, txtCount;
    private RadioButton rd1, rd2, rd3, rdAll;
    private Button btnMap;

    private InputMethodManager imm;                 // 키보드를 숨기기 위해 필요함

    private int scope;                              // 범위

    // GPS
    private LocationManager locationManager;
    private Location location;

    // 최소 GPS 정보 업데이트 시간 밀리세컨이므로 10초
    private static final long GPS_MIN_TIME_BW_UPDATES = 1000 * 10;
    // 최소 GPS 정보 업데이트 거리 2미터
    private static final long GPS_MIN_DISTANCE_CHANGE_FOR_UPDATES = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_1);

        // 제목
        setTitle(R.string.activity_title_main_1);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        // 데이터 없을때 표시할 레이아웃
        this.layNoData = findViewById(R.id.layNoData);

        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.spFoodCategory = findViewById(R.id.spFoodCategory);

        this.editKeyword = findViewById(R.id.editKeyword);
        this.editKeyword.setImeOptions(EditorInfo.IME_ACTION_DONE);
        this.editKeyword.setHint(R.string.shop_name);

        // 음식 카테고리 구성
        ArrayList<String> categoryItems = new ArrayList<>();
        Collections.addAll(categoryItems, getResources().getStringArray(R.array.food_category));
        categoryItems.add(0, "All");
        this.spFoodCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryItems));

        // 좋아하는 카테고리 선택
        for (int i=0; i<categoryItems.size(); i++) {
            if (categoryItems.get(i).equals(GlobalVariable.user.getFavoriteCategory())) {
                this.spFoodCategory.setSelection(i);
                break;
            }
        }

        this.rd1 = findViewById(R.id.rd1);
        this.rd2 = findViewById(R.id.rd2);
        this.rd3 = findViewById(R.id.rd3);
        this.rdAll = findViewById(R.id.rdAll);

        // 범위 1Km를 디폴트로 선택
        this.scope = 1;
        this.rd1.setChecked(true);
        ((RadioGroup) findViewById(R.id.rdgScope)).setOnCheckedChangeListener(mCheckedChangeListener);

        this.txtAddress = findViewById(R.id.txtAddress);
        this.txtCount = findViewById(R.id.txtCount);

        this.btnMap = findViewById(R.id.btnMap);

        this.btnMap.setOnClickListener(mClickListener);
        findViewById(R.id.btnSearch).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // 종료 핸들러
        this.backPressHandler = new BackPressHandler(this);

        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 위치정보 사용여부 체크
        if (checkLocationServicesStatus()) {
            // Location 초기화
            initLocation();
        } else {
            // 범위 조건 Enabled false
            setScopeEnabled(false);

            // 위치정보 설정값으로 보여주기
            showLocationSettings();
        }

        // 가게 검색
        searchShop();
    }

    @Override
    public void onBackPressed() {
        if (this.executed) {
            return;
        }
        this.backPressHandler.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (this.locationManager != null) {
            // 위치정보 갱신 리스너 제거
            this.locationManager.removeUpdates(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // main 메뉴 생성
        getMenuInflater().inflate(R.menu.main_1, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                // 로그아웃
                new AlertDialog.Builder(this)
                        .setPositiveButton(getString(R.string.dialog_Yes), (dialog, which) -> {
                            // 로그아웃
                            logout();
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), null)
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_title_logout))
                        .setMessage(getString(R.string.dialog_msg_logout))
                        .show();

                return true;
            case R.id.menu_setting:
                // 설정
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "GPS: " + "위도 " + location.getLatitude() + ", 경도 " + location.getLongitude());
        this.location = location;
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d(TAG, "GPS OFF");

        // GPS OFF 될때
        if (this.locationManager != null) {
            this.locationManager.removeUpdates(this);
        }
        this.location = null;

        // 범위 조건 Enabled false
        setScopeEnabled(false);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(TAG, "GPS ON");

        // GPS ON 될때
        initLocation();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    /* Location 초기화 */
    private void initLocation() {
        // GPS 사용여부
        boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 네트워크 사용여부
        boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        boolean isLocation = false;
        if (!gpsEnabled && !networkEnabled) {
            // GPS 와 네트워크사용이 가능하지 않음
            Toast.makeText(this, getString(R.string.msg_location_disable), Toast.LENGTH_SHORT).show();
        } else {
            try {
                // 네트워크 정보로 부터 위치값 가져오기
                if (networkEnabled) {
                    this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            GPS_MIN_TIME_BW_UPDATES, GPS_MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    // 이전에 저장된 위치정보가 있으면 가져옴
                    this.location = this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                // GPS 로 부터 위치값 가져오기
                if (gpsEnabled && this.locationManager != null) {
                    this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            GPS_MIN_TIME_BW_UPDATES, GPS_MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (this.location == null) {
                        // 이전에 저장된 위치정보가 있으면 가져옴
                        this.location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }

                isLocation = true;

                if (this.location != null) {
                    Log.d(TAG, "GPS: " + "위도 " + this.location.getLatitude() + ", 경도 " + this.location.getLongitude());
                } else {
                    // 처음 위치정보 가져올 때는 이전 정보가 없기 때문에 null 값임
                    Log.d(TAG, "GPS: NULL");
                }
            } catch (SecurityException e) {
                Log.d(TAG, "Error: " + e.toString());
            }
        }

        // 범위 조건 Enabled 설정
        setScopeEnabled(isLocation);
    }

    /* 위치정보 사용여부 체크 */
    private boolean checkLocationServicesStatus() {
        return this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /* 위치정보 설정값으로 보여주기 */
    private void showLocationSettings() {
        new AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.dialog_Yes), (dialog, id) -> {
                    // 위치 서비스 설정창
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .setTitle(getString(R.string.dialog_title_location_setting))
                .setMessage(getString(R.string.dialog_msg_location_setting))
                .show();
    }

    /* 음식점 검색 */
    private void searchShop() {
        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editKeyword.getWindowToken(), 0);

        this.executed = true;
        this.layLoading.setVisibility(View.VISIBLE);
        // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 음식점 리스트
            listShop();
        }, Constants.LoadingDelay.SHORT);
    }

    /* 음식점 리스트 */
    private void listShop() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        /* 음식점 데이터 모두 가져와서 범위 안에 있는 음식점 찾기 */
        Query query = db.collection(Constants.FirestoreCollectionName.SHOP);
        // 카테고리를 선택했으면
        if (this.spFoodCategory.getSelectedItemPosition() > 0) {
            query = query.whereEqualTo("foodCategory", this.spFoodCategory.getSelectedItem().toString());
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.items = new ArrayList<>();

                    // 음식점 ArrayList 구성
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // 음식점 정보
                        Shop shop = document.toObject(Shop.class);

                        String keyword = this.editKeyword.getText().toString();
                        // 검색어가 있으면
                        if (!TextUtils.isEmpty(keyword)) {
                            // 상호에 검색어가 포함 되어 있는지 체크
                            if (!shop.getName().contains(keyword)) {
                                // 없으면
                                shop = null;
                            }
                        }

                        if (shop != null) {
                            double latitude = 0;
                            double longitude = 0;

                            if (this.location != null) {
                                // 현위치 기준
                                if (TextUtils.isEmpty(Constants.DEFAULT_ADDRESS)) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                } else {
                                    // 주소로 위도 경도 얻기
                                    Point point = Utils.getGpsFromAddress(this, Constants.DEFAULT_ADDRESS);
                                    latitude = point.latitude;
                                    longitude = point.longitude;
                                }
                            }

                            // 거리 구하기
                            double distance = -1;
                            if (latitude != 0) {
                                // 거리 구하기 (m)
                                distance = Utils.getDistance(latitude, longitude, shop.getLatitude(), shop.getLongitude());
                            }

                            ShopItem item = new ShopItem(document.getId(), shop, distance);
                            if (distance == -1 || this.scope == 0) {
                                // 위치 설정이 OFF 또는 범위를 전체로 했을 경우
                                this.items.add(item);
                            } else {
                                // 범위 적용
                                if (distance <= (scope * 1000)) {
                                    // 범위 안에 있는 음식점이면
                                    this.items.add(item);
                                }
                            }
                        }
                    }

                    if (this.items.size() == 0) {
                        // 음식점이 하나도 없으면
                        this.layNoData.setVisibility(View.VISIBLE);
                    } else {
                        this.layNoData.setVisibility(View.GONE);
                        if (this.location != null) {
                            // 거리순으로 정렬
                            Collections.sort(this.items, getComparator());
                        }
                    }

                    this.txtCount.setText(String.valueOf(items.size()));

                    // 리스트에 어뎁터 설정
                    this.adapter = new ShopAdapter(mAdapterListener, this.items);
                    this.recyclerView.setAdapter(this.adapter);
                }
            } else {
                // 오류
                Log.d(TAG, task.getException().toString());
            }

            // 로딩 레이아웃 숨김
            this.layLoading.setVisibility(View.GONE);
            this.executed = false;
        });
    }

    /* 데이터 정렬을 위한 Comparator (거리 ASC) */
    private Comparator<ShopItem> getComparator() {
        return (sort1, sort2) -> Double.compare(sort1.distance, sort2.distance);
    }

    /* 범위 조건 Enabled true/false */
    private void setScopeEnabled(boolean enabled) {
        this.rd1.setEnabled(enabled);
        this.rd2.setEnabled(enabled);
        this.rd3.setEnabled(enabled);
        this.rdAll.setEnabled(enabled);

        this.btnMap.setEnabled(enabled);

        if (enabled) {
            // 현재위치 표시
            if (TextUtils.isEmpty(Constants.DEFAULT_ADDRESS)) {
                this.txtAddress.setText(Utils.getAddressFromGps(this, this.location.getLatitude(), this.location.getLongitude()));
            } else {
                this.txtAddress.setText(Constants.DEFAULT_ADDRESS);
            }
        } else {
            this.txtAddress.setText(getString(R.string.msg_location_disable));
        }
    }

    /* 지도로 보기 */
    private void goMap() {
        if (this.items == null) {
            return;
        }

        if (this.location == null) {
            // 위치 설정 OFF 상태
            return;
        }

        if (this.items.size() == 0) {
            // 검색 결과가 없음
            Toast.makeText(this, R.string.msg_search_shop_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        // 전역변수에 음식점 list 설정
        GlobalVariable.shopItems = items;

        double latitude;
        double longitude;
        // 현재위치
        if (TextUtils.isEmpty(Constants.DEFAULT_ADDRESS)) {
            latitude = this.location.getLatitude();
            longitude = this.location.getLongitude();
        } else {
            // 주소로 위도 경도 얻기
            Point point = Utils.getGpsFromAddress(this, Constants.DEFAULT_ADDRESS);
            latitude = point.latitude;
            longitude = point.longitude;
        }

        // 지도로 보기
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        startActivity(intent);
    }

    /* 로그아웃 */
    private void logout() {
        // 구글 연동 logout
        // 파이어베이스 인증 sign out
        FirebaseAuth.getInstance().signOut();

        // 구글 api 클라이언트
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                Utils.getGoogleSignInOptions(getString(R.string.default_web_client_id)));
        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Document Id 값 clear
            SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, "");
            SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.NONE);

            // 회원선택화면으로 이동
            Intent intent = new Intent(this, MemberSelectActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /* 음식점 리스트 항목 클릭 리스너 */
    private final IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {
            // 음식점 선택
            int position = bundle.getInt("position");

            // 정보보기
            Intent intent = new Intent(MainActivity1.this, ShopInfoActivity.class);
            intent.putExtra("shop_doc_id", items.get(position).id);
            intent.putExtra("shop_name", items.get(position).shop.getName());
            intent.putExtra("distance", items.get(position).distance);
            startActivity(intent);
        }

        @Override
        public void onButtonClick(Bundle bundle, int id) {
        }
    };

    /* 범위 Radio 버튼 체크 리스너 */
    private final RadioGroup.OnCheckedChangeListener mCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
            if (location == null) {
                // 위치 설정 OFF 상태
                return;
            }

            switch (checkedId) {
                case R.id.rd1:
                    // 1Km
                    scope = 1;
                    break;
                case R.id.rd2:
                    // 3Km
                    scope = 3;
                    break;
                case R.id.rd3:
                    // 5Km
                    scope = 5;
                    break;
                case R.id.rdAll:
                    // 전체
                    scope = 0;
                    break;
            }

            Log.d(TAG, "scope" + scope);

            // 검색
            searchShop();
        }
    };

    /* 클릭 리스너 */
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.btnSearch:
                // 검색
                searchShop();
                break;
            case R.id.btnMap:
                // 지도로 보기
                goMap();
                break;
            case R.id.layLoading:
                // 로딩중 클릭 방지
                break;
        }
    };

    /* Back Press Class */
    private class BackPressHandler {
        private Context context;
        private Toast toast;

        private long backPressedTime = 0;

        public BackPressHandler(Context context) {
            this.context = context;
        }

        public void onBackPressed() {
            if (System.currentTimeMillis() > this.backPressedTime + (Constants.LoadingDelay.LONG * 2)) {
                this.backPressedTime = System.currentTimeMillis();

                this.toast = Toast.makeText(this.context, R.string.msg_back_press_end, Toast.LENGTH_SHORT);
                this.toast.show();

                return;
            }

            if (System.currentTimeMillis() <= this.backPressedTime + (Constants.LoadingDelay.LONG * 2)) {
                // 종료
                moveTaskToBack(true);
                finish();
                this.toast.cancel();
            }
        }
    }
}
