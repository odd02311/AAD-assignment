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

    private InputMethodManager imm;                 // 키보드를 숨기기 위해 필요함

    private String filePath;                        // 대표사진 파일 경로

    private final static int MIN_SIZE = 6;          // 아이디/비밀번호 최소 자리수

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_join);

        // 제목
        setTitle(R.string.activity_title_shop_join);

        // 로딩 레이아웃
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

        // 음식 카테고리 구성
        ArrayList<String> categoryItems = new ArrayList<>();
        Collections.addAll(categoryItems, getResources().getStringArray(R.array.food_category));
        categoryItems.add(0, "");
        this.spFoodCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryItems));

        this.imgPhoto.setOnClickListener(mClickListener);
        findViewById(R.id.btnJoin).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);

        // 키보드를 숨기기 위해 필요함
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

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 아이디 입력 체크
        String shopId = this.editShopId.getText().toString();
        if (TextUtils.isEmpty(shopId)) {
            Toast.makeText(this, getString(R.string.msg_shop_id_check_empty), Toast.LENGTH_SHORT).show();
            this.editShopId.requestFocus();
            return false;
        }

        // 아이디 자리수 체크
        if (shopId.length() < MIN_SIZE) {
            Toast.makeText(this, getString(R.string.msg_shop_id_check_length), Toast.LENGTH_SHORT).show();
            this.editShopId.requestFocus();
            return false;
        }

        // 비밀번호 입력 체크
        String password1 = this.editPassword1.getText().toString();
        if (TextUtils.isEmpty(password1)) {
            Toast.makeText(this, getString(R.string.msg_password_check_empty), Toast.LENGTH_SHORT).show();
            this.editPassword1.requestFocus();
            return false;
        }

        // 비밀번호 자리수 체크
        if (password1.length() < MIN_SIZE) {
            Toast.makeText(this, getString(R.string.msg_password_check_length), Toast.LENGTH_SHORT).show();
            this.editPassword1.requestFocus();
            return false;
        }

        // 비밀번호 확인 체크
        String password2 = this.editPassword2.getText().toString();
        if (!password1.equals(password2)) {
            Toast.makeText(this, getString(R.string.msg_password_check_confirm), Toast.LENGTH_SHORT).show();
            this.editPassword2.requestFocus();
            return false;
        }

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

        // 가입일시 (millisecond)
        long joinTimeMillis = System.currentTimeMillis();

        String fileName = "";
        // 대표사진 선택 체크
        if (!TextUtils.isEmpty(this.filePath)) {
            // 파일 확장자명 얻기
            String fileExtension = Utils.getFileExtension(Utils.getFileName(this.filePath));
            // 파일명 현재시간(millisecond) 으로 만들기
            fileName = joinTimeMillis + "." + fileExtension;
        }

        // 가게 위도/경도 얻기
        Point point = Utils.getGpsFromAddress(this, address);

        // 업주(가게) 회원정보
        final Shop shop = new Shop(shopId, password, shopName, phoneNumber, address, foodCategory, menuTable, memo, fileName, point.latitude, point.longitude, joinTimeMillis);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.SHOP);

        // 아이디 중복 체크
        Query query = reference.whereEqualTo("shopId", shopId);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {
                        // 아이디 중복 아님

                        // 회원가입 하기 (자동 문서 ID 값 생성 (컬렉션에 add 하면 document 가 자동 생성됨))
                        db.collection(Constants.FirestoreCollectionName.SHOP)
                                .add(shop)
                                .addOnSuccessListener(documentReference -> {
                                    // 성공

                                    // 대표사진이 있으면
                                    if (TextUtils.isEmpty(shop.getImageFileName())) {
                                        // 대표사진 없음
                                        // 회원가입 완료
                                        complete(shop.getShopId(), shop.getPassword());
                                    } else {
                                        // 대표사진 업로드
                                        uploadFile(documentReference.getId(), shop.getImageFileName(), shop.getShopId(), shop.getPassword());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // 회원가입 실패
                                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                    this.layLoading.setVisibility(View.GONE);
                                    this.executed = false;
                                });
                    } else {
                        // 아이디 중복
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
                // 오류
                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                this.layLoading.setVisibility(View.GONE);
                this.executed = false;
            }
        });
    }

    /* 이미지 파일 올리기 */
    private void uploadFile(String docId, final String fileName, final String shopId, final String password) {
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
            complete(shopId, password);
        }).addOnSuccessListener(taskSnapshot -> {
            // 성공
            Log.d(TAG, "Cloud Storage 성공");
            // 완료
            complete(shopId, password);
        });
    }

    /* 회원가입 완료 */
    private void complete(String shopId, String password) {
        this.layLoading.setVisibility(View.GONE);
        this.executed = false;

        // 로그인 Activity 에 전달 (바로 로그인 되게 하기 위함)
        Intent intent = new Intent();
        intent.putExtra("shop_id", shopId);
        intent.putExtra("password", password);
        setResult(Activity.RESULT_OK, intent);

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
            case R.id.btnJoin:
                // 업주 회원 가입
                if (checkData()) {
                    this.executed = true;
                    this.layLoading.setVisibility(View.VISIBLE);
                    // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // 아이디 중복체크 후 가입
                        join();
                    }, Constants.LoadingDelay.SHORT);
                }
                break;
            case R.id.layLoading:
                // 로딩중 클릭 방지
                break;
        }
    };
}
