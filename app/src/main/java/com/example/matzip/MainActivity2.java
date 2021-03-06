package com.example.matzip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.matzip.util.Constants;
import com.example.matzip.util.SharedPreferencesUtils;

public class MainActivity2 extends AppCompatActivity {
    private static final String TAG = MainActivity2.class.getSimpleName();

    private BackPressHandler backPressHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);


        setTitle(R.string.activity_title_main_2);


        this.backPressHandler = new BackPressHandler(this);

        findViewById(R.id.btnEdit).setOnClickListener(mClickListener);
    }

    @Override
    public void onBackPressed() {
        this.backPressHandler.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {

            new AlertDialog.Builder(this)
                    .setPositiveButton(getString(R.string.dialog_Yes), (dialog, which) -> {

                        logout();
                    })
                    .setNegativeButton(getString(R.string.dialog_cancel), null)
                    .setCancelable(false)
                    .setTitle(getString(R.string.dialog_title_logout))
                    .setMessage(getString(R.string.dialog_msg_logout))
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void logout() {

        SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.SHOP_DOCUMENT_ID, "");
        SharedPreferencesUtils.getInstance(this).put(Constants.SharedPreferencesName.MEMBER_KIND, Constants.MemberKind.NONE);


        Intent intent = new Intent(this, ShopLoginActivity.class);
        startActivity(intent);
        finish();
    }


    private final View.OnClickListener mClickListener = v -> {
        if (v.getId() == R.id.btnEdit) {

            Intent intent = new Intent(this, ShopEditActivity.class);
            startActivity(intent);
        }
    };


    private class BackPressHandler {
        private Context context;
        private Toast toast;

        private long backPressedTime = 0;

        public BackPressHandler(Context context) {
            this.context = context;
        }

        public void onBackPressed() {
            if (System.currentTimeMillis() > this.backPressedTime + (Constants.LoadingDelay.LONG * 2)) {
                this.backPressedTime = System.currentTimeMillis();

                this.toast = Toast.makeText(this.context, R.string.msg_back_press_end, Toast.LENGTH_SHORT);
                this.toast.show();

                return;
            }

            if (System.currentTimeMillis() <= this.backPressedTime + (Constants.LoadingDelay.LONG * 2)) {

                moveTaskToBack(true);
                finish();
                this.toast.cancel();
            }
        }
    }
}
