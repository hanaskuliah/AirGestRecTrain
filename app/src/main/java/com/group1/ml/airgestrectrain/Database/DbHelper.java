package com.group1.ml.airgestrectrain.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Hanas Subakti on 1/4/2016.
 */
public class DbHelper extends SQLiteOpenHelper {

    static String DATABASE_NAME="userdata";
    public static final String TABLE_NAME="user_signature";
    public static final String KEY_NAME="key_name";
    public static final String KEY_USERNAME="key_username";
    public static final String KEY_PASSWORD="key_password";
    public static final String KEY_ID="id";
    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE="CREATE TABLE "+TABLE_NAME+" ("+KEY_ID+" INTEGER PRIMARY KEY, "+KEY_NAME+" TEXT, "+KEY_USERNAME+" TEXT,"+KEY_PASSWORD+" TEXT)";
        db.execSQL(CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);

    }

}
