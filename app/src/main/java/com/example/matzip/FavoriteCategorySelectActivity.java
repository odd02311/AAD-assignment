package com.example.matzip;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.example.matzip.adapter.FoodCategoryAdapter;
import com.example.matzip.listener.IAdapterOnClickListener;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class FavoriteCategorySelectActivity extends AppCompatActivity {
    private static final String TAG = FavoriteCategorySelectActivity.class.getSimpleName();

    private boolean executed = false;

    private LinearLayout layLoading;

    private RecyclerView recyclerView;

    private boolean isEdit;                 // 편집 여부

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_category_select);

        // 편집모드 여부
        Intent intent = getIntent();
        this.isEdit = intent.getBooleanExtra("edit", false);

        // 제목
        setTitle(R.string.activity_title_favorite_category_select);

        if (this.isEdit) {
            // 홈버튼(<-) 표시
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        }

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        // 리사이클러뷰
        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // 음식 카테고리 목록
        listFoodCategory();
    }

    @Override
    public void onBackPressed() {
        if (this.executed) {
            return;
        }

        if (this.isEdit) {
            super.onBackPressed();
        } else {
            // 회원가입 후 설정모드
            moveTaskToBack(true);
            finish();
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

    /* 음식 카테고리 목록 */
    private void listFoodCategory() {
        ArrayList<String> items = new ArrayList<>();
        Collections.addAll(items, getResources().getStringArray(R.array.food_category));

        // 리스트에 어뎁터 설정
        FoodCategoryAdapter adapter = new FoodCategoryAdapter(mAdapterListener, items);
        this.recyclerView.setAdapter(adapter);
    }

    /* 좋아하는 카테고리 설정 */
    private void setFavoriteCategory(final String favoriteCategory) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 일반회원 document 참조
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.userDocumentId);
        // 좋아하는 카테고리 설정
        reference.update("favoriteCategory", favoriteCategory)
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    layLoading.setVisibility(View.GONE);

                    GlobalVariable.user.setFavoriteCategory(favoriteCategory);

                    if (this.isEdit) {
                        // 설정 Activity 에 전달
                        Intent intent = new Intent();
                        intent.putExtra("category", favoriteCategory);
                        setResult(Activity.RESULT_OK, intent);

                        finish();
                    } else {
                        // 메인으로 이동
                        goMain();
                    }
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                });
    }

    /* 일반회원 메인화면으로 이동 */
    private void goMain() {
        Intent intent = new Intent(this, MainActivity1.class);
        startActivity(intent);

        finish();
    }

    /* 장소  클릭 리스너 */
    private final IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {
            // 카테고리 선택
            String category = bundle.getString("category");
            if (!TextUtils.isEmpty(category)) {
                executed = true;
                layLoading.setVisibility(View.VISIBLE);
                // 로딩 레이아웃을 표시하기 위해 딜레이 적용
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // 좋아하는 카테고리 설정
                    setFavoriteCategory(category);
                }, Constants.LoadingDelay.SHORT);
            }
        }

        @Override
        public void onButtonClick(Bundle bundle, int id) {
        }
    };
}
