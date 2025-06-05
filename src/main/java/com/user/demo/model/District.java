package com.user.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "districts")
@Data
public class District {
    @Id
    private String code; // District code (primary key)
    private String name; // District name
    private String type; // District type (huyện, quận, thành phố)
    private String provinceCode; // Province code (foreign key)
} 