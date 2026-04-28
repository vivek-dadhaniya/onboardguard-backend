package com.onboardguard.screening.enums;

public enum CorroborationLevel {
    NAME_ONLY,               // multiplier 0.5
    NAME_AND_ONE_ID,         // multiplier 0.8  (PAN or Aadhaar)
    NAME_AND_TWO_IDS,        // multiplier 1.0  (PAN + Aadhaar)
    NAME_AND_ORG,            // multiplier 0.75
    NAME_ORG_AND_DESIGNATION // multiplier 1.0
}