package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.dao.AccountDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.service.AccountService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AccountServiceImpl implements AccountService {
    private final AccountDao accountDao = new AccountDaoImpl();

    @Override
    public ServiceResult<List<Account>> listAccounts(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            List<Account> accounts = accountDao.findByCustomerId(connection, customerId);
            return ServiceResult.success("账户查询成功。", accounts);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("账户查询失败，请检查数据库配置或稍后重试。");
        }
    }
}
