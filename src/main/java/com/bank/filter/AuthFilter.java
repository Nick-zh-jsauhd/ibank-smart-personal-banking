package com.bank.filter;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(filterName = "AuthFilter", urlPatterns = {
        "/dashboard", "/dashboard.jsp", "/accounts", "/account/*", "/deposit", "/withdraw", "/transfer",
        "/payment", "/payment-records",
        "/transactions", "/transaction/*", "/bill/*", "/security/*", "/wealth/*", "/risk/*",
        "/notifications", "/notifications/*", "/tickets", "/ticket/*", "/assistant", "/assistant.jsp"
})
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("loginUser") == null) {
            String next = URLEncoder.encode(httpRequest.getRequestURI(), "UTF-8");
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?next=" + next);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
