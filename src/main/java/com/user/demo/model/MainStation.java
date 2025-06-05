package com.user.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "main_stations")
@Data
public class MainStation {
    @Id
    private String code; // Main station code (primary key)
    private String name; // Main station name
} 