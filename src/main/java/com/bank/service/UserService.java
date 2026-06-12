package com.bank.service;

import com.bank.dto.RegisterRequest;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;

public interface UserService {
    ServiceResult<Void> register(RegisterRequest request);

    ServiceResult<SessionUser> login(String identity, String password, String ipAddress, String userAgent);

    ServiceResult<Void> setPayPassword(long userId, String loginPassword, String payPassword,
                                       String confirmPayPassword);
}
