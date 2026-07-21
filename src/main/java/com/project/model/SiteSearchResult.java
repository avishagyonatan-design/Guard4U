package com.project.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SiteSearchResult {
    private Integer customerID;
    private String fullName;
    private Integer siteCount;
    private Integer validSerialNum;
    private LocalDateTime lastCallDate;
}
