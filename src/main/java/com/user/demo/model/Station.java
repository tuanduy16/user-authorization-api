package com.user.demo.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stations")
@Data
public class Station {
    @Id
    private String code; // Station code (primary key)
} 