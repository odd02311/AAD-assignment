package com.example.matzip;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.Objects;

public class ImageViewActivity extends AppCompatActivity {
    private static final String TAG = ImageViewActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        // 이미지 정보
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String filePath = intent.getStringExtra("file_path");

        setTitle(title);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ImageView imgPhoto = findViewById(R.id.imgPhoto);

        // 이미지 표시
        Glide.with(this)
                .load("file://" + filePath)
                .error(R.drawable.ic_alert_circle_24_gray)
                .apply(new RequestOptions().fitCenter())
                .transition(new DrawableTransitionOptions().crossFade())
                .into(imgPhoto);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
