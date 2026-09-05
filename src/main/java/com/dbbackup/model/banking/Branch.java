package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 20)
    private String branchCode;

    private String address;
    private String city;

    @Column(unique = true, nullable = false, length = 20)
    private String ifscCode;

    private String managerName;
    private String contactPhone;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    private LocalDateTime createdAt = LocalDateTime.now();

    public Branch() {}

    public Branch(String name, String branchCode, String address, String city, String ifscCode, String managerName, String contactPhone) {
        this.name = name;
        this.branchCode = branchCode;
        this.address = address;
        this.city = city;
        this.ifscCode = ifscCode;
        this.managerName = managerName;
        this.contactPhone = contactPhone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
