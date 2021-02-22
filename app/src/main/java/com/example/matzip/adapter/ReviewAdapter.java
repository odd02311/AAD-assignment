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


    public void add(ReviewItem data, int position) {
        position = position == -1 ? getItemCount()  : position;
        // Writing review
        this.items.add(position, data);

        notifyItemInserted(position);
    }


    public ReviewItem remove(int position){
        ReviewItem data = null;

        if (position < getItemCount()) {
            data = this.items.get(position);
            // Delete Review
            this.items.remove(position);

            notifyItemRemoved(position);
        }

        return data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, null);


        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // Create ViewHolder
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtContents.setText(this.items.get(position).review.getContents());
        holder.txtPoint.setText(String.valueOf(this.items.get(position).review.getPoint()));


        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.items.get(position).review.getInputTimeMillis());
        holder.txtDateTime.setText(DateFormat.format("yyyy-MM-dd HH:mm:ss", calendar).toString());

        // My review
        if (this.items.get(position).review.getUserDocId().equals(GlobalVariable.userDocumentId)) {
            // Fixing Review
            holder.imgEdit.setVisibility(View.VISIBLE);
        } else {
            holder.imgEdit.setVisibility(View.GONE);
        }

        // Display reviewer name
        displayMaster(this.items.get(position).review.getUserDocId(), holder.txtWriter);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }


    private void displayMaster(String id, final TextView txtWriter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER).document(id);
        reference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Success
                DocumentSnapshot document = task.getResult();
                if (document != null) {
                    if (document.getData() != null) {
                        txtWriter.setText(document.getData().get("name").toString());
                    }
                }
            } else {
                txtWriter.setText("Error");
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
                // Edit
                bundle.putInt("position", position);
                listener.onButtonClick(bundle, v.getId());
            }
        }

        @Override
        public boolean onLongClick(View view) {

            int position = getAdapterPosition();

            if (!items.get(position).review.getUserDocId().equals(GlobalVariable.userDocumentId)) {
                return true;
            }

            Bundle bundle = new Bundle();

            bundle.putInt("position", position);
            listener.onItemClick(bundle, view.getId());


            return true;
        }
    }
}
