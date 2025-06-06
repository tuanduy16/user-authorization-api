package com.user.demo.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "provinces")
@Data
public class Province {
    @Id
    private String code; // Province code (primary key)
    private String name; // Province name
    private String type; // Province type (tỉnh, thành phố)
    
    @ManyToOne
    @JoinColumn(name = "area_code")
    private Area area; // Area reference
} 