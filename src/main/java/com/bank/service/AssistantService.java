package com.bank.service;

import com.bank.dto.AssistantReply;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;

public interface AssistantService {
    ServiceResult<AssistantReply> answer(SessionUser sessionUser, String question);
}
