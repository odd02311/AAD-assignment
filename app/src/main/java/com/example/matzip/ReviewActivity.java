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

    // 로딩 레이아웃, 데이터 없을때 표시할 레이아웃
    private LinearLayout layLoading, layNoData;

    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private ArrayList<ReviewItem> items;

    private TextView txtCount, txtPoint;

    private String shopDocId;                       // 가게 Doc Id

    private int selectedPosition;                   // 리스트 선택 위치

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // 가게 doc id
        Intent intent = getIntent();
        this.shopDocId = intent.getStringExtra("shop_doc_id");
        String shopName = intent.getStringExtra("shop_name");

        // 제목 표시
        setTitle(shopName);

        // 홈버튼(<-) 표시
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        // 데이터 없을때 표시할 레이아웃
        this.layNoData = findViewById(R.id.layNoData);

        // 리사이클러뷰
        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.txtCount = findViewById(R.id.txtCount);
        this.txtPoint = findViewById(R.id.txtPoint);

        this.txtCount.setText("");
        this.txtPoint.setText("");

        findViewById(R.id.fabAdd).setOnClickListener(mClickListener);

        this.layLoading.setVisibility(View.VISIBLE);
        // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 리뷰 리스트
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

    /* 리뷰 리스트 */
    private void listReview() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 리뷰 리스트 가져오기 (최근순 정렬)
        Query query = db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .collection(Constants.FirestoreCollectionName.REVIEW)
                .orderBy("inputTimeMillis", Query.Direction.DESCENDING);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    this.items = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Review review = document.toObject(Review.class);
                        // 리뷰 추가
                        this.items.add(new ReviewItem(document.getId(), review));
                    }

                    if (items.size() == 0) {
                        // 리뷰 목록 없으면
                        this.layNoData.setVisibility(View.VISIBLE);
                    } else {
                        this.layNoData.setVisibility(View.GONE);
                    }

                    // 리뷰 개수 및 평점 표시
                    displayPoint();

                    // 리스트에 어뎁터 설정
                    this.adapter = new ReviewAdapter(mAdapterListener, items);
                    this.recyclerView.setAdapter(adapter);
                }
            } else {
                // 오류
                Log.d(TAG, "error:" + task.getException().toString());
            }

            this.layLoading.setVisibility(View.GONE);
        });
    }

    /* 리뷰 등록 */
    private void inputReview(int point, String contents) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 가게 별점 점수 설정
        db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .update("reviewPoint", FieldValue.increment(point), "reviewCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    final Review review = new Review(this.shopDocId, GlobalVariable.userDocumentId, point, contents, System.currentTimeMillis());

                    // 리뷰 등록
                    db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                            .collection(Constants.FirestoreCollectionName.REVIEW)
                            .add(review)
                            .addOnSuccessListener(documentReference -> {
                                // 성공
                                this.layLoading.setVisibility(View.GONE);

                                ReviewItem item = new ReviewItem(documentReference.getId(), review);

                                // 리스트에 최 상단에 추가
                                this.adapter.add(item, 0);
                                this.recyclerView.scrollToPosition(0);

                                // 리뷰 개수 및 평점 표시
                                displayPoint();

                                this.layNoData.setVisibility(View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                // 등록 실패
                                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                this.layLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                });
    }

    /* 리뷰 수정 */
    private void modifyReview(String reviewId, final int point, final String contents) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 가게 별점 점수 설정
        db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .update("reviewPoint", FieldValue.increment(point - this.items.get(this.selectedPosition).review.getPoint()))
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    // 리뷰 수정
                    db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                            .collection(Constants.FirestoreCollectionName.REVIEW).document(reviewId)
                            .update("point", point, "contents", contents)
                            .addOnSuccessListener(bVoid -> {
                                // 성공
                                this.layLoading.setVisibility(View.GONE);

                                // 변경된 리뷰 적용
                                this.items.get(this.selectedPosition).review.setPoint(point);
                                this.items.get(this.selectedPosition).review.setContents(contents);
                                this.adapter.notifyItemChanged(this.selectedPosition);

                                // 리뷰 개수 및 평점 표시
                                displayPoint();
                            })
                            .addOnFailureListener(e -> {
                                // 실패
                                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                this.layLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                });
    }

    /* 리뷰 삭제 */
    private void deleteReview(String reviewId) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 가게 별점 점수 설정
        db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                .update("reviewPoint", FieldValue.increment(-this.items.get(this.selectedPosition).review.getPoint()), "reviewCount", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {
                    // 성공
                    // 리뷰 삭제
                    db.collection(Constants.FirestoreCollectionName.SHOP).document(this.shopDocId)
                            .collection(Constants.FirestoreCollectionName.REVIEW).document(reviewId).delete()
                            .addOnSuccessListener(bVoid -> {
                                // 성공
                                this.layLoading.setVisibility(View.GONE);

                                // 리스트에서 삭제
                                this.adapter.remove(this.selectedPosition);

                                // 리뷰 개수 및 평점 표시
                                displayPoint();

                                if (this.items.size() == 0) {
                                    this.layNoData.setVisibility(View.VISIBLE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                // 실패
                                Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                                this.layLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    // 실패
                    Toast.makeText(this, R.string.msg_error, Toast.LENGTH_SHORT).show();
                    this.layLoading.setVisibility(View.GONE);
                });
    }

    /* 리뷰 개수 및 평점 표시 */
    private void displayPoint() {
        this.txtCount.setText(String.valueOf(this.items.size()));

        int point = 0;
        for (ReviewItem item : this.items) {
            point += item.review.getPoint();
        }

        double pointAvg = 0;
        if (this.items.size() > 0) {
            // 평점 표시 (소수점 한자리까지 표시 (반올림))
            pointAvg = (Math.round(((double) point / this.items.size()) * 10) / 10.0);
        }
        this.txtPoint.setText(String.valueOf(pointAvg));
    }

    /* 리뷰 등록 팝업창 호출 */
    private void onPopupReview(int point, String contents) {
        View popupView = View.inflate(this, R.layout.popup_review, null);
        ReviewPopup popup = new ReviewPopup(popupView, point, contents);
        popup.setClickListener(mCListener);
        // Back 키 눌렸을때 닫기 위함
        popup.setFocusable(true);
        popup.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    /* 리뷰 클릭 리스너 */
    private final IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {
            // 삭제 (롱클릭시 이벤트 발생)
            selectedPosition = bundle.getInt("position");

            final String reviewId = items.get(selectedPosition).id;

            new AlertDialog.Builder(ReviewActivity.this)
                    .setPositiveButton(getString(R.string.dialog_Yes), (dialog, which) -> {
                        layLoading.setVisibility(View.VISIBLE);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // 리뷰 삭제
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
                // 수정하기
                selectedPosition = bundle.getInt("position");

                int point = items.get(selectedPosition).review.getPoint();
                String contents = items.get(selectedPosition).review.getContents();

                // 리뷰 수정 팝업창 호출
                onPopupReview(point, contents);
            }
        }
    };

    /* 팝업창에서 사용할 클릭 리스너 */
    private final IClickListener mCListener = new IClickListener() {
        @Override
        public void onClick(Bundle bundle, int id) {
            if (id == R.id.btnOk) {
                final int mode = bundle.getInt("mode");
                final int point = bundle.getInt("point");
                final String contents = bundle.getString("contents");

                layLoading.setVisibility(View.VISIBLE);
                // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // 등록 및 수정
                    if (mode == 0) {
                        // 등록
                        inputReview(point, contents);
                    } else {
                        // 수정
                        String reviewId = items.get(selectedPosition).id;
                        modifyReview(reviewId, point, contents);
                    }
                }, Constants.LoadingDelay.SHORT);
            }
        }
    };

    /* 클릭 리스터 */
    private final View.OnClickListener mClickListener = view -> {
        if (view.getId() == R.id.fabAdd) {
            // 리뷰 등록 팝업창 호출
            onPopupReview(0, "");
        }
    };
}
