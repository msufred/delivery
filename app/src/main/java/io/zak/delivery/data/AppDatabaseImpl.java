package io.zak.delivery.data;

import android.content.Context;

import androidx.room.Room;

public class AppDatabaseImpl {

    private static AppDatabase database;

    public static AppDatabase getInstance(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context, AppDatabase.class, "zakdeliverydb").build();
        }
        return database;
    }
}
