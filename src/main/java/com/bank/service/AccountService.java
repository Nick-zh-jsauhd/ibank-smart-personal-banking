package com.bank.service;

import com.bank.bean.Account;
import com.bank.dto.ServiceResult;

import java.util.List;

public interface AccountService {
    ServiceResult<List<Account>> listAccounts(long customerId);
}
