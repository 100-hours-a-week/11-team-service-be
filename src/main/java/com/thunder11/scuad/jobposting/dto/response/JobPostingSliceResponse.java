package com.thunder11.scuad.jobposting.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPostingSliceResponse {
    private List<JobPostingListResponse> items;
    @JsonProperty("next_cursor")
    private String nextCursor;
    private boolean last;

    public static JobPostingSliceResponse of(List<JobPostingListResponse> jobPostingList, String nextCursor, boolean last) {
        return JobPostingSliceResponse.builder()
                .items(jobPostingList)
                .nextCursor(nextCursor)
                .last(last)
                .build();
    }
}
