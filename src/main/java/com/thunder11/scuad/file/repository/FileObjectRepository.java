package com.thunder11.scuad.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.file.domain.FileObject;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileObjectRepository extends JpaRepository<FileObject, Long> {
    // fileId로 파일 정보 조회 (메시지 단건 파일 정보 표시용)
    @Query("SELECT f.id, f.originalName, f.contentType, f.sizeBytes " +
            "FROM FileObject f WHERE f.id = :fileId AND f.deletedAt IS NULL")
    Optional<Object[]> findFileInfoById(@Param("fileId") Long fileId);

    // 여러 fileId의 파일 정보 일괄 조회 (메시지 목록 파일 정보 표시용, N+1 방지)
    @Query("SELECT f.id, f.originalName, f.contentType, f.sizeBytes " +
            "FROM FileObject f WHERE f.id IN :fileIds AND f.deletedAt IS NULL")
    List<Object[]> findFileInfosByIds(@Param("fileIds") List<Long> fileIds);

    // fileId로 파일 존재 여부 확인 (파일 전송 시 검증용)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
            "FROM FileObject f WHERE f.id = :fileId AND f.deletedAt IS NULL")
    boolean existsByIdAndNotDeleted(@Param("fileId") Long fileId);
}
