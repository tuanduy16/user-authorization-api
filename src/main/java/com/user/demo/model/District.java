package com.user.demo.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "districts")
@Data
public class District {
    @Id
    private String code; // District code (primary key)
    private String name; // District name
    private String type; // District type (huyện, quận, thành phố)
    
    @ManyToOne
    @JoinColumn(name = "province_code")
    private Province province; // Province reference
} 