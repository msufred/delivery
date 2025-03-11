package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_items", foreignKeys = {
        @ForeignKey(entity = Order.class, parentColumns = "orderId", childColumns = "fkOrderId", onDelete = ForeignKey.CASCADE)
})
public class OrderItem {
    @PrimaryKey(autoGenerate = true)
    public int orderItemId;
    public int fkOrderId;           // the ID of the Order this item belongs to
    public int fkWarehouseStockId;  // WarehouseStock ID of the product
    public int fkProductId;         // Product ID; assumes Delivery App and Inventory App shares the same database
    public double sellingPrice;
    public int quantity;
    public double subtotal;
}
