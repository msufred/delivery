package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Inventory app can only create, edit, and delete Category entries. Synchronize with Firebase to fetch
 * updated list of entries.
 */
@Entity(tableName = "categories")
public class Category {

    @PrimaryKey
    public int categoryId;
    public String categoryName;
}
