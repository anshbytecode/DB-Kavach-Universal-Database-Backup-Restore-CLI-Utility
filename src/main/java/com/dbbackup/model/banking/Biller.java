package com.dbbackup.model.banking;

import jakarta.persistence.*;

@Entity
@Table(name = "billers")
public class Biller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String category; // ELECTRICITY, WATER, INTERNET, MOBILE, GAS, INSURANCE, EDUCATION

    @Column(unique = true, nullable = false, length = 30)
    private String billerCode;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public Biller() {}

    public Biller(String name, String category, String billerCode) {
        this.name = name;
        this.category = category;
        this.billerCode = billerCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBillerCode() { return billerCode; }
    public void setBillerCode(String billerCode) { this.billerCode = billerCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
