package io.zak.delivery.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.delivery.data.entities.VehicleStock;

@Dao
public interface VehicleStockDao {

    @Insert
    long insert(VehicleStock vehicleStock);

    @Update
    int update(VehicleStock vehicleStock);

    @Delete
    int delete(VehicleStock vehicleStock);

    @Query("SELECT * FROM vehicle_stocks WHERE fkVehicleId=:vehicleId")
    List<VehicleStock> getVehicleStocks(int vehicleId);
}
