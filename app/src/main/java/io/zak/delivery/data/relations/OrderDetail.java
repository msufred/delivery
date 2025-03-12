package io.zak.delivery.data.relations;

import androidx.room.Embedded;

import io.zak.delivery.data.entities.Consumer;
import io.zak.delivery.data.entities.Order;

public class OrderDetail {
    @Embedded
    public Order order;

    @Embedded
    public Consumer consumer;
}
