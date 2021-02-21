package com.example.matzip.listener;

import android.os.Bundle;

public interface IAdapterOnClickListener {

    /* 아이템 클릭 */
    void onItemClick(Bundle bundle, int id);

    /* 버튼 (등록 / 수정 / 삭제 / 기타 ...) 클릭 */
    void onButtonClick(Bundle bundle, int id);

}
