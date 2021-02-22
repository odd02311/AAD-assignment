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
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.matzip.entity.Point;
import com.example.matzip.entity.Shop;
import com.example.matzip.util.Constants;
import com.example.matzip.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class ShopJoinActivity extends AppCompatActivity {
    private static final String TAG = ShopJoinActivity.class.getSimpleName();

    private boolean executed = false;

    private LinearLayout layLoading;
    private ImageView imgPhoto;
    private Spinner spFoodCategory;
    private EditText editShopId, editPassword1, editPassword2, editShopName, editPhoneNumber, editAddress, editMenuTable, editMemo;

    private InputMethodManager imm;

    private String filePath;

    private final static int MIN_SIZE = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_join);


        setTitle(R.string.activity_title_shop_join);


        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.imgPhoto = findViewById(R.id.imgPhoto);

        this.spFoodCategory = findViewById(R.id.spFoodCategory);

        this.editShopId = findViewById(R.id.editShopId);
        this.editShopId.setHint(R.string.hint_length_min_size_6);
        this.editShopId.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        this.editPassword1 = findViewById(R.id.editPassword1);
        this.editPassword1.setHint(R.string.hint_length_min_size_6);
        this.editPassword1.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        this.editPassword2 = findViewById(R.id.editPassword2);
        this.editPassword2.setHint(R.string.hint_confirm_password);
        this.editPassword2.setImeOptions(EditorInfo.IME_ACTION_NEXT);

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
        findViewById(R.id.btnJoin).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);


        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
                this.filePath = Utils.getRealPathFromURI(this, uri);
                Log.d(TAG, "file: " + this.filePath);


                Glide.with(this)
                        .load("file://" + this.filePath)
                        .error(R.drawable.ic_alert_circle_24_gray)
                        .transition(new DrawableTransitionOptions().crossFade())
                        .into(this.imgPhoto);
            }
        }
    }


    private boolean checkData() {

        String shopId = this.editShopId.getText().toString();
        if (TextUtils.isEmpty(shopId)) {
            Toast.makeText(this, getString(R.string.msg_shop_id_check_empty), Toast.LENGTH_SHORT).show();
            this.editShopId.requestFocus();
            return false;
        }


        if (shopId.length() < MIN_SIZE) {
            Toast.makeText(this, getString(R.string.msg_shop_id_check_length), Toast.LENGTH_SHORT).show();
            this.editShopId.requestFocus();
            return false;
        }


        String password1 = this.editPassword1.getText().toString();
        if (TextUtils.isEmpty(password1)) {
            Toast.makeText(this, getString(R.string.msg_password_check_empty), Toast.LENGTH_SHORT).show();
            this.editPassword1.requestFocus();
            return false;
        }


        if (password1.length() < MIN_SIZE) {
            Toast.makeText(this, getString(R.string.msg_password_check_length), Toast.LENGTH_SHORT).show();
            this.editPassword1.requestFocus();
            return false;
        }


        String password2 = this.editPassword2.getText().toString();
        if (!password1.equals(password2)) {
            Toast.makeText(this, getString(R.string.msg_password_check_confirm), Toast.LENGTH_SHORT).show();
            this.editPassword2.requestFocus();
            return false;
        }


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

    /* 업주 회원가입 */
    private void join() {
        String shopId = this.editShopId.getText().toString();
        String password = this.editPassword1.getText().toString();
        String shopName = this.editShopName.getText().toString();
        String phoneNumber = this.editPhoneNumber.getText().toString();
        String address = this.editAddress.getText().toString();
        String foodCategory = this.spFoodCategory.getSelectedItem().toString();
        String menuTable = this.editMenuTable.getText().toString();
        String memo = this.editMemo.getText().toString();


        long joinTimeMillis = System.currentTimeMillis();

        String fileName = "";

        if (!TextUtils.isEmpty(this.filePath)) {

            String fileExtension = Utils.getFileExtension(Utils.getFileName(this.filePath));

            fileName = joinTimeMillis + "." + fileExtension;
        }


        Point point = Utils.getGpsFromAddress(this, address);


        final Shop shop = new Shop(shopId, password, shopName, phoneNumber, address, foodCategory, menuTable, memo, fileName, point.latitude, point.longitude, joinTimeMillis);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.SHOP);


        Query query = reference.whereEqualTo("shopId", shopId);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {



                        db.collection(Constants.FirestoreCollectionName.SHOP)
                                .add(shop)
                                .addOnSuccessListener(documentReference -> {



                                    if (TextUtils.isEmpty(shop.getImageFileName())) {


                                        complete(shop.getShopId(), shop.getPassword());
                                    } else {

                                        uploadFile(documentReference.getId(), shop.getImageFileName(), shop.getShopId(), shop.getPassword());
                                    }
                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                    this.layLoading.setVisibility(View.GONE);
                                    this.executed = false;
                                });
                    } else {

                        Toast.makeText(this, getString(R.string.msg_shop_id_check_overlap), Toast.LENGTH_SHORT).show();
                        this.layLoading.setVisibility(View.GONE);
                        this.executed = false;
                    }
                } else {
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                }
            } else {

                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                this.layLoading.setVisibility(View.GONE);
                this.executed = false;
            }
        });
    }


    private void uploadFile(String docId, final String fileName, final String shopId, final String password) {
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

            complete(shopId, password);
        }).addOnSuccessListener(taskSnapshot -> {

            Log.d(TAG, "Cloud Storage Success");

            complete(shopId, password);
        });
    }


    private void complete(String shopId, String password) {
        this.layLoading.setVisibility(View.GONE);
        this.executed = false;


        Intent intent = new Intent();
        intent.putExtra("shop_id", shopId);
        intent.putExtra("password", password);
        setResult(Activity.RESULT_OK, intent);

        finish();
    }


    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.imgPhoto:

                Utils.goGallery(this, Constants.RequestCode.PICK_GALLERY);
                break;
            case R.id.btnJoin:

                if (checkData()) {
                    this.executed = true;
                    this.layLoading.setVisibility(View.VISIBLE);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        join();
                    }, Constants.LoadingDelay.SHORT);
                }
                break;
            case R.id.layLoading:

                break;
        }
    };
}
