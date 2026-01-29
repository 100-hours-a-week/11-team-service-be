package com.thunder11.scuad.jobposting.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.thunder11.scuad.common.exception.ApiException;
import com.thunder11.scuad.common.exception.ErrorCode;
import com.thunder11.scuad.infra.ai.client.AiServiceClient;
import com.thunder11.scuad.infra.ai.dto.request.AiJobAnalysisRequest;
import com.thunder11.scuad.infra.ai.dto.response.AiJobAnalysisResponse;
import com.thunder11.scuad.jobposting.domain.Company;
import com.thunder11.scuad.jobposting.domain.CompanyAlias;
import com.thunder11.scuad.jobposting.domain.JobMaster;
import com.thunder11.scuad.jobposting.domain.JobMasterSkill;
import com.thunder11.scuad.jobposting.domain.JobPost;
import com.thunder11.scuad.jobposting.domain.Skill;
import com.thunder11.scuad.jobposting.domain.type.JobSourceType;
import com.thunder11.scuad.jobposting.domain.type.JobStatus;
import com.thunder11.scuad.jobposting.domain.type.RecruitmentStatus;
import com.thunder11.scuad.jobposting.domain.type.RegistrationStatus;
import com.thunder11.scuad.jobposting.dto.response.JobAnalysisResultResponse;
import com.thunder11.scuad.jobposting.repository.CompanyAliasRepository;
import com.thunder11.scuad.jobposting.repository.CompanyRepository;
import com.thunder11.scuad.jobposting.repository.JobMasterRepository;
import com.thunder11.scuad.jobposting.repository.JobPostRepository;
import com.thunder11.scuad.jobposting.repository.SkillAliasRepository;
import com.thunder11.scuad.jobposting.repository.SkillRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostingAnalysisService {

    private final AiServiceClient aiServiceClient;
    private final JobMasterRepository jobMasterRepository;
    private final JobPostRepository jobPostRepository;
    private final CompanyRepository companyRepository;
    private final CompanyAliasRepository companyAliasRepository;
    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;

    @Transactional
    public JobAnalysisResultResponse analyze(String url, Long userId) {
        log.info("Analyzing URL: {}", url);

        String normalizedUrl = normalizeUrl(url);
        String sourceUrlHash = generateHash(normalizedUrl);

        Optional<JobPost> existingJobPost = jobPostRepository.findBySourceUrlHashAndDeletedAtIsNull(sourceUrlHash);
        if (existingJobPost.isPresent()) {
            JobPost jobPost = existingJobPost.get();
            JobMaster jobMaster = jobPost.getJobMaster();

            log.info("URL 중복 발견 기존 공고 반환: ID{}", jobPost.getId());

            return JobAnalysisResultResponse.builder()
                    .jobMasterId(jobMaster.getId())
                    .jobPostingId(jobPost.getId())
                    .isExisting(true)
                    .companyName(jobMaster.getCompany().getName())
                    .jobTitle(jobPost.getRawJobTitle())
                    .mainTasks(jobMaster.getMainTasks())
                    .skills(jobMaster.getJobMasterSkills().stream()
                            .map(jms -> jms.getSkill().getName())
                            .toList())
                    .aiSummary(jobMaster.getAiSummary())
                    .startDate(jobMaster.getStartDate())
                    .status(jobMaster.getStatus().name())
                    .build();
        }

        AiJobAnalysisRequest aiRequest = AiJobAnalysisRequest.builder()
                .url(url)
                .build();

        AiJobAnalysisResponse aiData = aiServiceClient.analyzeJob(aiRequest);
        log.info("AI 분석 데이터: {}", aiData);
        if (aiData.isExisting() && aiData.getJobPostingId() != null) {
            JobPost existingPost = jobPostRepository.findByIdAndDeletedAtIsNull(aiData.getJobPostingId())
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "AI가 식별한 기존 공고를 찾을 수 없습니다."));

            JobMaster jobMaster = existingPost.getJobMaster();

            return JobAnalysisResultResponse.builder()
                    .jobMasterId(jobMaster.getId())
                    .jobPostingId(existingPost.getId())
                    .isExisting(true) // 프론트엔드가 이 값을 보고 "이미 등록된 공고" 팝업을 띄움
                    .companyName(jobMaster.getCompany().getName())
                    .jobTitle(existingPost.getRawJobTitle())
                    .mainTasks(jobMaster.getMainTasks())
                    .skills(jobMaster.getJobMasterSkills().stream()
                            .map(jms -> jms.getSkill().getName())
                            .toList())
                    .aiSummary(jobMaster.getAiSummary())
                    .startDate(jobMaster.getStartDate())
                    .status(jobMaster.getStatus().name())
                    .build();
        }

        String sourceDomain = extractDomain(normalizedUrl);
        Company company = resolveCompany(aiData.getCompanyName(), null, sourceDomain);

        String endDateStr = aiData.getRecruitmentPeriod() != null
                ? aiData.getRecruitmentPeriod().getEndDate()
                : null;

        String fingerprintHash = generateFingerprint(company.getName(), aiData.getJobTitle(), endDateStr);

        try {
            return createNewJobEntry(normalizedUrl, sourceUrlHash, fingerprintHash, company, aiData, userId);
        } catch (DataIntegrityViolationException e) {
            log.warn("동시성 충돌 발생. 기존 공고 재조회 시도.");

            JobPost conflictedPost = jobPostRepository.findBySourceUrlHashAndDeletedAtIsNull(sourceUrlHash)
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.INTERNAL_ERROR,
                            "동시성 충돌이 발생했으나 데이터를 찾을 수 없습니다."
                    ));

            return JobAnalysisResultResponse.builder()
                    .jobMasterId(conflictedPost.getJobMaster().getId())
                    .jobPostingId(conflictedPost.getId())
                    .isExisting(true)
                    .build();
        }
    }

    private JobAnalysisResultResponse createNewJobEntry(String url, String urlHash, String fingerprint, Company company,
                                                        AiJobAnalysisResponse aiData, Long userId) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (aiData.getRecruitmentPeriod() != null) {
            startDate = parseDate(aiData.getRecruitmentPeriod().getStartDate());
            endDate = parseDate(aiData.getRecruitmentPeriod().getEndDate());
        }

        JobMaster jobMaster = JobMaster.builder()
                .company(company)
                .jobTitle(aiData.getJobTitle().trim())
                .mainTasks(aiData.getMainResponsibilities())
                .aiSummary(aiData.getAiSummary())
                .startDate(startDate)
                .endDate(endDate)
                .status(determineStatus(endDate))
                .build();

        JobPost jobPost = JobPost.builder()
                .jobMaster(jobMaster)
                .aiJobId(aiData.getJobPostingId())
                .company(company)
                .createdBy(userId)
                .sourceType(JobSourceType.USER)
                .sourceUrl(url)
                .sourceUrlHash(urlHash)
                .fingerprintHash(fingerprint)
                .rawCompanyName(aiData.getCompanyName())
                .rawJobTitle(aiData.getJobTitle())
                .mainTasks(aiData.getMainResponsibilities())
                .startDate(startDate)
                .endDate(endDate)
                .recruitmentStatus(determineRecruitmentStatus(startDate, endDate))
                .registrationStatus(RegistrationStatus.DRAFT)
                .build();

        jobMaster.getJobPosts().add(jobPost);

        if (aiData.getRequiredSkills() != null) {
            for (String skillName : aiData.getRequiredSkills()) {
                Skill skill = resolveSkill(skillName);

                JobMasterSkill jobMasterSkill = JobMasterSkill.builder()
                        .jobMaster(jobMaster)
                        .skill(skill)
                        .build();

                jobMaster.getJobMasterSkills().add(jobMasterSkill);
            }
        }

        JobMaster savedJobMaster = jobMasterRepository.save(jobMaster);
        JobPost savedJobPost = savedJobMaster.getJobPosts().get(0);

        return JobAnalysisResultResponse.builder()
                .jobMasterId(savedJobMaster.getId())
                .jobPostingId(savedJobPost.getId())
                .isExisting(false)
                .companyName(company.getName())
                .jobTitle(savedJobMaster.getJobTitle())
                .mainTasks(savedJobMaster.getMainTasks())
                .skills(savedJobMaster.getJobMasterSkills().stream()
                        .map(jms -> jms.getSkill().getName())
                        .toList())
                .aiSummary(savedJobMaster.getAiSummary())
                .startDate(savedJobMaster.getStartDate())
                .status(savedJobMaster.getStatus().name())
                .build();
    }

    private String normalizeCompanyName(String rawName) {
        if (rawName == null) {
            return "";
        }
        return rawName.trim()
                .replaceAll("\\(주\\)", "")
                .replaceAll("㈜", "")
                .trim();
    }

    private Company resolveCompany(String rawCompanyName, String officialDomain, String sourceDomain) {
        String normalizedName = normalizeCompanyName(rawCompanyName);

        Company company = companyRepository.findByName(normalizedName)
                .orElseGet(() -> companyRepository.save(new Company(normalizedName, officialDomain)));

        if (sourceDomain != null && !sourceDomain.isBlank()) {
            if(!companyAliasRepository.existsByCompanyAndAliasNormalized(company, normalizedName)) {
                companyAliasRepository.save(new CompanyAlias(company, sourceDomain, rawCompanyName, normalizedName));
            }
        }
        return company;
    }

    private Skill resolveSkill(String rawSkillName) {
        String normalizedName = rawSkillName.trim().toUpperCase();

        Optional<Skill> skill = skillRepository.findByName(normalizedName);
        if (skill.isPresent())
            return skill.get();

        Optional<Skill> aliased = skillAliasRepository.findSkillByAliasNormalized(normalizedName);
        if (aliased.isPresent())
            return aliased.get();

        return skillRepository.save(Skill.builder().name(normalizedName).build());
    }

    private RecruitmentStatus determineRecruitmentStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        if (endDate == null) {
            return RecruitmentStatus.OPEN;
        }
        if (today.isAfter(endDate)) {
            return RecruitmentStatus.CLOSED;
        }
        if (startDate != null && startDate.isBefore(endDate)) {
            return RecruitmentStatus.SCHEDULED;
        }
        return RecruitmentStatus.OPEN;
    }

    private JobStatus determineStatus(LocalDate endDate) {
        if (endDate == null) {
            return JobStatus.OPEN;
        }
        if (endDate.isBefore(LocalDate.now())) {
            return JobStatus.CLOSED;
        }
        return JobStatus.OPEN;
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";

        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        String[] paramsToRemove = {
                "relayNonce", "recommend_ids", "search_uuid", "paid_fl",
                "t_ref_content", "t_ref", "utm_source", "utm_medium",
                "utm_campaign", "utm_term", "utm_content", "friend_id",
                "skillSet", "part", "company", "keyword", "employeeType", "page"
        };

        for (String param : paramsToRemove) {
            trimmed = trimmed.replaceAll("([?&])" + param + "=[^&]*", "");
        }

        trimmed = trimmed.replaceAll("&&", "&");
        if (trimmed.endsWith("&") || trimmed.endsWith("?")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String generateFingerprint(String companyName, String jobTitle, Object endDate) {
        String raw = companyName.trim() + "|" + jobTitle.trim() + "|" + (endDate != null ? endDate.toString() : "");
        return generateHash(raw);
    }

    private String generateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "SHA-256 해시 알고리즘 오류가 발생했습니다.");
        }
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return (domain != null && domain.startsWith("www.")) ? domain.substring(4) : domain;
        } catch (Exception e) {
            log.warn("URL 파싱 실패: {}", url);
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "상시채용".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            log.warn("Date parsing failed for value: {}", dateStr);
            return null;
        }
    }
}
