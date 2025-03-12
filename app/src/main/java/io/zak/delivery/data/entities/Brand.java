package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Inventory app can only create, edit, and delete brand entries. Synchronize with Firebase to fetch
 * updated list of entries.
 */
@Entity(tableName = "brands")
public class Brand {

    @PrimaryKey
    public int brandId;
    public String brandName;

}
