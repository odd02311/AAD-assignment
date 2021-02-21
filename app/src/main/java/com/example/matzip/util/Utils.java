package com.example.matzip.util;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import com.example.matzip.entity.Point;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class Utils {

    /* 거리 */
    public static String getDistanceStr(double distance) {
        String distanceStr;

        // 1km 이상이면
        if (distance > 1000) {
            distance = distance / 1000;
            // 소수점 한자리까지 표시 (반올림)
            distanceStr = (Math.round(distance*10) / 10.0) + "Km";
        } else {
            // 소수점 버림
            distanceStr = (int) Math.floor(distance) + "m";
        }

        return distanceStr;
    }

    /* 거리 계산 */
    public static double getDistance(double lat1 , double lng1 , double lat2 , double lng2 ){
        double distance;

        Location locationA = new Location("point A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lng1);

        Location locationB = new Location("point B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lng2);

        distance = locationA.distanceTo(locationB);

        return distance;
    }

    /* GPS 정보로 주소 얻기 */
    public static String getAddressFromGps(Context context, double lat, double lng) {
        // 지오코더... GPS 를 주소로 변환
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        //Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
        } catch (IllegalArgumentException illegalArgumentException) {
            return "Incorrect GPS coordinates";
        } catch (IOException ioException) {
            //네트워크 문제
            return "Network error";
        }

        if (addresses == null || addresses.size() == 0) {
            return "There is no address information.";
        } else {
            Address address = addresses.get(0);
            String addr = address.getAddressLine(0);
            addr = addr.replace("대한민국", "").trim();
            return addr;
        }
    }

    /* 주소로 GPS (위도,경도) 얻기 */
    public static Point getGpsFromAddress(Context context, String address) {
        // 지오코더... 주소 를 GPS 로 변환
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocationName(address, 1);
        } catch (IllegalArgumentException illegalArgumentException) {
            return new Point(0 ,0);
        } catch (IOException ioException) {
            //네트워크 문제
            return new Point(0 ,0);
        }

        if (addresses == null || addresses.size() == 0) {
            return new Point(0 ,0);
        } else {
            // 위도 경도 넘김
            return new Point(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
        }
    }

    /* 파일명 얻기 */
    public static String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }

    /* 파일 확장자명 얻기 */
    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /* 이미지 파일 사이즈 조절 */
    public static Bitmap resizeImage(String filePath, int size) {
        Bitmap bmp;

        // Bitmap 정보를 가져온다.
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bmOptions);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = size;

        bmp = BitmapFactory.decodeFile(filePath, bmOptions);

        return bmp;
    }

    /* Bitmap => InputStream 변환 */
    public static InputStream bitmapToInputStream(Bitmap bitmap, String fileExtension) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (fileExtension.equals("png")) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        }

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /* 회전 각도 구하기  */
    public static int getExifOrientation(String filePath) {
        ExifInterface exif;

        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            return 0;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);

        int degree = 0;
        if (orientation != -1) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        }

        return degree;
    }

    /* 이미지 회전하기 */
    public static Bitmap getRotatedBitmap(Bitmap bitmap, int degree) {
        if (degree != 0 && bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

            try {
                Bitmap tmpBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                if (bitmap != tmpBitmap) {
                    bitmap.recycle();
                    bitmap = tmpBitmap;
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    /* GoogleSignInOptions 얻기 */
    public static GoogleSignInOptions getGoogleSignInOptions(String tokenId) {
        // Google 로그인을 앱에 통합
        // GoogleSignInOptions 개체를 구성할 때 requestIdToken 을 호출
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(tokenId)
                .requestEmail()
                .build();

        return googleSignInOptions;
    }

    /* 갤러리(사진) 연동 */
    public static void goGallery(Activity activity, int requestCode) {
        // ACTION_PICK (MediaStore.Images.Media.CONTENT_TYPE) 사진을 고르겠다
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        activity.startActivityForResult(intent, requestCode);
    }

    /* uri 로부터 실제경로 얻기 */
    public static String getRealPathFromURI(final Context context, final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator
                            + split[1];
                } else {
                    String sdCardPath = getRemovableSDCardPath(context).split("/Android")[0];
                    return sdCardPath + File.separator + split[1];
                }
            }

            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:", "");
                } else {
                    long fileId = getFileId(uri);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            InputStream inputStream = context.getContentResolver().openInputStream(uri);
                            File file = new File(context.getCacheDir().getAbsolutePath() + "/" + fileId);
                            writeFile(inputStream, file);
                            return file.getAbsolutePath();
                        } catch (FileNotFoundException e) {
                            return null;
                        }
                    } else {
                        String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads",
                                "content://downloads/all_downloads"
                        };

                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), fileId);
                                String path = getDataColumn(context, contentUri, null, null);
                                if (path != null) {
                                    return path;
                                }
                            } catch (Exception e) {
                                return null;
                            }
                        }
                    }
                }
            }

            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static String getRemovableSDCardPath(Context context) {
        File[] storage = ContextCompat.getExternalFilesDirs(context, null);
        if (storage.length > 1 && storage[0] != null && storage[1] != null)
            return storage[1].toString();
        else
            return "";
    }

    private static String getDataColumn(Context context, Uri uri,
                                        String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }


    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }


    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }


    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }

    /* ID 가 Long 형태가 아닌 경우가 있어서 처리한다. */
    private static Long getFileId(Uri uri) {
        long strReulst = 0;
        try {
            String path = uri.getPath();
            String[] paths = path.split("/");
            if (paths.length >= 3) {
                strReulst = Long.parseLong(paths[2]);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            strReulst = Long.parseLong(new File(uri.getPath()).getName());
        }
        return strReulst;
    }

    /* 캐시 폴더에 저장 */
    private static void writeFile(InputStream in, File file) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
