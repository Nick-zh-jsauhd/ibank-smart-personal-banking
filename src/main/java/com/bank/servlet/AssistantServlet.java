package com.bank.servlet;

import com.bank.dto.AssistantReply;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.AssistantService;
import com.bank.service.impl.AssistantServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AssistantServlet", urlPatterns = "/assistant")
public class AssistantServlet extends HttpServlet {
    private final AssistantService assistantService = new AssistantServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String question = RequestUtil.trim(request, "question");
        answer(request, question);
        forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String question = RequestUtil.trim(request, "question");
        answer(request, question);
        forward(request, response);
    }

    private void answer(HttpServletRequest request, String question) {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<AssistantReply> result = assistantService.answer(sessionUser, question);
        request.setAttribute("question", question);
        request.setAttribute("reply", result.getData());
        if (!result.isSuccess()) {
            request.setAttribute("error", result.getMessage());
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("activeNav", "assistant");
        request.getRequestDispatcher("/assistant.jsp").forward(request, response);
    }
}
