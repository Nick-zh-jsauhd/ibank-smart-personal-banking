package com.bank.service;

import com.bank.dto.RiskCheckRequest;
import com.bank.dto.RiskDecision;
import com.bank.dto.RiskEventView;
import com.bank.dto.ServiceResult;

import java.sql.Connection;
import java.util.List;

public interface RiskService {
    ServiceResult<RiskDecision> evaluateAndReserve(Connection connection, RiskCheckRequest request);

    ServiceResult<List<RiskEventView>> listEvents(long customerId, String decision);
}
