package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders")
public class Order {

    @PrimaryKey(autoGenerate = true)
    public int orderId;
    public String orNo;             // official receipt no; REQUIRED
    public int fkVehicleId;         // from assigned vehicle
    public int fkEmployeeId;        // by assigned employee/driver
    public int fkConsumerId;
    public long dateOrdered;        // Date converted to long value (use getTime() of Date)
    public double totalAmount;      // total amount of the order
    public String orderStatus;      // i.e. Processing, Completed
}
