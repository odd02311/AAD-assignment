package com.example.matzip.adapter;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.matzip.R;
import com.example.matzip.entity.ReviewItem;
import com.example.matzip.listener.IAdapterOnClickListener;
import com.example.matzip.util.Constants;
import com.example.matzip.util.GlobalVariable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private static final String TAG = ReviewAdapter.class.getSimpleName();

    private IAdapterOnClickListener listener;
    private ArrayList<ReviewItem> items;

    public ReviewAdapter(IAdapterOnClickListener listener, ArrayList<ReviewItem> items) {
        this.listener = listener;
        this.items = items;
    }

    /* 추가 */
    public void add(ReviewItem data, int position) {
        position = position == -1 ? getItemCount()  : position;
        // 리뷰 추가
        this.items.add(position, data);
        // 추가된 리뷰를 리스트에 적용하기 위함
        notifyItemInserted(position);
    }

    /* 삭제 */
    public ReviewItem remove(int position){
        ReviewItem data = null;

        if (position < getItemCount()) {
            data = this.items.get(position);
            // 리뷰 삭제
            this.items.remove(position);
            // 삭제된 리뷰를 리스트에 적용하기 위함
            notifyItemRemoved(position);
        }

        return data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, null);

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // ViewHolder 생성
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtContents.setText(this.items.get(position).review.getContents());              // 리뷰
        holder.txtPoint.setText(String.valueOf(this.items.get(position).review.getPoint()));    // 별점

        // 등록일시
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.items.get(position).review.getInputTimeMillis());
        holder.txtDateTime.setText(DateFormat.format("yyyy-MM-dd HH:mm:ss", calendar).toString());

        // 나의 리뷰면
        if (this.items.get(position).review.getUserDocId().equals(GlobalVariable.userDocumentId)) {
            // 수정 가능
            holder.imgEdit.setVisibility(View.VISIBLE);
        } else {
            holder.imgEdit.setVisibility(View.GONE);
        }

        // 작성자 이름 표시
        displayMaster(this.items.get(position).review.getUserDocId(), holder.txtWriter);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    /* 작성자 이름 표시 */
    private void displayMaster(String id, final TextView txtWriter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER).document(id);
        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 성공
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    if (document.getData() != null) {
                        txtWriter.setText(document.getData().get("name").toString());
                    }
                }
            } else {
                txtWriter.setText("오류");
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView txtWriter, txtContents, txtPoint, txtDateTime;
        ImageView imgEdit;

        ViewHolder(View view) {
            super(view);

            this.txtWriter = view.findViewById(R.id.txtWriter);
            this.txtContents = view.findViewById(R.id.txtContents);
            this.txtPoint = view.findViewById(R.id.txtPoint);
            this.txtDateTime = view.findViewById(R.id.txtDateTime);
            this.imgEdit = view.findViewById(R.id.imgEdit);

            this.imgEdit.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            if (!items.get(position).review.getUserDocId().equals(GlobalVariable.userDocumentId)) {
                return;
            }

            Bundle bundle = new Bundle();

            if (v.getId() == R.id.imgEdit) {
                // 편집하기
                bundle.putInt("position", position);
                listener.onButtonClick(bundle, v.getId());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            // 롱클릭시 삭제 처리 하기
            int position = getAdapterPosition();

            if (!items.get(position).review.getUserDocId().equals(GlobalVariable.userDocumentId)) {
                return true;
            }

            Bundle bundle = new Bundle();

            bundle.putInt("position", position);
            listener.onItemClick(bundle, view.getId());

            // 다른데서는 처리할 필요없음 true
            return true;
        }
    }
}
