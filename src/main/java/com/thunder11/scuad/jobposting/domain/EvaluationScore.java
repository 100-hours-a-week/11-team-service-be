package com.thunder11.scuad.jobposting.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScore {
    private String name;
    private Integer score;
    private String description;
}