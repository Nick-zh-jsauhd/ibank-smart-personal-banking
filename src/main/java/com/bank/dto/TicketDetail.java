package com.bank.dto;

import com.bank.bean.AdjustmentRequest;
import com.bank.bean.ServiceTicket;
import com.bank.bean.TicketActionLog;
import com.bank.bean.TicketReply;

import java.util.ArrayList;
import java.util.List;

public class TicketDetail {
    private ServiceTicket ticket;
    private List<AdjustmentRequest> adjustmentRequests = new ArrayList<AdjustmentRequest>();
    private List<TicketReply> replies = new ArrayList<TicketReply>();
    private List<TicketActionLog> actionLogs = new ArrayList<TicketActionLog>();

    public ServiceTicket getTicket() {
        return ticket;
    }

    public void setTicket(ServiceTicket ticket) {
        this.ticket = ticket;
    }

    public List<AdjustmentRequest> getAdjustmentRequests() {
        return adjustmentRequests;
    }

    public void setAdjustmentRequests(List<AdjustmentRequest> adjustmentRequests) {
        this.adjustmentRequests = adjustmentRequests == null
                ? new ArrayList<AdjustmentRequest>() : adjustmentRequests;
    }

    public List<TicketReply> getReplies() {
        return replies;
    }

    public void setReplies(List<TicketReply> replies) {
        this.replies = replies == null ? new ArrayList<TicketReply>() : replies;
    }

    public List<TicketActionLog> getActionLogs() {
        return actionLogs;
    }

    public void setActionLogs(List<TicketActionLog> actionLogs) {
        this.actionLogs = actionLogs == null ? new ArrayList<TicketActionLog>() : actionLogs;
    }
}
