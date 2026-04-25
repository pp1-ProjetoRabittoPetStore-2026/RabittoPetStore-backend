package com.rabitto.backend.models;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class OrderItem {

    private String productName;
    private Integer quantity;
    private Double price;
}
