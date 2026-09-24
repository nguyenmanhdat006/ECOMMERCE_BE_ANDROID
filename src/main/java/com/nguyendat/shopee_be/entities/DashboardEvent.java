package com.nguyendat.shopee_be.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEvent<T> {
    private String type;      // ORDER_CREATED, KPI_UPDATE, etc.
    private T payload;        // dữ liệu kèm theo
    private String timestamp;
}