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

    private boolean isEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_category_select);


        Intent intent = getIntent();
        this.isEdit = intent.getBooleanExtra("edit", false);


        setTitle(R.string.activity_title_favorite_category_select);

        if (this.isEdit) {

            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        }


        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));


        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));


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


    private void listFoodCategory() {
        ArrayList<String> items = new ArrayList<>();
        Collections.addAll(items, getResources().getStringArray(R.array.food_category));


        FoodCategoryAdapter adapter = new FoodCategoryAdapter(mAdapterListener, items);
        this.recyclerView.setAdapter(adapter);
    }


    private void setFavoriteCategory(final String favoriteCategory) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.userDocumentId);

        reference.update("favoriteCategory", favoriteCategory)
                .addOnSuccessListener(aVoid -> {

                    layLoading.setVisibility(View.GONE);

                    GlobalVariable.user.setFavoriteCategory(favoriteCategory);

                    if (this.isEdit) {

                        Intent intent = new Intent();
                        intent.putExtra("category", favoriteCategory);
                        setResult(Activity.RESULT_OK, intent);

                        finish();
                    } else {

                        goMain();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                    this.executed = false;
                });
    }


    private void goMain() {
        Intent intent = new Intent(this, MainActivity1.class);
        startActivity(intent);

        finish();
    }


    private final IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {

            String category = bundle.getString("category");
            if (!TextUtils.isEmpty(category)) {
                executed = true;
                layLoading.setVisibility(View.VISIBLE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    setFavoriteCategory(category);
                }, Constants.LoadingDelay.SHORT);
            }
        }

        @Override
        public void onButtonClick(Bundle bundle, int id) {
        }
    };
}
