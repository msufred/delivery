package io.zak.delivery.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.zak.delivery.data.dao.ProductDao;
import io.zak.delivery.data.dao.UserDao;
import io.zak.delivery.data.entities.Product;
import io.zak.delivery.data.entities.User;

@Database(entities = {
        User.class,
        Product.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao users();

    public abstract ProductDao products();
}
