package com.thunder11.scuad.jobposting.dto.request;

import lombok.Data;

@Data
public class JobPostingSearchCondition {

    private Long cursor;
    private Integer size = 20;
    private String keyword;
    private String status = "OPEN";
    private String sort = "DEADLINE_ASC";
}
