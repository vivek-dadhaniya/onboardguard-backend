package com.onboardguard.admin.repository;

import com.onboardguard.admin.entity.ApprovalRequest;
import com.onboardguard.shared.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(RequestStatus requestStatus);

    Boolean existsByTargetEntityTypeAndTargetEntityIdAndStatus(String systemConfig, Long configId, RequestStatus requestStatus);

    Long countByStatus(RequestStatus requestStatus);
}
