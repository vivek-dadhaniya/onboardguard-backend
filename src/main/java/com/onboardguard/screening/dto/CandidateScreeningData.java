package com.onboardguard.screening.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Flat snapshot of a candidate's data passed to the screening engine.
 * Using a dedicated DTO means strategies never hold a reference to the
 * Candidate JPA entity, keeping them stateless and testable.
 */
@Getter
@Builder
public class CandidateScreeningData {

    private Long   candidateId;

    // Personal
    private String fullName;           // e.g. "Rohit S. Sharma"
    private String fullNameNormalized; // lowercase, trimmed, spaces-collapsed
    private String panNumber;          // uppercase, no spaces
    private String aadhaarNumber;      // digits only
    private String passportNumber;

    // Professional
    private String organizationName;
    private String organizationNameNormalized;
    private String designation;
    private String designationNormalized;

    // Type (Employee / Vendor / Contractor) — for future rule extensions
    private String candidateType;
}