package com.example.matzip.popupwindow;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.matzip.R;
import com.example.matzip.listener.IClickListener;

public class ReviewPopup extends PopupWindow {

    private IClickListener listener;

    private EditText editContents;
    private TextView txtMessage;

    private int point;

    private int mode;

    public ReviewPopup(View view, int point, String contents) {
        super(view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        if (point == 0) {

            this.mode = 0;

            this.point = 5;
        } else {

            this.mode = 1;
            this.point = point;
        }

        switch (this.point) {
            case 1:
                ((RadioButton) view.findViewById(R.id.rd1)).setChecked(true);
                break;
            case 2:
                ((RadioButton) view.findViewById(R.id.rd2)).setChecked(true);
                break;
            case 3:
                ((RadioButton) view.findViewById(R.id.rd3)).setChecked(true);
                break;
            case 4:
                ((RadioButton) view.findViewById(R.id.rd4)).setChecked(true);
                break;
            case 5:
                ((RadioButton) view.findViewById(R.id.rd5)).setChecked(true);
                break;
        }
        ((RadioGroup) view.findViewById(R.id.rdgPoint)).setOnCheckedChangeListener(mCheckedChangeListener);

        this.editContents = view.findViewById(R.id.editContents);
        this.editContents.setHint(R.string.hint_review);
        this.editContents.setText(contents);


        this.txtMessage = view.findViewById(R.id.txtMessage);
        this.txtMessage.setText("");

        view.findViewById(R.id.btnOk).setOnClickListener(mClickListener);
        view.findViewById(R.id.btnCancel).setOnClickListener(mClickListener);
    }


    public void setClickListener(IClickListener listener) {
        this.listener = listener;
    }


    private boolean checkData() {

        String contents = this.editContents.getText().toString();
        if (TextUtils.isEmpty(contents)) {
            this.txtMessage.setText(R.string.msg_review_check_empty);
            this.editContents.requestFocus();
            return false;
        }

        this.txtMessage.setText("");

        return true;
    }


    private RadioGroup.OnCheckedChangeListener mCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
            switch (checkedId) {
                case R.id.rd1:
                    point = 1;
                    break;
                case R.id.rd2:
                    point = 2;
                    break;
                case R.id.rd3:
                    point = 3;
                    break;
                case R.id.rd4:
                    point = 4;
                    break;
                case R.id.rd5:
                    point = 5;
                    break;
            }
        }
    };


    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btnOk:

                    if (!checkData()) {
                        return;
                    }

                    if (listener != null) {

                        Bundle bundle = new Bundle();
                        bundle.putInt("point", point);
                        bundle.putString("contents", editContents.getText().toString());
                        bundle.putInt("mode", mode);

                        listener.onClick(bundle, view.getId());
                    }

                    dismiss();
                    break;
                case R.id.btnCancel:

                    dismiss();
                    break;
            }
        }
    };
}
