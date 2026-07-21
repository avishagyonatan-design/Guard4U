package com.project.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Site {
    private Integer customerID;
    private Integer siteID;
    private LocalDate startDate;
    private String location;

    // Latest subscription data (joined from tblServiceCharge + tblServiceType)
    private Integer serialNum;
    private LocalDate serviceStartDate;
    private String serviceDescription;
    private Double discount;
    private Integer valid;
}
