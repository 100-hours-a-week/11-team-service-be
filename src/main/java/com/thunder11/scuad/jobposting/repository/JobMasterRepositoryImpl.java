package com.thunder11.scuad.jobposting.repository;

import static com.thunder11.scuad.jobposting.domain.QCompany.company;
import static com.thunder11.scuad.jobposting.domain.QJobMaster.jobMaster;
import static com.thunder11.scuad.jobposting.domain.QJobMasterSkill.jobMasterSkill;
import static com.thunder11.scuad.jobposting.domain.QSkill.skill;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;
import com.thunder11.scuad.jobposting.dto.request.JobPostingSearchCondition;
import com.thunder11.scuad.common.util.CursorTokenUtil;

@RequiredArgsConstructor
public class JobMasterRepositoryImpl implements JobMasterRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<JobMaster> searchJobPostings(JobPostingSearchCondition condition, CursorTokenUtil.CursorData cursorData) {

        List<Long> ids = queryFactory
                .select(jobMaster.id)
                .from(jobMaster)
                .join(jobMaster.company, company)
                .where(
                        cursorCondition(cursorData, condition.getSort()),
                        eqStatus(condition.getStatus()),
                        containsKeyword(condition.getKeyword()))
                .orderBy(getOrderSpecifier(condition.getSort()))
                .limit(condition.getSize())
                .fetch();

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<JobMaster> results = queryFactory
                .selectFrom(jobMaster)
                .join(jobMaster.company, company).fetchJoin()
                .leftJoin(jobMaster.jobMasterSkills, jobMasterSkill).fetchJoin()
                .leftJoin(jobMasterSkill.skill, skill).fetchJoin()
                .where(jobMaster.id.in(ids))
                .fetch();

        Map<Long, JobMaster> resultMap = results.stream()
                .collect(Collectors.toMap(JobMaster::getId, Function.identity()));

        return ids.stream()
                .map(resultMap::get)
                .collect(Collectors.toList());
    }

    private BooleanExpression eqStatus(String status) {
        if ("CLOSED".equalsIgnoreCase(status))
            return jobMaster.status.eq(JobStatus.CLOSED);
        if ("OPEN".equalsIgnoreCase(status))
            return jobMaster.status.eq(JobStatus.OPEN);
        return null;
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword))
            return null;
        return jobMaster.jobTitle.contains(keyword)
                .or(company.name.contains(keyword));
    }

    private OrderSpecifier[] getOrderSpecifier(String sort) {
        if ("DEADLINE_ASC".equalsIgnoreCase(sort)) {
            return new OrderSpecifier[] {
                    getStatusRank().asc(),
                    jobMaster.endDate.asc(),
                    jobMaster.id.desc()
            };
        }
        return new OrderSpecifier[] { jobMaster.id.desc() };
    }

    private NumberExpression<Integer> getStatusRank() {
        return new CaseBuilder()
                .when(jobMaster.status.eq(JobStatus.OPEN)).then(1)
                .otherwise(2);
    }

    private BooleanExpression cursorCondition(CursorTokenUtil.CursorData cursorData, String sort) {
        if (cursorData == null)
            return null;

        Long cursorId = cursorData.cursorId;
        LocalDate cursorEndDate = cursorData.cursorEndDate;
        JobStatus cursorStatus = cursorData.cursorStatus;

        if ("DEADLINE_ASC".equalsIgnoreCase(sort)) {
            Integer cursorRank = (cursorStatus == JobStatus.OPEN) ? 1 : 2;
            NumberExpression<Integer> statusRank = getStatusRank();

            return statusRank.gt(cursorRank)
                    .or(statusRank.eq(cursorRank).and(jobMaster.endDate.gt(cursorEndDate)))
                    .or(statusRank.eq(cursorRank).and(jobMaster.endDate.eq(cursorEndDate))
                            .and(jobMaster.id.lt(cursorId)));
        }

        return jobMaster.id.lt(cursorData.cursorId);
    }
}