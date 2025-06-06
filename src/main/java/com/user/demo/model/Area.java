package com.user.demo.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "areas")
@Data
public class Area {
    @Id
    private String code; // Area code (primary key)
    private String name; // Area name
} 