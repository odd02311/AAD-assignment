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

    private InputMethodManager imm;                 // 키보드를 숨기기 위해 필요함

    private String filePath;                        // 대표사진 파일 경로

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_edit);

        // 제목
        setTitle(R.string.activity_title_shop_edit);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 로딩 레이아웃
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

        // 음식 카테고리 구성
        ArrayList<String> categoryItems = new ArrayList<>();
        Collections.addAll(categoryItems, getResources().getStringArray(R.array.food_category));
        categoryItems.add(0, "");
        this.spFoodCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryItems));

        this.imgPhoto.setOnClickListener(mClickListener);
        findViewById(R.id.btnSave).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // 정보 표시
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
                // 앨범
                if (data == null) {
                    return;
                }

                Uri uri = data.getData();
                this.filePath = Utils.getRealPathFromURI(this, uri);  // 실제 경로 얻기
                Log.d(TAG, "file: " + this.filePath);

                // 이미지 표시 하기
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

    /* 정보 표시 */
    private void displayInfo() {
        this.txtShopId.setText(GlobalVariable.shop.getShopId());        // 아이디

        this.editShopName.setText(GlobalVariable.shop.getName());
        this.editPhoneNumber.setText(GlobalVariable.shop.getPhoneNumber());
        this.editAddress.setText(GlobalVariable.shop.getAddress());
        this.editMenuTable.setText(GlobalVariable.shop.getMenuTable());
        this.editMemo.setText(GlobalVariable.shop.getMemo());

        // 음식 카테고리
        String[] foodCategoryArray = getResources().getStringArray(R.array.food_category);
        for (int i=0; i<foodCategoryArray.length; i++) {
            if (foodCategoryArray[i].equals(GlobalVariable.shop.getFoodCategory())) {
                this.spFoodCategory.setSelection(i+1);
                break;
            }
        }

        // 대표사진이 있으면
        if (!TextUtils.isEmpty(GlobalVariable.shop.getImageFileName())) {
            this.imgPhoto.setImageResource(R.drawable.ic_progress_download_24_gray);

            // 이미지 파일 다운로드
            downloadFile(GlobalVariable.shop.getImageFileName());
        }
    }

    /* (대표사진) 파일 다운로드 */
    private void downloadFile(String fileName) {
        this.executed = true;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // 파일 확장자명 얻기
        String fileExtension = Utils.getFileExtension(fileName);

        // Storage 경로
        String storagePath = Constants.StorageFolderName.SHOP + "/" + GlobalVariable.shopDocumentId + "/" + fileName;
        StorageReference islandRef = storageRef.child(storagePath);

        try {
            // 캐시 활용 (임시저장)
            final File localFile = File.createTempFile(Constants.TEMP_FILE_PREFIX_NAME, "." + fileExtension);
            islandRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // 성공
                Log.d(TAG, "성공");

                try {
                    // 로드 되기전에 Activity 종료 될때 오류 발생을 막기 위해 (try catch 문 적용)
                    // 이미지 표시 하기
                    Glide.with(this)
                            .load("file://" + localFile.getAbsolutePath())
                            .error(R.drawable.ic_alert_circle_24_gray)
                            .transition(new DrawableTransitionOptions().crossFade())
                            .into(imgPhoto);
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

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 상호 입력 체크
        String shopName = this.editShopName.getText().toString();
        if (TextUtils.isEmpty(shopName)) {
            Toast.makeText(this, getString(R.string.msg_shop_name_check_empty), Toast.LENGTH_SHORT).show();
            this.editShopName.requestFocus();
            return false;
        }

        // 전화번호 입력 체크
        String phoneNumber = this.editPhoneNumber.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, getString(R.string.msg_shop_phone_number_check_empty), Toast.LENGTH_SHORT).show();
            this.editPhoneNumber.requestFocus();
            return false;
        }

        // 주소 입력 체크
        String address = this.editAddress.getText().toString();
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, getString(R.string.msg_shop_address_check_empty), Toast.LENGTH_SHORT).show();
            this.editAddress.requestFocus();
            return false;
        }

        // 음식 카테고리 선택 체크
        if (this.spFoodCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, getString(R.string.msg_food_category_select_empty), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 메뉴 입력 체크
        String menuTable = this.editMenuTable.getText().toString();
        if (TextUtils.isEmpty(menuTable)) {
            Toast.makeText(this, getString(R.string.msg_menu_table_check_empty), Toast.LENGTH_SHORT).show();
            this.editMenuTable.requestFocus();
            return false;
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editMenuTable.getWindowToken(), 0);

        return true;
    }

    /* 변경내역 저장 */
    private void save() {
        final String shopName = this.editShopName.getText().toString();
        final String phoneNumber = this.editPhoneNumber.getText().toString();
        final String address = this.editAddress.getText().toString();
        final String foodCategory = this.spFoodCategory.getSelectedItem().toString();
        final String menuTable = this.editMenuTable.getText().toString();
        final String memo = this.editMemo.getText().toString();

        final String fileName;
        // 이미지가 변경되지 않았으면
        if (TextUtils.isEmpty(this.filePath)) {
            fileName = GlobalVariable.shop.getImageFileName();
        } else {
            if (TextUtils.isEmpty(GlobalVariable.shop.getImageFileName())) {
                // 파일 확장자명 얻기
                String fileExtension = Utils.getFileExtension(Utils.getFileName(this.filePath));
                // 파일명 회원가입시간(millisecond) 으로 만들기
                fileName = GlobalVariable.shop.getJoinTimeMillis() + "." + fileExtension;
            } else {
                fileName = GlobalVariable.shop.getImageFileName();
            }
        }

        final double latitude;
        final double longitude;
        // 주소가 변경되지 않았으면
        if (address.equals(GlobalVariable.shop.getAddress())) {
            latitude = GlobalVariable.shop.getLatitude();
            longitude = GlobalVariable.shop.getLongitude();
        } else {
            // 가게 위도/경도 얻기
            Point point = Utils.getGpsFromAddress(this, address);
            latitude = point.latitude;
            longitude = point.longitude;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.SHOP)
                .document(GlobalVariable.shopDocumentId);
        // 가게 정보 update
        reference.update("name", shopName, "phoneNumber", phoneNumber, "address", address, "foodCategory", foodCategory,
                "menuTable", menuTable, "memo", memo, "imageFileName", fileName, "latitude", latitude, "longitude", longitude)
                .addOnSuccessListener(aVoid -> {
                    // 성공
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
                        // 완료
                        complete();
                    } else {
                        // 이미지 업로드
                        uploadFile(GlobalVariable.shopDocumentId, fileName);
                    }
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                });
    }

    /* 이미지 파일 올리기 */
    private void uploadFile(String docId, final String fileName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Storage 경로
        String storagePath = Constants.StorageFolderName.SHOP + "/" + docId + "/" + fileName;
        final StorageReference riversRef = storageRef.child(storagePath);
        UploadTask uploadTask;

        // 이미지 회전 각도 구하기
        int degree = Utils.getExifOrientation(this.filePath);
        // 이미지 파일 사이즈 줄이기
        Bitmap bitmap = Utils.resizeImage(this.filePath, Constants.IMAGE_SCALE);
        if (degree != 0) {
            // 회전 하기
            bitmap = Utils.getRotatedBitmap(bitmap, degree);
        }
        // 파일 확장자명 얻기
        String fileExtension = Utils.getFileExtension(fileName);
        InputStream stream = Utils.bitmapToInputStream(bitmap, fileExtension);
        uploadTask = riversRef.putStream(stream);

        uploadTask.addOnFailureListener(exception -> {
            // 실패
            Log.d(TAG, "Cloud Storage 실패 " + exception.toString());
            // 이미지파일 등록 실패해도 완료 처리
            complete();
        }).addOnSuccessListener(taskSnapshot -> {
            // 성공
            Log.d(TAG, "Cloud Storage 성공");
            // 완료
            complete();
        });
    }

    /* 저장 완료 */
    private void complete() {
        this.layLoading.setVisibility(View.GONE);
        this.executed = false;

        Toast.makeText(this, R.string.msg_info_save, Toast.LENGTH_SHORT).show();
        finish();
    }

    /* 클릭 리스너 */
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.imgPhoto:
                // 이미지 (갤러리(사진) 연동)
                Utils.goGallery(this, Constants.RequestCode.PICK_GALLERY);
                break;
            case R.id.btnSave:
                // 저장
                if (checkData()) {
                    this.executed = true;
                    this.layLoading.setVisibility(View.VISIBLE);
                    // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // 변경 내용 저장
                        save();
                    }, Constants.LoadingDelay.SHORT);
                }
                break;
            case R.id.layLoading:
                // 로딩중 클릭 방지
                break;
        }
    };
}
