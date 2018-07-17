package com.amadeus.session.agent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.DefaultMethod;
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * This is sample class used to test bytecode logic for injection into
 * {@link Filter} implementations.
 */
public class ByteBuddyFilterAdapter implements AgentBuilder.Transformer {

  private static final String SERVLET_CONTEXT_FIELD_NAME = "$$inject_servletContext";

  private static final String INIT_FOR_SESSION_METHOD_NAME = "$$injected_initForSession";

  private static final String INIT_METHOD_NAME = "init";

  @Override
  public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
      ClassLoader classLoader, JavaModule module) {
    return builder.defineField(SERVLET_CONTEXT_FIELD_NAME, ServletContext.class, Visibility.PROTECTED)
        .defineMethod(INIT_FOR_SESSION_METHOD_NAME, Void.class, Visibility.PUBLIC).withParameter(FilterConfig.class)
        .intercept(MethodDelegation.withDefaultConfiguration()
            .withBinders(FieldProxy.Binder.install(ServletContextGetterSetter.class)).to(ByteBuddyFilterAdapter.class))
        .method(ElementMatchers.<MethodDescription>named(INIT_METHOD_NAME))
        .intercept(MethodCall.invoke(ElementMatchers.<MethodDescription>named(INIT_FOR_SESSION_METHOD_NAME))
            .andThen(MethodCall.invokeSelf()))
        .method(ElementMatchers.<MethodDescription>named("doFilter"))
        .intercept(MethodDelegation.withDefaultConfiguration()
            .withBinders(FieldProxy.Binder.install(ServletContextGetterSetter.class)).to(ByteBuddyFilterAdapter.class));
  }

  /**
   * Initializes session management based on repository for current servlet
   * context. This method is internal method for session management. Note that
   * it has cyrillic letter e in the name.
   *
   * @param config
   *          The filter configuration.
   */
  public void initForSession(@Argument(0) FilterConfig config, @This Filter theFilter,
      @FieldProxy(SERVLET_CONTEXT_FIELD_NAME) ServletContextGetterSetter<ServletContext> servletContextAccessor,
      @SuperMethod(nullIfImpossible = true) Method superInit) {
    if (servletContextAccessor.getServletContext() == null) {
      ServletContext servletContext = config.getServletContext();
      servletContextAccessor.setServletContext(servletContext);
      invokeSessionHelpersFacade(theFilter, "initSessionManagement", servletContext);
    }
    if (superInit != null) {
      try {
        superInit.invoke(theFilter, config);
      } catch (InvocationTargetException e) {
        SessionAgent.debug("Error invoking super.init with filter type %s and parent class %s: %s",
            theFilter.getClass().getCanonicalName(), theFilter.getClass().getSuperclass().getCanonicalName(),
            e.getMessage());
      } catch (IllegalAccessException e) {
        SessionAgent.debug("Error invoking super.init with filter type %s and parent class %s: %s",
            theFilter.getClass().getCanonicalName(), theFilter.getClass().getSuperclass().getCanonicalName(),
            e.getMessage());
      } catch (Throwable e) {
        if (e instanceof Error) {
          throw (Error)e;
        }
        if (e instanceof RuntimeException) {
          throw (RuntimeException)e;
        }
        throw new IllegalStateException("An exception occured while invoking super-class method initForSеssion", e);
      }
    }
  }

  public interface ServletContextGetterSetter<T> {
    T getServletContext();

    void setServletContext(T servletContext);
  }

  private static Object invokeSessionHelpersFacade(Object reference, String methodName, Object... arguments) {
    try {
      return reference.getClass().getClassLoader().loadClass("SessionHelpersFacade").getDeclaredMethod(methodName)
          .invoke(null, arguments);
    } catch (Throwable e) {
      if (e instanceof Error) {
        throw (Error)e;
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      throw new IllegalStateException("An exception occured while invoking " + methodName + " on SessionHelpersFacade",
          e);
    }
  }

  /**
   * Following method is never called is is used to that we can use bytecode/ASM
   * plugin to copy/paste byte code. This method is meant for classes that don't
   * implement {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}
   * but rely on super-class implementation.
   * <p>
   * To use the code, modify this method as needed and test it. Then open
   * bytecode view in eclipse and look at ASM code. Copy the bytecode to
   * addDoFilterWithSuper() method in FilterAdapter class in session-agent.
   *
   * @param request
   * @param response
   * @param chain
   * @throws IOException
   * @throws ServletException
   */
  public void doFilter(@Argument(0) ServletRequest request, @Argument(0) ServletResponse response,
      @Argument(0) FilterChain chain, @DefaultMethod Method doFilterMethod, @This Filter filter,
      @FieldProxy(SERVLET_CONTEXT_FIELD_NAME) ServletContextGetterSetter<ServletContext> servletContextAccessor)
      throws IOException, ServletException {
    ServletRequest oldRequest = request;
    request = (ServletRequest)invokeSessionHelpersFacade(filter, "prepareRequest", oldRequest, response,
        servletContextAccessor.getServletContext());
    response = (ServletResponse)invokeSessionHelpersFacade(filter, "prepareResponse", request, response,
        servletContextAccessor.getServletContext());

    try {
      doFilterMethod.invoke(filter, request, response, chain);

    } catch (Throwable e) {
      if (e instanceof Error) {
        throw (Error)e;
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      if (e instanceof ServletException) {
        throw (ServletException)e;
      }
      if (e instanceof IOException) {
        throw (IOException)e;
      }
      throw new ServletException("An exception occured while invoking super-class method doFilter", e);
    } finally {
      invokeSessionHelpersFacade(filter, "commitRequest", request, oldRequest,
          servletContextAccessor.getServletContext());
    }
  }

}
