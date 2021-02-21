package com.example.matzip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.matzip.entity.Shop;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.example.matzip.util.SharedPreferencesUtils;
import com.example.matzip.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ShopLoginActivity extends AppCompatActivity {
    private static final String TAG = ShopLoginActivity.class.getSimpleName();

    private boolean executed = false;

    private LinearLayout layLoading;
    private EditText editShopId, editPassword;

    private InputMethodManager imm;                 // 키보드를 숨기기 위해 필요함

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_login);

        // 제목
        setTitle(R.string.activity_title_shop_join);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.editShopId = findViewById(R.id.editShopId);
        this.editShopId.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        this.editPassword = findViewById(R.id.editPassword);
        this.editPassword.setImeOptions(EditorInfo.IME_ACTION_DONE);

        findViewById(R.id.btnLogin).setOnClickListener(mClickListener);
        findViewById(R.id.btnSignUp).setOnClickListener(mClickListener);
        this.layLoading.setOnClickListener(mClickListener);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        this.editShopId.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (!this.executed) {
            moveTaskToBack(true);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.RequestCode.JOIN) {
                // 회원가입 이후 로그인하기
                if (data != null) {
                    this.editShopId.setText(data.getStringExtra("shop_id"));
                    this.editPassword.setText(data.getStringExtra("password"));

                    this.layLoading.setVisibility(View.VISIBLE);
                    // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // 로그인
                        login();
                    }, Constants.LoadingDelay.SHORT);
                }
            }
        }
    }

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 아이디 입력 체크
        String shopId = this.editShopId.getText().toString();
        if (TextUtils.isEmpty(shopId)) {
            Toast.makeText(this, R.string.msg_shop_id_check_empty, Toast.LENGTH_SHORT).show();
            this.editShopId.requestFocus();
            return false;
        }

        // 비밀번호 입력 체크
        String password = this.editPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.msg_password_check_empty, Toast.LENGTH_SHORT).show();
            this.editPassword.requestFocus();
            return false;
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editPassword.getWindowToken(), 0);

        return true;
    }

    /* 로그인 */
    private void login() {
        String shopId = this.editShopId.getText().toString();
        final String password = this.editPassword.getText().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.SHOP);

        // 로그인
        Query query = reference.whereEqualTo("shopId", shopId).limit(1);
        query.get().addOnCompleteListener(task -> {
            this.layLoading.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {
                        // 로그인 실패 (회원이 아님)
                        Toast.makeText(this, getString(R.string.msg_login_shop_none), Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            Shop shop = document.toObject(Shop.class);
                            if (shop.getPassword().equals(password)) {
                                // 로그인 성공

                                // Document Id 저장
                                GlobalVariable.shopDocumentId = document.getId();
                                GlobalVariable.shop = shop;

                                // SharedPreferences 에 록그인 정보 저장 (자동 로그인 기능)
                                SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.SHOP);
                                SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.SHOP_DOCUMENT_ID, GlobalVariable.shopDocumentId);

                                // 메인 화면으로 이동
                                goMain();
                            } else {
                                // 로그인 실패 (비밀번호 틀림)
                                Toast.makeText(this, R.string.msg_login_password_wrong, Toast.LENGTH_SHORT).show();
                                this.executed = false;
                            }
                            break;
                        }
                    }
                } else {
                    // 오류
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.executed = false;
                }
            } else {
                // 오류
                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                this.executed = false;
            }
        });
    }

    /* 메인화면으로 이동 */
    private void goMain() {
        Intent intent = new Intent(this, MainActivity2.class);
        startActivity(intent);

        finish();
    }

    /* 업주(가게) 회원가입화면으로 이동 */
    private void goJoin() {
        Intent intent = new Intent(this, ShopJoinActivity.class);
        startActivityForResult(intent, Constants.RequestCode.JOIN);
    }

    /* 클릭 리스너 */
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.btnLogin:
                // 로그인
                if (checkData()) {
                    this.executed = true;
                    this.layLoading.setVisibility(View.VISIBLE);
                    // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // 로그인
                        login();
                    }, Constants.LoadingDelay.SHORT);
                }
                break;
            case R.id.btnSignUp:
                // 회원가입 화면으로 이동
                goJoin();
                break;
            case R.id.layLoading:
                // 로딩중 클릭 방지
                break;
        }
    };
}
