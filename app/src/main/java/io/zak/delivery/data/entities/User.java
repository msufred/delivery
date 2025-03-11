package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * This table holds the data of the delivery drivers (aka User).
 */
@Entity(tableName = "users") // aka Delivery User
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String username;
    public String password;
    public String fullName;
    public String position;
    public String license;
    public String address;
    public String contactNo;
    public String email;
}
