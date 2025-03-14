package io.zak.delivery.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.zak.delivery.data.entities.Category;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Category category);

    @Query("DELETE FROM categories")
    int deleteAll();

    @Query("SELECT * FROM categories")
    List<Category> getAll();
}
