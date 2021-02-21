package com.example.matzip.util;

public class Constants {

    public static final String DEFAULT_ADDRESS = "8 Shenton Way, Singapore 068811"; // 디폴트 현재 위치

    public static final String TEMP_FILE_PREFIX_NAME = "TEMP_"; // 임시파일 PREFIX 명
    public static final int IMAGE_SCALE = 4;                // 이미지 inSampleSize 값
    public static final double VIEW_IMAGE_RATE = 0.75;      // 뷰화면에서 이미지 비율

    /* SharedPreferences 관련 상수 */
    public static class SharedPreferencesName {
        public static final String USER_DOCUMENT_ID = "user_document_id";           // 일반 Fire store Document ID
        public static final String SHOP_DOCUMENT_ID = "shop_document_id";           // 업주 Fire store Document ID
        public static final String MEMBER_KIND = "member_kind";                     // 회원 종류 (일반/업주)
    }

    /* Activity 요청 코드 */
    public static class RequestCode {
        public static final int JOIN = 0;                   // 회원가입
        public static final int FAVORITE_CATEGORY = 1;      // 좋아하는 카테고리
        public static final int GOOGLE_SIGN_IN = 100;       // 구글 연동
        public static final int PICK_GALLERY = 101;         // 이미지 (갤러리...)
    }

    /* Fire store Collection 이름 */
    public static class FirestoreCollectionName {
        public static final String USER = "users";          // 사용자 (일반회원)
        public static final String SHOP = "shops";          // 가게 (업주회원)
        public static final String REVIEW = "reviews";      // 리뷰
    }

    /* Cloud Storage 폴더 이름 */
    public static class StorageFolderName {
        public static final String SHOP = "shops";          // 가게 (대표사진)
    }

    /* 회원 종류 */
    public static class MemberKind {
        public static final int NONE = 0;                   // 로그아웃 상태
        public static final int USER = 1;                   // 사용자 (일반회원)
        public static final int SHOP = 2;                   // 가게 (업주회원)
    }

    /* 로딩 딜레이 */
    public static class LoadingDelay {
        public static final int SHORT = 300;
        public static final int LONG = 1000;
    }
}
