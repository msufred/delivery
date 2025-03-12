package io.zak.delivery.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.delivery.data.entities.VehicleStock;
import io.zak.delivery.data.relations.VehicleStockDetail;

@Dao
public interface VehicleStockDao {

    @Insert
    long insert(VehicleStock vehicleStock);

    @Update
    int update(VehicleStock vehicleStock);

    @Delete
    int delete(VehicleStock vehicleStock);

    @Query("SELECT vehicle_stocks.*, products.* " +
            "FROM vehicle_stocks " +
            "INNER JOIN products " +
            "ON vehicle_stocks.fkProductId = products.productId " +
            "WHERE vehicle_stocks.fkVehicleId=:vehicleId")
    List<VehicleStockDetail> getVehicleStocks(int vehicleId);
}
