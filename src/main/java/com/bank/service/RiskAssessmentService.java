package com.bank.service;

import com.bank.bean.RiskAssessment;
import com.bank.dto.ServiceResult;

import java.util.List;

public interface RiskAssessmentService {
    ServiceResult<RiskAssessment> getLatestAssessment(long customerId);

    ServiceResult<RiskAssessment> submitAssessment(long customerId, long userId, String age, String experience,
                                                   String lossTolerance, String goal, String horizon,
                                                   String liquidity, String knowledge);

    ServiceResult<List<RiskAssessment>> listAssessments(long customerId);
}
