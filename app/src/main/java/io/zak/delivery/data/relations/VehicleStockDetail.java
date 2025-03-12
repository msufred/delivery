package io.zak.delivery.data.relations;

import androidx.room.Embedded;

import io.zak.delivery.data.entities.Product;
import io.zak.delivery.data.entities.VehicleStock;

public class VehicleStockDetail {

    @Embedded
    public VehicleStock vehicleStock;

    @Embedded
    public Product product;
}
