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

    private String shopDocId;                   // 가게 Doc Id
    private Shop shop;                          // 가게 객체
    private String filePath;                    // 이미지 파일 경로 (캐쉬에 저장)

    private double distance;                    // 거리 (나의 위치와의 거리)

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_info);

        // 가게 doc id
        Intent intent = getIntent();
        this.shopDocId = intent.getStringExtra("shop_doc_id");
        this.distance = intent.getDoubleExtra("distance", -1);
        String shopName = intent.getStringExtra("shop_name");

        // 제목
        setTitle(shopName);

        // 홈버튼(<-) 표시
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

        // 정보 보기
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

    /* 정보 보기 */
    private void infoShop() {
        this.executed = true;

        // 가게 정보
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.SHOP)
                .document(this.shopDocId);
        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 성공
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    this.shop = document.toObject(Shop.class);
                    if (this.shop != null) {
                        this.txtShopName.setText(this.shop.getName());              // 상호
                        this.txtFoodCategory.setText(this.shop.getFoodCategory());  // 음식 카테고리

                        // 평점
                        double pointAvg = 0;
                        if (this.shop.getReviewPoint() > 0) {
                            // 평점 표시 (소수점 한자리까지 표시 (반올림))
                            pointAvg = (Math.round(((double) this.shop.getReviewPoint() / this.shop.getReviewCount()) * 10) / 10.0);
                        }
                        this.txtReviewPoint.setText(String.valueOf(pointAvg));

                        this.txtReviewCount.setText(String.valueOf(this.shop.getReviewCount()));    // 리뷰수

                        if (this.distance != -1) {
                            // 거리 표시
                            this.txtDistance.setText(Utils.getDistanceStr(this.distance));
                            this.txtDistance.setVisibility(View.VISIBLE);
                            this.imgMap.setVisibility(View.VISIBLE);
                        } else {
                            // 거리정보가 없으면
                            this.txtDistance.setVisibility(View.GONE);
                            this.imgMap.setVisibility(View.GONE);
                        }
                        this.txtAddress.setText(this.shop.getAddress());        // 주소
                        this.btnCall.setText(this.shop.getPhoneNumber());       // 전화번호
                        this.txtMenuTable.setText(this.shop.getMenuTable());    // 메뉴
                        this.txtMemo.setText(this.shop.getMemo());              // 가게 설명

                        // 대표사진
                        if (TextUtils.isEmpty(this.shop.getImageFileName())) {
                            // 없음
                            this.imgPhoto.setVisibility(View.GONE);
                            this.executed = false;
                        } else {
                            // 이미지 있음 ((대표사진) 파일 다운로드)
                            downloadFile(this.shop.getImageFileName());
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.executed = false;
                }
            } else {
                // 실패
                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                this.executed = false;
            }
        });
    }

    /* (대표사진) 파일 다운로드 */
    private void downloadFile(String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // 파일 확장자명 얻기
        String fileExtension = Utils.getFileExtension(fileName);

        // Storage 경로
        String storagePath = Constants.StorageFolderName.SHOP + "/" + this.shopDocId + "/" + fileName;
        StorageReference islandRef = storageRef.child(storagePath);

        try {
            // 캐시 활용 (임시저장)
            final File localFile = File.createTempFile(Constants.TEMP_FILE_PREFIX_NAME, "." + fileExtension);
            islandRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // 성공
                Log.d(TAG, "성공");

                // 파일 경로
                this.filePath = localFile.getAbsolutePath();

                // 이미지 사이즈 구성 (height)
                int width = this.imgPhoto.getWidth();
                this.imgPhoto.getLayoutParams().height = (int) (width * Constants.VIEW_IMAGE_RATE);
                this.imgPhoto.requestLayout();

                try {
                    // 로드 되기전에 Activity 종료 될때 오류 발생을 막기 위해 (try catch 문 적용)
                    // 이미지 표시 하기
                    Glide.with(this)
                            .load("file://" + filePath)
                            .error(R.drawable.ic_alert_circle_24_gray)
                            .apply(new RequestOptions().centerCrop())
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(this.imgPhoto);
                } catch (Exception ignored) {}

                this.executed = false;
            }).addOnFailureListener(exception -> {
                // 실패
                Log.d(TAG, "실패: " + exception.toString());
                this.executed = false;
            });
        } catch (IOException e) {
            // 오류
            Log.d(TAG, "오류: " + e.toString());
            this.executed = false;
        }
    }

    /* 클릭 리스너 */
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        if (this.executed) {
            return;
        }

        Intent intent;
        switch (v.getId()) {
            case R.id.imgPhoto:
                // 이미지 크게보기
                if (TextUtils.isEmpty(this.filePath)) {
                    return;
                }

                if (this.shop == null) {
                    return;
                }

                // 이미지 크게보기
                intent = new Intent(this, ImageViewActivity.class);
                intent.putExtra("title", this.shop.getName());
                intent.putExtra("file_path", this.filePath);
                startActivity(intent);
                break;
            case R.id.btnCall:
                // 전화걸기
                if (this.shop == null) {
                    return;
                }

                intent = new Intent("android.intent.action.DIAL", Uri.parse("tel:" + this.shop.getPhoneNumber()));
                startActivity(intent);
                break;
            case R.id.btnReview:
                // 리뷰
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
