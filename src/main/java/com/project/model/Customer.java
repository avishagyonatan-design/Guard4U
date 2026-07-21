package com.project.model;

import lombok.Data;

@Data
public class Customer {
    private Integer customerID;
    private String firstName;
    private String lastName;
    private String address;
    private String phoneNum;
    private String email;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
