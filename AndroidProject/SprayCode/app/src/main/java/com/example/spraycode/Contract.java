package com.example.spraycode;

public class Contract {
    private String contract_name;
    private String contractor_name;
    private String costing;
    private String expiry;
    private String status;
    private String role;

    public Contract(String contract_name, String contractor_name, String costing, String expiry, String status, String role) {
        this.contract_name = contract_name;
        this.contractor_name = contractor_name;
        this.costing = costing;
        this.expiry = expiry;
        this.status = status;
        this.role = role;
    }

    public String getContractName() {
        return contract_name;
    }

    public String getContractorName() {
        return contractor_name;
    }


    public String getCosting() {
        return costing;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }
}