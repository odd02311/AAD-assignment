package com.example.matzip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.example.matzip.adapter.ReviewAdapter;
import com.example.matzip.entity.Review;
import com.example.matzip.entity.ReviewItem;
import com.example.matzip.listener.IAdapterOnClickListener;
import com.example.matzip.listener.IClickListener;
import com.example.matzip.popupwindow.ReviewPopup;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

public class ReviewActivity extends AppCompatActivity {
    private static final String TAG = ReviewActivity.class.getSimpleName();


    private LinearLayout layLoading, layNoData;

    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private ArrayList<ReviewItem> items;

    private TextView txtCount, txtPoint;

    private String shopDocId;

    private int selectedPosition;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);


        Intent intent = getIntent();
        this.shopDocId = intent.getStringExtra("shop_doc_id");
        String shopName = intent.getStringExtra("shop_name");


        setTitle(shopName);


        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));


        this.layNoData = findViewById(R.id.layNoData);


        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.txtCount = findViewById(R.id.txtCount);
        this.txtPoint = findViewById(R.id.txtPoint);

        this.txtCount.setText("");
        this.txtPoint.setText("");

        findViewById(R.id.fabAdd).setOnClickListener(mClickListener);

        this.layLoading.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            listReview();
        }, Constants.LoadingDelay.SHORT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void listReview() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        Query query = db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .collection(Constants.FirestoreCollectionName.REVIEW)
                .orderBy("inputTimeMillis", Query.Direction.DESCENDING);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.items = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Review review = document.toObject(Review.class);

                        this.items.add(new ReviewItem(document.getId(), review));
                    }

                    if (items.size() == 0) {

                        this.layNoData.setVisibility(View.VISIBLE);
                    } else {
                        this.layNoData.setVisibility(View.GONE);
                    }


                    displayPoint();


                    this.adapter = new ReviewAdapter(mAdapterListener, items);
                    this.recyclerView.setAdapter(adapter);
                }
            } else {

                Log.d(TAG, "error:" + task.getException().toString());
            }

            this.layLoading.setVisibility(View.GONE);
        });
    }


    private void inputReview(int point, String contents) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .update("reviewPoint", FieldValue.increment(point), "reviewCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {

                    final Review review = new Review(this.shopDocId, GlobalVariable.userDocumentId, point, contents, System.currentTimeMillis());


                    db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                            .collection(Constants.FirestoreCollectionName.REVIEW)
                            .add(review)
                            .addOnSuccessListener(documentReference -> {

                                this.layLoading.setVisibility(View.GONE);

                                ReviewItem item = new ReviewItem(documentReference.getId(), review);


                                this.adapter.add(item, 0);
                                this.recyclerView.scrollToPosition(0);


                                displayPoint();

                                this.layNoData.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                this.layLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                });
    }


    private void modifyReview(String reviewId, final int point, final String contents) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .update("reviewPoint", FieldValue.increment(point - this.items.get(this.selectedPosition).review.getPoint()))
                .addOnSuccessListener(aVoid -> {

                    db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                            .collection(Constants.FirestoreCollectionName.REVIEW).document(reviewId)
                            .update("point", point, "contents", contents)
                            .addOnSuccessListener(bVoid -> {

                                this.layLoading.setVisibility(View.GONE);


                                this.items.get(this.selectedPosition).review.setPoint(point);
                                this.items.get(this.selectedPosition).review.setContents(contents);
                                this.adapter.notifyItemChanged(this.selectedPosition);


                                displayPoint();
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                this.layLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                });
    }


    private void deleteReview(String reviewId) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .update("reviewPoint", FieldValue.increment(-this.items.get(this.selectedPosition).review.getPoint()), "reviewCount", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {

                    db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                            .collection(Constants.FirestoreCollectionName.REVIEW).document(reviewId).delete()
                            .addOnSuccessListener(bVoid -> {

                                this.layLoading.setVisibility(View.GONE);


                                this.adapter.remove(this.selectedPosition);


                                displayPoint();

                                if (this.items.size() == 0) {
                                    this.layNoData.setVisibility(View.VISIBLE);
                                }
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                this.layLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                });
    }


    private void displayPoint() {
        this.txtCount.setText(String.valueOf(this.items.size()));

        int point = 0;
        for (ReviewItem item : this.items) {
            point += item.review.getPoint();
        }

        double pointAvg = 0;
        if (this.items.size() > 0) {

            pointAvg = (Math.round(((double) point / this.items.size()) * 10) / 10.0);
        }
        this.txtPoint.setText(String.valueOf(pointAvg));
    }


    private void onPopupReview(int point, String contents) {
        View popupView = View.inflate(this, R.layout.popup_review, null);
        ReviewPopup popup = new ReviewPopup(popupView, point, contents);
        popup.setClickListener(mCListener);

        popup.setFocusable(true);
        popup.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }


    private final IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {

            selectedPosition = bundle.getInt("position");

            final String reviewId = items.get(selectedPosition).id;

            new AlertDialog.Builder(ReviewActivity.this)
                    .setPositiveButton(getString(R.string.dialog_Yes), (dialog, which) -> {
                        layLoading.setVisibility(View.VISIBLE);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {

                            deleteReview(reviewId);
                        }, Constants.LoadingDelay.SHORT);
                    })
                    .setNegativeButton(getString(R.string.dialog_cancel), null)
                    .setCancelable(false)
                    .setTitle(getString(R.string.dialog_title_review_delete))
                    .setMessage(getString(R.string.dialog_msg_review_delete))
                    .show();
        }

        @Override
        public void onButtonClick(Bundle bundle, int id) {
            if (id == R.id.imgEdit) {

                selectedPosition = bundle.getInt("position");

                int point = items.get(selectedPosition).review.getPoint();
                String contents = items.get(selectedPosition).review.getContents();


                onPopupReview(point, contents);
            }
        }
    };


    private final IClickListener mCListener = new IClickListener() {
        @Override
        public void onClick(Bundle bundle, int id) {
            if (id == R.id.btnOk) {
                final int mode = bundle.getInt("mode");
                final int point = bundle.getInt("point");
                final String contents = bundle.getString("contents");

                layLoading.setVisibility(View.VISIBLE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    if (mode == 0) {

                        inputReview(point, contents);
                    } else {

                        String reviewId = items.get(selectedPosition).id;
                        modifyReview(reviewId, point, contents);
                    }
                }, Constants.LoadingDelay.SHORT);
            }
        }
    };


    private final View.OnClickListener mClickListener = view -> {
        if (view.getId() == R.id.fabAdd) {

            onPopupReview(0, "");
        }
    };
}
