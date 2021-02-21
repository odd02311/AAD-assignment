package com.example.matzip;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.matzip.entity.Shop;
import com.example.matzip.entity.User;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.example.matzip.util.SharedPreferencesUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class IntroActivity extends AppCompatActivity {
    private static final String TAG = IntroActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 툴바 안보이게 하기 위함
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_intro);

        // 인트로 화면을 일정시간 보여줌
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 초기화
            IntroActivityPermissionsDispatcher.initWithPermissionCheck(this);
        }, Constants.LoadingDelay.LONG);
    }

    @Override
    public void onBackPressed() {
        // 백키 눌려도 종료 안되게 하기 위함
        //super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        IntroActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /* 초기화  */
    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE})
    void init() {
        SharedPreferencesUtils preferencesUtils = SharedPreferencesUtils.getInstance(this);

        // 회원 종류
        int kind = preferencesUtils.get(Constants.SharedPreferencesName.MEMBER_KIND, -1);
        String docId = "";

        switch (kind) {
            case Constants.MemberKind.USER:
                // 일반
                docId = preferencesUtils.get(Constants.SharedPreferencesName.USER_DOCUMENT_ID);
                break;
            case Constants.MemberKind.SHOP:
                // 업주(가게)
                docId = preferencesUtils.get(Constants.SharedPreferencesName.SHOP_DOCUMENT_ID);
                break;
        }

        Log.d(TAG, "docId:" + docId);

        if (!TextUtils.isEmpty(docId)) {
            // 회원 Doc Id 값이 있으면 자동 로그인
            login(kind, docId);
        } else {
            // 회원선택 화면으로 이동
            goMemberSelect();
        }
    }

    @OnShowRationale({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton(getString(R.string.dialog_allow), (dialog, which) -> request.proceed())
                .setNegativeButton(getString(R.string.dialog_deny), (dialog, which) -> request.cancel())
                .setCancelable(false)
                .setMessage(getString(R.string.permission_rationale_app_use))
                .show();
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showDenied() {
        Toast.makeText(this, getString(R.string.permission_rationale_app_use), Toast.LENGTH_LONG).show();
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE})
    void showNeverAsk() {
        Toast.makeText(this, getString(R.string.permission_rationale_app_use), Toast.LENGTH_LONG).show();
    }

    /* 로그인 */
    private void login(final int kind, final String id) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String collectionName = Constants.FirestoreCollectionName.USER;
        if (kind == Constants.MemberKind.SHOP) {
            // 업주(가게)
            collectionName = Constants.FirestoreCollectionName.SHOP;
        }

        DocumentReference reference = db.collection(collectionName).document(id);
        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 성공
                DocumentSnapshot document = task.getResult();

                if (document != null) {
                    if (kind == Constants.MemberKind.USER) {
                        // 일반회원
                        GlobalVariable.userDocumentId = document.getId();
                        // 일반회원 객체 생성
                        GlobalVariable.user = document.toObject(User.class);

                        // 좋아하는 음식 카테고리를 선택하지 않았으면
                        if (TextUtils.isEmpty(GlobalVariable.user.getFavoriteCategory())) {
                            // 좋아하는 음식 카테고리 선택화면으로 이동
                            goFavoriteCategorySelect();
                        } else {
                            // 메인으로 이동
                            goMain(kind);
                        }
                    } else {
                        // 업주회원
                        GlobalVariable.shopDocumentId = document.getId();
                        // 업주회원 객체 생성
                        GlobalVariable.shop = document.toObject(Shop.class);

                        // 메인으로 이동
                        goMain(kind);
                    }
                } else {
                    // 회원선택 화면으로 이동
                    goMemberSelect();
                }
            } else {
                // 회원선택 화면으로 이동
                goMemberSelect();
            }
        });
    }

    /* 회원선택 화면으로 이동 */
    private void goMemberSelect() {
        Intent intent = new Intent(this, MemberSelectActivity.class);
         startActivity(intent);

        finish();
    }

    /* 좋아하는 음식 카테고리 선택화면으로 이동 */
    private void goFavoriteCategorySelect() {
        Intent intent = new Intent(this, FavoriteCategorySelectActivity.class);
        startActivity(intent);

        finish();
    }

    /* 메인화면으로 이동 */
    private void goMain(int kind) {
        Intent intent;

        if (kind == Constants.MemberKind.USER) {
            // 일반
            intent = new Intent(this, MainActivity1.class);
        } else {
            // 업주
            intent = new Intent(this, MainActivity2.class);
        }
        startActivity(intent);

        finish();
    }
}
