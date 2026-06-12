package com.bank.service;

import com.bank.bean.ServiceTicket;
import com.bank.dto.ServiceResult;
import com.bank.dto.TicketDetail;

import java.util.List;

public interface TicketService {
    ServiceResult<ServiceTicket> createTicket(long customerId, long userId, String ticketType, String priority,
                                              String title, String description, String relatedBusinessType,
                                              String relatedBusinessId);

    ServiceResult<List<ServiceTicket>> listCustomerTickets(long customerId);

    ServiceResult<TicketDetail> getCustomerTicket(long customerId, long ticketId);

    ServiceResult<Void> addCustomerReply(long customerId, long userId, long ticketId, String content);

    ServiceResult<Void> closeByCustomer(long customerId, long userId, long ticketId, String note);

    ServiceResult<Void> reopenByCustomer(long customerId, long userId, long ticketId, String note);

    ServiceResult<List<ServiceTicket>> listAdminTickets(long adminUserId, String status, String ticketType,
                                                        String priority, boolean assignedToMe, String keyword);

    ServiceResult<TicketDetail> getAdminTicket(long adminUserId, long ticketId);

    ServiceResult<Void> handleAdminAction(long adminUserId, long ticketId, String action, String assignedRoleCode,
                                          String replyContent, String note, String ipAddress);
}
