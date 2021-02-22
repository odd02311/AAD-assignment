package com.example.matzip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.matzip.entity.Point;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.example.matzip.util.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class ShopEditActivity extends AppCompatActivity {
    private static final String TAG = ShopJoinActivity.class.getSimpleName();

    private boolean executed = false;

    private LinearLayout layLoading;
    private ImageView imgPhoto;
    private Spinner spFoodCategory;
    private EditText editShopName, editPhoneNumber, editAddress, editMenuTable, editMemo;
    private TextView txtShopId;

    private InputMethodManager imm;

    private String filePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_edit);


        setTitle(R.string.activity_title_shop_edit);


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.imgPhoto = findViewById(R.id.imgPhoto);

        this.spFoodCategory = findViewById(R.id.spFoodCategory);

        this.txtShopId = findViewById(R.id.txtShopId);

        this.editShopName = findViewById(R.id.editShopName);
        this.editShopName.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        this.editPhoneNumber = findViewById(R.id.editPhoneNumber);
        this.editPhoneNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        this.editAddress = findViewById(R.id.editAddress);
        this.editAddress.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        this.editMenuTable = findViewById(R.id.editMenuTable);
        this.editMemo = findViewById(R.id.editMemo);


        ArrayList<String> categoryItems = new ArrayList<>();
        Collections.addAll(categoryItems, getResources().getStringArray(R.array.food_category));
        categoryItems.add(0, "");
        this.spFoodCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryItems));

        this.imgPhoto.setOnClickListener(mClickListener);
        findViewById(R.id.btnSave).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);


        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);


        displayInfo();
    }

    @Override
    public void onBackPressed() {
        if (this.executed) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.RequestCode.PICK_GALLERY) {

                if (data == null) {
                    return;
                }

                Uri uri = data.getData();
                this.filePath = Utils.getRealPathFromURI(this, uri);  // 실제 경로 얻기
                Log.d(TAG, "file: " + this.filePath);


                Glide.with(this)
                        .load("file://" + this.filePath)
                        .error(R.drawable.ic_alert_circle_24_gray)
                        .transition(new DrawableTransitionOptions().crossFade())
                        .into(this.imgPhoto);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void displayInfo() {
        this.txtShopId.setText(GlobalVariable.shop.getShopId());

        this.editShopName.setText(GlobalVariable.shop.getName());
        this.editPhoneNumber.setText(GlobalVariable.shop.getPhoneNumber());
        this.editAddress.setText(GlobalVariable.shop.getAddress());
        this.editMenuTable.setText(GlobalVariable.shop.getMenuTable());
        this.editMemo.setText(GlobalVariable.shop.getMemo());


        String[] foodCategoryArray = getResources().getStringArray(R.array.food_category);
        for (int i=0; i<foodCategoryArray.length; i++) {
            if (foodCategoryArray[i].equals(GlobalVariable.shop.getFoodCategory())) {
                this.spFoodCategory.setSelection(i+1);
                break;
            }
        }


        if (!TextUtils.isEmpty(GlobalVariable.shop.getImageFileName())) {
            this.imgPhoto.setImageResource(R.drawable.ic_progress_download_24_gray);


            downloadFile(GlobalVariable.shop.getImageFileName());
        }
    }


    private void downloadFile(String fileName) {
        this.executed = true;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();


        String fileExtension = Utils.getFileExtension(fileName);


        String storagePath = Constants.StorageFolderName.SHOP + "/" + GlobalVariable.shopDocumentId + "/" + fileName;
        StorageReference islandRef = storageRef.child(storagePath);

        try {

            final File localFile = File.createTempFile(Constants.TEMP_FILE_PREFIX_NAME, "." + fileExtension);
            islandRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {

                Log.d(TAG, "Success");

                try {

                    Glide.with(this)
                            .load("file://" + localFile.getAbsolutePath())
                            .error(R.drawable.ic_alert_circle_24_gray)
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(imgPhoto);
                } catch (Exception ignored) {}
                this.executed = false;
            }).addOnFailureListener(exception -> {

                Log.d(TAG, "Fail: " + exception.toString());
                this.executed = false;
            });
        } catch (IOException e) {

            Log.d(TAG, "Error: " + e.toString());
            this.executed = false;
        }
    }


    private boolean checkData() {

        String shopName = this.editShopName.getText().toString();
        if (TextUtils.isEmpty(shopName)) {
            Toast.makeText(this, getString(R.string.msg_shop_name_check_empty), Toast.LENGTH_SHORT).show();
            this.editShopName.requestFocus();
            return false;
        }


        String phoneNumber = this.editPhoneNumber.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, getString(R.string.msg_shop_phone_number_check_empty), Toast.LENGTH_SHORT).show();
            this.editPhoneNumber.requestFocus();
            return false;
        }


        String address = this.editAddress.getText().toString();
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, getString(R.string.msg_shop_address_check_empty), Toast.LENGTH_SHORT).show();
            this.editAddress.requestFocus();
            return false;
        }


        if (this.spFoodCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, getString(R.string.msg_food_category_select_empty), Toast.LENGTH_SHORT).show();
            return false;
        }


        String menuTable = this.editMenuTable.getText().toString();
        if (TextUtils.isEmpty(menuTable)) {
            Toast.makeText(this, getString(R.string.msg_menu_table_check_empty), Toast.LENGTH_SHORT).show();
            this.editMenuTable.requestFocus();
            return false;
        }


        this.imm.hideSoftInputFromWindow(this.editMenuTable.getWindowToken(), 0);

        return true;
    }


    private void save() {
        final String shopName = this.editShopName.getText().toString();
        final String phoneNumber = this.editPhoneNumber.getText().toString();
        final String address = this.editAddress.getText().toString();
        final String foodCategory = this.spFoodCategory.getSelectedItem().toString();
        final String menuTable = this.editMenuTable.getText().toString();
        final String memo = this.editMemo.getText().toString();

        final String fileName;

        if (TextUtils.isEmpty(this.filePath)) {
            fileName = GlobalVariable.shop.getImageFileName();
        } else {
            if (TextUtils.isEmpty(GlobalVariable.shop.getImageFileName())) {

                String fileExtension = Utils.getFileExtension(Utils.getFileName(this.filePath));

                fileName = GlobalVariable.shop.getJoinTimeMillis() + "." + fileExtension;
            } else {
                fileName = GlobalVariable.shop.getImageFileName();
            }
        }

        final double latitude;
        final double longitude;

        if (address.equals(GlobalVariable.shop.getAddress())) {
            latitude = GlobalVariable.shop.getLatitude();
            longitude = GlobalVariable.shop.getLongitude();
        } else {

            Point point = Utils.getGpsFromAddress(this, address);
            latitude = point.latitude;
            longitude = point.longitude;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.SHOP)
                .document(GlobalVariable.shopDocumentId);

        reference.update("name", shopName, "phoneNumber", phoneNumber, "address", address, "foodCategory", foodCategory,
                "menuTable", menuTable, "memo", memo, "imageFileName", fileName, "latitude", latitude, "longitude", longitude)
                .addOnSuccessListener(aVoid -> {

                    GlobalVariable.shop.setName(shopName);
                    GlobalVariable.shop.setPhoneNumber(phoneNumber);
                    GlobalVariable.shop.setAddress(address);
                    GlobalVariable.shop.setFoodCategory(foodCategory);
                    GlobalVariable.shop.setMenuTable(menuTable);
                    GlobalVariable.shop.setMemo(memo);
                    GlobalVariable.shop.setImageFileName(fileName);
                    GlobalVariable.shop.setLatitude(latitude);
                    GlobalVariable.shop.setLongitude(longitude);

                    if (TextUtils.isEmpty(this.filePath)) {

                        complete();
                    } else {

                        uploadFile(GlobalVariable.shopDocumentId, fileName);
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                });
    }


    private void uploadFile(String docId, final String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();


        String storagePath = Constants.StorageFolderName.SHOP + "/" + docId + "/" + fileName;
        final StorageReference riversRef = storageRef.child(storagePath);
        UploadTask uploadTask;


        int degree = Utils.getExifOrientation(this.filePath);

        Bitmap bitmap = Utils.resizeImage(this.filePath, Constants.IMAGE_SCALE);
        if (degree != 0) {

            bitmap = Utils.getRotatedBitmap(bitmap, degree);
        }

        String fileExtension = Utils.getFileExtension(fileName);
        InputStream stream = Utils.bitmapToInputStream(bitmap, fileExtension);
        uploadTask = riversRef.putStream(stream);

        uploadTask.addOnFailureListener(exception -> {

            Log.d(TAG, "Cloud Storage Fail " + exception.toString());

            complete();
        }).addOnSuccessListener(taskSnapshot -> {

            Log.d(TAG, "Cloud Storage Success");

            complete();
        });
    }


    private void complete() {
        this.layLoading.setVisibility(View.GONE);
        this.executed = false;

        Toast.makeText(this, R.string.msg_info_save, Toast.LENGTH_SHORT).show();
        finish();
    }


    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.imgPhoto:

                Utils.goGallery(this, Constants.RequestCode.PICK_GALLERY);
                break;
            case R.id.btnSave:

                if (checkData()) {
                    this.executed = true;
                    this.layLoading.setVisibility(View.VISIBLE);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        save();
                    }, Constants.LoadingDelay.SHORT);
                }
                break;
            case R.id.layLoading:

                break;
        }
    };
}
