package com.user.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "provinces")
@Data
public class Province {
    @Id
    private String code; // Province code (primary key)
    private String name; // Province name
    private String type; // Province type (tỉnh, thành phố)
    private String areaCode; // Area code (foreign key)
} 