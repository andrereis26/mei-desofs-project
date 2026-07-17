package com.desofs.project.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void doFilter_WhenHeaderMissing_GeneratesRequestIdAndClearsMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(MDC.get("request_id")).isNull();
    }

    @Test
    void doFilter_WhenHeaderContainsUnsafeCharacters_SanitizesAndTruncatesIt() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Request-Id", " abc/def:ghi " + "x".repeat(80));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id"))
                .hasSize(64)
                .doesNotContain("/", ":")
                .startsWith("abc_def_ghi");
    }
}
