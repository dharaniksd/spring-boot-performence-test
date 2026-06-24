package com.dharaniksd.performence.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lightweight request-logging interceptor that logs method, URI, status, and
 * elapsed time at DEBUG level. Payload logging is intentionally omitted to
 * avoid performance overhead in perf/prod profiles.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor());
    }

    static class RequestLoggingInterceptor implements HandlerInterceptor {

        private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
        private static final String START_ATTR = "reqStartMs";

        @Override
        public boolean preHandle(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler) {
            request.setAttribute(START_ATTR, System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Object handler,
                                    Exception ex) {
            Long start = (Long) request.getAttribute(START_ATTR);
            if (start != null && log.isDebugEnabled()) {
                long elapsed = System.currentTimeMillis() - start;
                log.debug("{} {} -> {} ({}ms)",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        elapsed);
            }
        }
    }
}
