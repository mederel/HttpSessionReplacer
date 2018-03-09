package com.amadeus.session.agent;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class MockDerivedFilter extends MockAbstractFilter {
  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig config) throws ServletException {
  }
}
