package com.example.matzip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.matzip.entity.User;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.example.matzip.util.SharedPreferencesUtils;
import com.example.matzip.util.Utils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MemberSelectActivity extends AppCompatActivity {
    private static final String TAG = MemberSelectActivity.class.getSimpleName();

    private boolean executed = false;

    private GoogleSignInClient googleSignInClient;      // 구글 api 클라이언트
    private FirebaseAuth firebaseAuth;                  // 파이어베이스 인증 객체 생성

    private LinearLayout layLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_select);

        // 제목
        setTitle(R.string.activity_title_member_select);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        findViewById(R.id.btnUser).setOnClickListener(mClickListener);
        findViewById(R.id.btnShop).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);

        // 파이어베이스 인증 객체 선언
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.googleSignInClient = GoogleSignIn.getClient(this,
                Utils.getGoogleSignInOptions(getString(R.string.default_web_client_id)));
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
            if (requestCode == Constants.RequestCode.GOOGLE_SIGN_IN) {
                // 구글 연동

                // 구글로그인 버튼 응답
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    // 구글 로그인 성공
                    Log.d(TAG, "연동 성공");

                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    authWithGoogle(account);
                } catch (ApiException e) {
                    Log.d(TAG, "연동 실패: " + e.toString());

                    Toast.makeText(this, getString(R.string.msg_google_sign_failure), Toast.LENGTH_LONG).show();
                    this.executed = false;
                }
            }
        }
    }

    /* 일반회원 구글 연동 */
    private void signInGoogle() {
        this.executed = true;

        // 현재 연동되어 있는지 확인
        FirebaseUser currentUser = this.firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, Constants.RequestCode.GOOGLE_SIGN_IN);
        } else {
            successAuth(currentUser);
        }
    }

    // 사용자가 정상적으로 로그인한 후에 GoogleSignInAccount 개체에서 ID 토큰을 가져와서
    // Firebase 사용자 인증 정보로 교환하고 Firebase 사용자 인증 정보를 사용해 Firebase 에 인증합니다.
    private void authWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        this.firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 로그인 성공
                        Log.d(TAG, "성공");

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        successAuth(user);
                    } else {
                        // 로그인 실패
                        Log.d(TAG, "실패");

                        Toast.makeText(this, getString(R.string.msg_google_sign_failure), Toast.LENGTH_LONG).show();
                        executed = false;
                    }
                });
    }

    /* 구글 인증 성공 */
    private void successAuth(FirebaseUser snsUser) {
        if (snsUser != null) {
            Log.d(TAG, "Uid: " + snsUser.getUid());
            Log.d(TAG, "Email: " + snsUser.getEmail());
            Log.d(TAG, "DisplayName: " + snsUser.getDisplayName());
            Log.d(TAG, "PhoneNumber: " + snsUser.getPhoneNumber());
            Log.d(TAG, "PhotoUrl: " + snsUser.getPhotoUrl());

            String userName = snsUser.getDisplayName();
            if (TextUtils.isEmpty(userName)) {
                userName = "None";
            }

            String phoneNumber = snsUser.getPhoneNumber();
            if (TextUtils.isEmpty(phoneNumber)) {
                phoneNumber = "";
            }

            String email = snsUser.getEmail();
            if (TextUtils.isEmpty(email)) {
                email = "";
            }

            // 사용자 정보
            User user = new User(snsUser.getUid(), userName, phoneNumber, email, System.currentTimeMillis());

            this.layLoading.setVisibility(View.VISIBLE);
            // 로딩 레이아웃을 표시하기 위해 딜레이 적용
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 일반회원 로그인
                login(user);
            }, Constants.LoadingDelay.SHORT);
        } else {
            Log.d(TAG, "user: 없음");

            Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
            this.executed = false;
        }
    }

    /* 일반회원 로그인 */
    private void login(final User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 로그인
        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .whereEqualTo("snsKey", user.getSnsKey()).limit(1);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {
                        // 로그인 실패 (회원가입하기)
                        join(user);
                    } else {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());

                            this.layLoading.setVisibility(View.GONE);

                            // 일반회원
                            User user1 = document.toObject(User.class);
                            if (user1 != null) {
                                // Document Id 저장
                                GlobalVariable.userDocumentId = document.getId();
                                GlobalVariable.user = user1;

                                // SharedPreferences 에 록그인 정보 저장 (자동 로그인 기능)
                                SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.USER);
                                SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, GlobalVariable.userDocumentId);

                                // 좋아하는 음식 카테고리를 선택하지 않았으면
                                if (TextUtils.isEmpty(user1.getFavoriteCategory())) {
                                    // 좋아하는 음식 카테고리 선택화면으로 이동
                                    goFavoriteCategorySelect();
                                } else {
                                    // 메인으로 이동
                                    goMain();
                                }
                            } else {
                                Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                                this.executed = false;
                            }
                            break;
                        }
                    }
                } else {
                    // 오류
                    Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                }
            } else {
                // 오류
                Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                this.layLoading.setVisibility(View.GONE);
                this.executed = false;
            }
        });
    }

    /* SNS 로그인을 통해 회원가입 */
    private void join(final User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 회원가입
        db.collection(Constants.FirestoreCollectionName.USER)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    // 성공
                    this.layLoading.setVisibility(View.GONE);

                    // Document Id 저장
                    GlobalVariable.userDocumentId = documentReference.getId();
                    GlobalVariable.user = user;

                    // SharedPreferences 에 록그인 정보 저장 (자동 로그인 기능)
                    SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.USER);
                    SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, GlobalVariable.userDocumentId);

                    // 좋아하는 음식 카테고리 선택화면으로 이동
                    goFavoriteCategorySelect();
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                    this.layLoading.setVisibility(View.GONE);
                    executed = false;
                });
    }

    /* 좋아하는 음식 카테고리 선택화면으로 이동 */
    private void goFavoriteCategorySelect() {
        Intent intent = new Intent(this, FavoriteCategorySelectActivity.class);
        startActivity(intent);

        finish();
    }

    /* 업주회원 로그인화면으로 이동 */
    private void goShopLogin() {
        Intent intent = new Intent(this, ShopLoginActivity.class);
        startActivity(intent);

        finish();
    }

    /* 일반회원 메인화면으로 이동 */
    private void goMain() {
        Intent intent = new Intent(this, MainActivity1.class);
        startActivity(intent);

        finish();
    }

    /* 클릭 리스너 */
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.btnUser:
                // 일반회원 구글 연동
                signInGoogle();
                break;
            case R.id.btnShop:
                // 업주회원
                goShopLogin();
                break;
            case R.id.layLoading:
                // 로딩중 클릭 방지
                break;
        }
    };
}
