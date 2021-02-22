package com.example.matzip;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.matzip.entity.Shop;
import com.example.matzip.util.Constants;
import com.example.matzip.util.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ShopInfoActivity extends AppCompatActivity {
    private static final String TAG = ShopInfoActivity.class.getSimpleName();

    private boolean executed = false;

    private ImageView imgPhoto, imgMap;
    private TextView txtShopName, txtFoodCategory, txtReviewPoint, txtReviewCount, txtDistance, txtAddress, txtMenuTable, txtMemo;
    private Button btnCall;

    private String shopDocId;
    private Shop shop;
    private String filePath;

    private double distance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_info);


        Intent intent = getIntent();
        this.shopDocId = intent.getStringExtra("shop_doc_id");
        this.distance = intent.getDoubleExtra("distance", -1);
        String shopName = intent.getStringExtra("shop_name");


        setTitle(shopName);


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        this.imgPhoto = findViewById(R.id.imgPhoto);
        this.imgMap = findViewById(R.id.imgMap);

        this.txtShopName = findViewById(R.id.txtShopName);
        this.txtFoodCategory = findViewById(R.id.txtFoodCategory);
        this.txtReviewPoint = findViewById(R.id.txtReviewPoint);
        this.txtReviewCount = findViewById(R.id.txtReviewCount);
        this.txtDistance = findViewById(R.id.txtDistance);
        this.txtAddress = findViewById(R.id.txtAddress);
        this.txtMenuTable = findViewById(R.id.txtMenuTable);
        this.txtMemo = findViewById(R.id.txtMemo);

        this.btnCall = findViewById(R.id.btnCall);

        this.imgPhoto.setOnClickListener(mClickListener);
        this.btnCall.setOnClickListener(mClickListener);
        (findViewById(R.id.btnReview)).setOnClickListener(mClickListener);


        infoShop();
    }

    @Override
    public void onBackPressed() {
        if (this.executed) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void infoShop() {
        this.executed = true;


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.SHOP)
                .document(this.shopDocId);
        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    this.shop = document.toObject(Shop.class);
                    if (this.shop != null) {
                        this.txtShopName.setText(this.shop.getName());
                        this.txtFoodCategory.setText(this.shop.getFoodCategory());


                        double pointAvg = 0;
                        if (this.shop.getReviewPoint() > 0) {

                            pointAvg = (Math.round(((double) this.shop.getReviewPoint() / this.shop.getReviewCount()) * 10) / 10.0);
                        }
                        this.txtReviewPoint.setText(String.valueOf(pointAvg));

                        this.txtReviewCount.setText(String.valueOf(this.shop.getReviewCount()));

                        if (this.distance != -1) {

                            this.txtDistance.setText(Utils.getDistanceStr(this.distance));
                            this.txtDistance.setVisibility(View.VISIBLE);
                            this.imgMap.setVisibility(View.VISIBLE);
                        } else {

                            this.txtDistance.setVisibility(View.GONE);
                            this.imgMap.setVisibility(View.GONE);
                        }
                        this.txtAddress.setText(this.shop.getAddress());
                        this.btnCall.setText(this.shop.getPhoneNumber());
                        this.txtMenuTable.setText(this.shop.getMenuTable());
                        this.txtMemo.setText(this.shop.getMemo());


                        if (TextUtils.isEmpty(this.shop.getImageFileName())) {

                            this.imgPhoto.setVisibility(View.GONE);
                            this.executed = false;
                        } else {

                            downloadFile(this.shop.getImageFileName());
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.executed = false;
                }
            } else {

                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                this.executed = false;
            }
        });
    }


    private void downloadFile(String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();


        String fileExtension = Utils.getFileExtension(fileName);


        String storagePath = Constants.StorageFolderName.SHOP + "/" + this.shopDocId + "/" + fileName;
        StorageReference islandRef = storageRef.child(storagePath);

        try {

            final File localFile = File.createTempFile(Constants.TEMP_FILE_PREFIX_NAME, "." + fileExtension);
            islandRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {

                Log.d(TAG, "Success");


                this.filePath = localFile.getAbsolutePath();


                int width = this.imgPhoto.getWidth();
                this.imgPhoto.getLayoutParams().height = (int) (width * Constants.VIEW_IMAGE_RATE);
                this.imgPhoto.requestLayout();

                try {

                    Glide.with(this)
                            .load("file://" + filePath)
                            .error(R.drawable.ic_alert_circle_24_gray)
                            .apply(new RequestOptions().centerCrop())
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(this.imgPhoto);
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


    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        if (this.executed) {
            return;
        }

        Intent intent;
        switch (v.getId()) {
            case R.id.imgPhoto:

                if (TextUtils.isEmpty(this.filePath)) {
                    return;
                }

                if (this.shop == null) {
                    return;
                }


                intent = new Intent(this, ImageViewActivity.class);
                intent.putExtra("title", this.shop.getName());
                intent.putExtra("file_path", this.filePath);
                startActivity(intent);
                break;
            case R.id.btnCall:

                if (this.shop == null) {
                    return;
                }

                intent = new Intent("android.intent.action.DIAL", Uri.parse("tel:" + this.shop.getPhoneNumber()));
                startActivity(intent);
                break;
            case R.id.btnReview:

                if (this.shop == null) {
                    return;
                }

                intent = new Intent(this, ReviewActivity.class);
                intent.putExtra("shop_doc_id", this.shopDocId);
                intent.putExtra("shop_name", this.shop.getName());
                startActivity(intent);
                break;
        }
    };
}
