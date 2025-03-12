package io.zak.delivery.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.delivery.data.entities.Order;
import io.zak.delivery.data.relations.OrderDetail;

@Dao
public interface OrderDao {

    @Insert
    long insert(Order order);

    @Update
    int update(Order order);

    @Delete
    int delete(Order order);

    @Query("SELECT orders.*, consumers.* " +
            "FROM orders " +
            "INNER JOIN consumers " +
            "ON consumers.consumerId = orders.fkConsumerId")
    List<OrderDetail> getOrdersWithDetail();
}
