package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * This table depends on the Inventory app. The ID will not be autogenerated, it will be manually
 * set or the entry can be fetched from online database (TBD).
 */
@Entity(tableName = "vehicles")
public class Vehicle {
    @PrimaryKey
    public int id;
    public String name;
    public String plateNo;
    public String status;
}
