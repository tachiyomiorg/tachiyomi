package eu.kanade.tachiyomi.io;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;

public class IOHandler {
    @TargetApi(Build.VERSION_CODES.KITKAT)
    /**
     * Get full filepath of build in Android File picker.
     * TODO this is kinda a workaround and stolen from https://stackoverflow.com/questions/26965016/get-file-path-from-uri
     * Need to look at this and improve this in the future.
     */
    public static String getFilePath(Context context, Uri uri) {
        int currentApiVersion;
        try {
            currentApiVersion = android.os.Build.VERSION.SDK_INT;
        } catch (NumberFormatException e) {
            //API 3 will crash if SDK_INT is called
            currentApiVersion = 3;
        }
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            String filePath = "";
            String wholeID = DocumentsContract.getDocumentId(uri);

            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];

            String[] column = {MediaStore.Images.Media.DATA};

            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, new String[]{id}, null);

            int columnIndex = cursor != null ? cursor.getColumnIndex(column[0]) : 0;

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
            return filePath;
        } else if (currentApiVersion <= Build.VERSION_CODES.HONEYCOMB_MR2 && currentApiVersion >= Build.VERSION_CODES.HONEYCOMB)

        {
            String[] proj = {MediaStore.Images.Media.DATA};
            String result = null;

            CursorLoader cursorLoader = new CursorLoader(
                    context,
                    uri, proj, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();

            if (cursor != null) {
                int column_index =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                result = cursor.getString(column_index);
            }
            return result;
        } else {

            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index
                    = cursor != null ? cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) : 0;
            cursor.moveToFirst();
            String result = cursor.getString(column_index);
            cursor.close();
            return result;
        }
    }

}
