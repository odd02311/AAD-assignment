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

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;

    private LinearLayout layLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_select);


        setTitle(R.string.activity_title_member_select);


        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        findViewById(R.id.btnUser).setOnClickListener(mClickListener);
        findViewById(R.id.btnShop).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);


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



                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {

                    Log.d(TAG, "Connect Success");

                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    authWithGoogle(account);
                } catch (ApiException e) {
                    Log.d(TAG, "Fail Connect: " + e.toString());

                    Toast.makeText(this, getString(R.string.msg_google_sign_failure), Toast.LENGTH_LONG).show();
                    this.executed = false;
                }
            }
        }
    }


    private void signInGoogle() {
        this.executed = true;


        FirebaseUser currentUser = this.firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, Constants.RequestCode.GOOGLE_SIGN_IN);
        } else {
            successAuth(currentUser);
        }
    }


    private void authWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        this.firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        Log.d(TAG, "Success");

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        successAuth(user);
                    } else {

                        Log.d(TAG, "Fail");

                        Toast.makeText(this, getString(R.string.msg_google_sign_failure), Toast.LENGTH_LONG).show();
                        executed = false;
                    }
                });
    }


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


            User user = new User(snsUser.getUid(), userName, phoneNumber, email, System.currentTimeMillis());

            this.layLoading.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                login(user);
            }, Constants.LoadingDelay.SHORT);
        } else {
            Log.d(TAG, "user: None");

            Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
            this.executed = false;
        }
    }


    private void login(final User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        Query query = db.collection(Constants.FirestoreCollectionName.USER)
                .whereEqualTo("snsKey", user.getSnsKey()).limit(1);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    if (task.getResult().size() == 0) {

                        join(user);
                    } else {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());

                            this.layLoading.setVisibility(View.GONE);


                            User user1 = document.toObject(User.class);
                            if (user1 != null) {

                                GlobalVariable.userDocumentId = document.getId();
                                GlobalVariable.user = user1;


                                SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.USER);
                                SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, GlobalVariable.userDocumentId);


                                if (TextUtils.isEmpty(user1.getFavoriteCategory())) {

                                    goFavoriteCategorySelect();
                                } else {

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

                    Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                }
            } else {

                Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                this.layLoading.setVisibility(View.GONE);
                this.executed = false;
            }
        });
    }


    private void join(final User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection(Constants.FirestoreCollectionName.USER)
                .add(user)
                .addOnSuccessListener(documentReference -> {

                    this.layLoading.setVisibility(View.GONE);


                    GlobalVariable.userDocumentId = documentReference.getId();
                    GlobalVariable.user = user;


                    SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.USER);
                    SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, GlobalVariable.userDocumentId);


                    goFavoriteCategorySelect();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_LONG).show();
                    this.layLoading.setVisibility(View.GONE);
                    executed = false;
                });
    }


    private void goFavoriteCategorySelect() {
        Intent intent = new Intent(this, FavoriteCategorySelectActivity.class);
        startActivity(intent);

        finish();
    }


    private void goShopLogin() {
        Intent intent = new Intent(this, ShopLoginActivity.class);
        startActivity(intent);

        finish();
    }


    private void goMain() {
        Intent intent = new Intent(this, MainActivity1.class);
        startActivity(intent);

        finish();
    }


    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mClickListener = v -> {
        switch (v.getId()) {
            case R.id.btnUser:

                signInGoogle();
                break;
            case R.id.btnShop:

                goShopLogin();
                break;
            case R.id.layLoading:

                break;
        }
    };
}
