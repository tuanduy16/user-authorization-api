package com.user.demo.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "nations")
@Data
public class Nation {
    @Id
    private String code; // Nation code (primary key)
    private String name; // Nation name
} 