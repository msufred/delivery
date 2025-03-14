package io.zak.delivery.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.delivery.data.entities.Brand;

@Dao
public interface BrandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Brand brand);

    @Query("DELETE FROM brands")
    int deleteAll();

    @Query("SELECT * FROM brands")
    List<Brand> getAll();
}
