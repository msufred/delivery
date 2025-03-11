package io.zak.delivery.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * This table depends on the Inventory app. Foreign key constraints will not be handled by this
 * app. This entry will only hold the ID of the brand, category, and supplier as reference for the
 * Inventory app.
 */
@Entity(tableName = "products")
public class Product {
    @PrimaryKey(autoGenerate = true)
    public int productId;
    public String productName;
    public int fkBrandId;       // no constraint
    public int fkCategoryId;    // no constraint
    public int fkSupplierId;    // no constraint
    public int criticalLevel;   // indicates when stock is in critical level
    public double price;
    public String productDescription;
}
