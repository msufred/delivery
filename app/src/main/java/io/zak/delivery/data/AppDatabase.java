package io.zak.delivery.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.zak.delivery.data.dao.ConsumerDao;
import io.zak.delivery.data.dao.OrderDao;
import io.zak.delivery.data.dao.ProductDao;
import io.zak.delivery.data.dao.UserDao;
import io.zak.delivery.data.dao.VehicleStockDao;
import io.zak.delivery.data.entities.Consumer;
import io.zak.delivery.data.entities.Order;
import io.zak.delivery.data.entities.Product;
import io.zak.delivery.data.entities.User;
import io.zak.delivery.data.entities.VehicleStock;

@Database(entities = {
        User.class,
        Product.class,
        VehicleStock.class,
        Consumer.class,
        Order.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao users();
    public abstract ProductDao products();
    public abstract VehicleStockDao vehicleStocks();
    public abstract ConsumerDao consumers();
    public abstract OrderDao orders();
}
