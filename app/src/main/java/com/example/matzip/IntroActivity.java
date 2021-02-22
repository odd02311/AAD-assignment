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


        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_intro);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            IntroActivityPermissionsDispatcher.initWithPermissionCheck(this);
        }, Constants.LoadingDelay.LONG);
    }

    @Override
    public void onBackPressed() {

        //super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        IntroActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }


    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE})
    void init() {
        SharedPreferencesUtils preferencesUtils = SharedPreferencesUtils.getInstance(this);


        int kind = preferencesUtils.get(Constants.SharedPreferencesName.MEMBER_KIND, -1);
        String docId = "";

        switch (kind) {
            case Constants.MemberKind.USER:

                docId = preferencesUtils.get(Constants.SharedPreferencesName.USER_DOCUMENT_ID);
                break;
            case Constants.MemberKind.SHOP:

                docId = preferencesUtils.get(Constants.SharedPreferencesName.SHOP_DOCUMENT_ID);
                break;
        }

        Log.d(TAG, "docId:" + docId);

        if (!TextUtils.isEmpty(docId)) {

            login(kind, docId);
        } else {

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


    private void login(final int kind, final String id) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String collectionName = Constants.FirestoreCollectionName.USER;
        if (kind == Constants.MemberKind.SHOP) {

            collectionName = Constants.FirestoreCollectionName.SHOP;
        }

        DocumentReference reference = db.collection(collectionName).document(id);
        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                DocumentSnapshot document = task.getResult();

                if (document != null) {
                    if (kind == Constants.MemberKind.USER) {

                        GlobalVariable.userDocumentId = document.getId();

                        GlobalVariable.user = document.toObject(User.class);


                        if (TextUtils.isEmpty(GlobalVariable.user.getFavoriteCategory())) {

                            goFavoriteCategorySelect();
                        } else {

                            goMain(kind);
                        }
                    } else {

                        GlobalVariable.shopDocumentId = document.getId();

                        GlobalVariable.shop = document.toObject(Shop.class);


                        goMain(kind);
                    }
                } else {

                    goMemberSelect();
                }
            } else {

                goMemberSelect();
            }
        });
    }


    private void goMemberSelect() {
        Intent intent = new Intent(this, MemberSelectActivity.class);
         startActivity(intent);

        finish();
    }


    private void goFavoriteCategorySelect() {
        Intent intent = new Intent(this, FavoriteCategorySelectActivity.class);
        startActivity(intent);

        finish();
    }


    private void goMain(int kind) {
        Intent intent;

        if (kind == Constants.MemberKind.USER) {

            intent = new Intent(this, MainActivity1.class);
        } else {

            intent = new Intent(this, MainActivity2.class);
        }
        startActivity(intent);

        finish();
    }
}
