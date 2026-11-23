package com.porter.ksa.repository;

import com.porter.ksa.entity.DocumentOwnerType;
import com.porter.ksa.entity.DocumentRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {

    List<DocumentRecord> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(DocumentOwnerType ownerType, Long ownerId);
}
