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


    private LinearLayout layLoading, layNoData;

    private RecyclerView recyclerView;
    private ShopAdapter adapter;
    private ArrayList<ShopItem> items;

    private Spinner spFoodCategory;
    private EditText editKeyword;
    private TextView txtAddress, txtCount;
    private RadioButton rd1, rd2, rd3, rdAll;
    private Button btnMap;

    private InputMethodManager imm;

    private int scope;


    private LocationManager locationManager;
    private Location location;


    private static final long GPS_MIN_TIME_BW_UPDATES = 1000 * 10;

    private static final long GPS_MIN_DISTANCE_CHANGE_FOR_UPDATES = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_1);


        setTitle(R.string.activity_title_main_1);


        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));


        this.layNoData = findViewById(R.id.layNoData);

        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.spFoodCategory = findViewById(R.id.spFoodCategory);

        this.editKeyword = findViewById(R.id.editKeyword);
        this.editKeyword.setImeOptions(EditorInfo.IME_ACTION_DONE);
        this.editKeyword.setHint(R.string.shop_name);


        ArrayList<String> categoryItems = new ArrayList<>();
        Collections.addAll(categoryItems, getResources().getStringArray(R.array.food_category));
        categoryItems.add(0, "All");
        this.spFoodCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryItems));


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


        this.scope = 1;
        this.rd1.setChecked(true);
        ((RadioGroup) findViewById(R.id.rdgScope)).setOnCheckedChangeListener(mCheckedChangeListener);

        this.txtAddress = findViewById(R.id.txtAddress);
        this.txtCount = findViewById(R.id.txtCount);

        this.btnMap = findViewById(R.id.btnMap);

        this.btnMap.setOnClickListener(mClickListener);
        findViewById(R.id.btnSearch).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);


        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


        this.backPressHandler = new BackPressHandler(this);

        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (checkLocationServicesStatus()) {

            initLocation();
        } else {

            setScopeEnabled(false);


            showLocationSettings();
        }


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

            this.locationManager.removeUpdates(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_1, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:

                new AlertDialog.Builder(this)
                        .setPositiveButton(getString(R.string.dialog_Yes), (dialog, which) -> {

                            logout();
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), null)
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_title_logout))
                        .setMessage(getString(R.string.dialog_msg_logout))
                        .show();

                return true;
            case R.id.menu_setting:

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


        if (this.locationManager != null) {
            this.locationManager.removeUpdates(this);
        }
        this.location = null;


        setScopeEnabled(false);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(TAG, "GPS ON");


        initLocation();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }


    private void initLocation() {

        boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        boolean isLocation = false;
        if (!gpsEnabled && !networkEnabled) {

            Toast.makeText(this, getString(R.string.msg_location_disable), Toast.LENGTH_SHORT).show();
        } else {
            try {

                if (networkEnabled) {
                    this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            GPS_MIN_TIME_BW_UPDATES, GPS_MIN_DISTANCE_CHANGE_FOR_UPDATES, this);


                    this.location = this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }


                if (gpsEnabled && this.locationManager != null) {
                    this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            GPS_MIN_TIME_BW_UPDATES, GPS_MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (this.location == null) {

                        this.location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }

                isLocation = true;

                if (this.location != null) {
                    Log.d(TAG, "GPS: " + "위도 " + this.location.getLatitude() + ", 경도 " + this.location.getLongitude());
                } else {

                    Log.d(TAG, "GPS: NULL");
                }
            } catch (SecurityException e) {
                Log.d(TAG, "Error: " + e.toString());
            }
        }


        setScopeEnabled(isLocation);
    }


    private boolean checkLocationServicesStatus() {
        return this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    private void showLocationSettings() {
        new AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.dialog_Yes), (dialog, id) -> {

                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .setTitle(getString(R.string.dialog_title_location_setting))
                .setMessage(getString(R.string.dialog_msg_location_setting))
                .show();
    }


    private void searchShop() {

        this.imm.hideSoftInputFromWindow(this.editKeyword.getWindowToken(), 0);

        this.executed = true;
        this.layLoading.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            listShop();
        }, Constants.LoadingDelay.SHORT);
    }


    private void listShop() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        Query query = db.collection(Constants.FirestoreCollectionName.SHOP);

        if (this.spFoodCategory.getSelectedItemPosition() > 0) {
            query = query.whereEqualTo("foodCategory", this.spFoodCategory.getSelectedItem().toString());
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.items = new ArrayList<>();


                    for (QueryDocumentSnapshot document : task.getResult()) {

                        Shop shop = document.toObject(Shop.class);

                        String keyword = this.editKeyword.getText().toString();

                        if (!TextUtils.isEmpty(keyword)) {

                            if (!shop.getName().contains(keyword)) {

                                shop = null;
                            }
                        }

                        if (shop != null) {
                            double latitude = 0;
                            double longitude = 0;

                            if (this.location != null) {

                                if (TextUtils.isEmpty(Constants.DEFAULT_ADDRESS)) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                } else {

                                    Point point = Utils.getGpsFromAddress(this, Constants.DEFAULT_ADDRESS);
                                    latitude = point.latitude;
                                    longitude = point.longitude;
                                }
                            }


                            double distance = -1;
                            if (latitude != 0) {

                                distance = Utils.getDistance(latitude, longitude, shop.getLatitude(), shop.getLongitude());
                            }

                            ShopItem item = new ShopItem(document.getId(), shop, distance);
                            if (distance == -1 || this.scope == 0) {

                                this.items.add(item);
                            } else {

                                if (distance <= (scope * 1000)) {

                                    this.items.add(item);
                                }
                            }
                        }
                    }

                    if (this.items.size() == 0) {

                        this.layNoData.setVisibility(View.VISIBLE);
                    } else {
                        this.layNoData.setVisibility(View.GONE);
                        if (this.location != null) {

                            Collections.sort(this.items, getComparator());
                        }
                    }

                    this.txtCount.setText(String.valueOf(items.size()));


                    this.adapter = new ShopAdapter(mAdapterListener, this.items);
                    this.recyclerView.setAdapter(this.adapter);
                }
            } else {

                Log.d(TAG, task.getException().toString());
            }


            this.layLoading.setVisibility(View.GONE);
            this.executed = false;
        });
    }


    private Comparator<ShopItem> getComparator() {
        return (sort1, sort2) -> Double.compare(sort1.distance, sort2.distance);
    }


    private void setScopeEnabled(boolean enabled) {
        this.rd1.setEnabled(enabled);
        this.rd2.setEnabled(enabled);
        this.rd3.setEnabled(enabled);
        this.rdAll.setEnabled(enabled);

        this.btnMap.setEnabled(enabled);

        if (enabled) {

            if (TextUtils.isEmpty(Constants.DEFAULT_ADDRESS)) {
                this.txtAddress.setText(Utils.getAddressFromGps(this, this.location.getLatitude(), this.location.getLongitude()));
            } else {
                this.txtAddress.setText(Constants.DEFAULT_ADDRESS);
            }
        } else {
            this.txtAddress.setText(getString(R.string.msg_location_disable));
        }
    }


    private void goMap() {
        if (this.items == null) {
            return;
        }

        if (this.location == null) {

            return;
        }

        if (this.items.size() == 0) {

            Toast.makeText(this, R.string.msg_search_shop_empty, Toast.LENGTH_SHORT).show();
            return;
        }


        GlobalVariable.shopItems = items;

        double latitude;
        double longitude;

        if (TextUtils.isEmpty(Constants.DEFAULT_ADDRESS)) {
            latitude = this.location.getLatitude();
            longitude = this.location.getLongitude();
        } else {

            Point point = Utils.getGpsFromAddress(this, Constants.DEFAULT_ADDRESS);
            latitude = point.latitude;
            longitude = point.longitude;
        }


        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        startActivity(intent);
    }


    private void logout() {


        FirebaseAuth.getInstance().signOut();


        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                Utils.getGoogleSignInOptions(getString(R.string.default_web_client_id)));
        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Document Id value clear
            SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, "");
            SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.NONE);


            Intent intent = new Intent(this, MemberSelectActivity.class);
            startActivity(intent);
            finish();
        });
    }


    private final IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {

            int position = bundle.getInt("position");


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


    private final RadioGroup.OnCheckedChangeListener mCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
            if (location == null) {

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
                    // all
                    scope = 0;
                    break;
            }

            Log.d(TAG, "scope" + scope);


            searchShop();
        }
    };


    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.btnSearch:

                searchShop();
                break;
            case R.id.btnMap:

                goMap();
                break;
            case R.id.layLoading:

                break;
        }
    };


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
                moveTaskToBack(true);
                finish();
                this.toast.cancel();
            }
        }
    }
}
