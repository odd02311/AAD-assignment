package com.example.matzip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;

import java.util.Objects;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = SettingActivity.class.getSimpleName();

    private TextView txtFavoriteCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 제목
        setTitle(R.string.activity_title_setting);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        this.txtFavoriteCategory = findViewById(R.id.txtFavoriteCategory);
        this.txtFavoriteCategory.setText(GlobalVariable.user.getFavoriteCategory());

        findViewById(R.id.layFavoriteCategory).setOnClickListener(mClickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.RequestCode.FAVORITE_CATEGORY) {
                // 좋아하는 카테고리 설정 이후
                if (data != null) {
                    String category = data.getStringExtra("category");
                    if (!TextUtils.isEmpty(category)) {
                        GlobalVariable.user.setFavoriteCategory(category);
                        this.txtFavoriteCategory.setText(category);
                    }
                }
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

    /* 클릭 리스너 */
    private final View.OnClickListener mClickListener = view -> {
        if (view.getId() == R.id.layFavoriteCategory) {
            // 좋아하는 카테고리 설정
            Intent intent = new Intent(this, FavoriteCategorySelectActivity.class);
            intent.putExtra("edit", true);
            startActivityForResult(intent, Constants.RequestCode.FAVORITE_CATEGORY);
        }
    };
}
