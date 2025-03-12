package io.zak.delivery.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.delivery.data.entities.Consumer;

@Dao
public interface ConsumerDao {

    @Insert
    long insert(Consumer consumer);

    @Update
    int update(Consumer consumer);

    @Delete
    int delete(Consumer consumer);

    @Query("SELECT * FROM consumers")
    List<Consumer> getAll();

}
