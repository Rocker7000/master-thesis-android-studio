package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class NumbersDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mydb.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "numbers";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NUMBER = "number";

    public NumbersDatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NUMBER + " INTEGER)";
        db.execSQL(createTable);

        for (int i = 0; i < 10; i++) {
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COLUMN_NUMBER  + ") VALUES (" + i + ")");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public List<Integer> getNumbers(){

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_NUMBER}, null ,null ,null,null, COLUMN_ID + " ASC", "10");

        List<Integer> numbers = new ArrayList<>();


        if(cursor.moveToFirst())
        {
            do{
                int number = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMBER));
                numbers.add(number);
            }while(cursor.moveToNext());
        }

        cursor.close();
        return  numbers;
    }
}
