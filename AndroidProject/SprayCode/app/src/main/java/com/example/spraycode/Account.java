package com.example.spraycode;

public class Account {
    private String AccountId;
    private String AccountType;
    private String AccountSubType;
    private String AccountNum;
    private String Name;
    private Boolean isDefault;

    // Getters and setters
    public String getAccountId() { return AccountId; }
    public void setAccountId(String accountId) { AccountId = accountId; }

    public String getAccountType() { return AccountType; }
    public void setAccountType(String accountType) { AccountType = accountType; }

    public String getAccountSubType() { return AccountSubType; }
    public void setAccountSubType(String accountSubType) { AccountSubType = accountSubType; }

    public String getAccountNum() { return AccountNum; }
    public void setAccountNum(String accountNum) { AccountNum = accountNum; }

    public String getName() { return Name; }
    public void setName(String name) { Name = name; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean is_default) { isDefault = is_default; }
}