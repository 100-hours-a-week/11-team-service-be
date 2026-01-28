package com.thunder11.scuad.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thunder11.scuad.file.domain.FileObject;

public interface FileObjcetRepository extends JpaRepository<FileObject, Long> {
}
