package com.thunder11.scuad.jobposting.domain;

import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.thunder11.scuad.common.entity.BaseTimeEntity;
import com.thunder11.scuad.jobposting.domain.type.ParsingStatus;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "application_document_parsed")
public class ApplicationDocumentParsed extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parsed_content_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_document_id", nullable = false, unique = true)
    private ApplicationDocument applicationDocument;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "structured_data", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> structuredData;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "parsing_status", nullable = false, length = 20)
    private ParsingStatus parsingStatus;

    @Column(name = "model_info", length = 50)
    private String modelInfo;

    @Column(name = "token_count")
    private Integer tokenCount;
}
