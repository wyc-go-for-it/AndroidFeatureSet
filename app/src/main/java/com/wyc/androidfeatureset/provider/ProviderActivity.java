package com.wyc.androidfeatureset.provider;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.wyc.androidfeatureset.R;
import com.wyc.androidfeatureset.camera.CaptureActivity;
import com.wyc.logger.Logger;

public class ProviderActivity extends AppCompatActivity {
    private final static String TAG = "ProviderActivity";
    private final static String AUTHORITY = "com.android.wyc.provider";
    private final static Uri STUDENT_URI = Uri.parse("content://" + AUTHORITY + "/student");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider);
        Logger.d(getCacheDir( ).getPath());
        Logger.d(getDatabasePath(Environment.DIRECTORY_PICTURES).getPath());
        Logger.d(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());
    }
    @Override
    protected void onResume() {
        super.onResume();

        insertValue();
        queryValue();

        Uri uri = Uri.parse("content://com.android.wyc.provider/student");
        Uri withAppendedIdUri = ContentUris.withAppendedId(uri, 1);
        Log.d(TAG," withAppendedId ~ uri = " + withAppendedIdUri.toString());
        long parseId = ContentUris.parseId(withAppendedIdUri);
        Log.d(TAG," parseId ~ uri = " + parseId);

        Uri.Builder ub = new Uri.Builder();
        ub.authority("com.android.wyc.provider")
                .appendPath("student");
        Log.d(TAG,"ub = " + ub.toString());
        Uri.Builder appendIdUri = ContentUris.appendId(ub,1);
        Log.d(TAG,"appendIdUri = " + appendIdUri.toString());

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void insertValue() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("id",2);
        contentValues.put("name","wyc");
        contentValues.put("gender",1);
        contentValues.put("number","201804091601");
        contentValues.put("score","80");

        getContentResolver().insert(STUDENT_URI,contentValues);
    }

    private void queryValue() {
        Cursor cursor = getContentResolver().query(STUDENT_URI, new String[]{"id", "name","gender","number","score"},null,null,null);
        try{
            while (cursor != null && cursor.moveToNext()) {
                Student student = new Student();
                student.id = cursor.getInt(cursor.getColumnIndex("id"));
                student.name = cursor.getString(cursor.getColumnIndex("name"));
                student.gender = cursor.getInt(cursor.getColumnIndex("gender"));
                student.number = cursor.getString(cursor.getColumnIndex("number"));
                student.score = cursor.getInt(cursor.getColumnIndex("score"));
                Log.d(TAG,"student = " + student.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    public static void start(Context context){
        final Intent intent = new Intent();
        intent.setClass(context, ProviderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    static private class Student {
        private final static String TAG = "Student";

        public Integer id;
        public String name;
        public Integer gender;
        public String number;
        public Integer score;

        @Override
        public String toString() {
            return "Student{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", gender=" + gender +
                    ", number='" + number + '\'' +
                    ", score=" + score +
                    '}';
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getGender() {
            return gender;
        }

        public void setGender(Integer gender) {
            this.gender = gender;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }
}