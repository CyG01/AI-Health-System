package com.example.aspect;

import com.example.annotation.RequiresSubscription;
import com.example.billing.SubscriptionService;
import com.example.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionAspectTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private RequiresSubscription requiresSubscription;

    @Mock
    private Signature signature;

    private SubscriptionAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new SubscriptionAspect(subscriptionService);
        lenient().when(pjp.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn("testMethod");
    }

    private void setRequestUserId(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userId != null) {
            request.setAttribute("userId", userId);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("userId present + has access -> proceeds normally")
    void shouldProceedWhenUserHasAccess() throws Throwable {
        setRequestUserId(1L);
        when(requiresSubscription.value()).thenReturn("pro");
        when(requiresSubscription.feature()).thenReturn("AI Analysis");
        when(subscriptionService.hasAccess(1L, "pro")).thenReturn(true);
        when(pjp.proceed()).thenReturn("success");

        Object result = aspect.checkSubscription(pjp, requiresSubscription);

        assertEquals("success", result);
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("userId present + no access -> throws BusinessException(402)")
    void shouldThrow402WhenUserHasNoAccess() throws Throwable {
        setRequestUserId(1L);
        when(requiresSubscription.value()).thenReturn("pro");
        when(requiresSubscription.feature()).thenReturn("AI Analysis");
        when(subscriptionService.hasAccess(1L, "pro")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkSubscription(pjp, requiresSubscription));

        assertEquals(402, ex.getCode());
        verify(pjp, never()).proceed();
    }

    @Test
    @DisplayName("userId null -> throws BusinessException(401)")
    void shouldThrow401WhenUserIdIsNull() throws Throwable {
        setRequestUserId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkSubscription(pjp, requiresSubscription));

        assertEquals(401, ex.getCode());
        verify(pjp, never()).proceed();
    }

    @Test
    @DisplayName("no request context -> throws BusinessException(401)")
    void shouldThrow401WhenNoRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkSubscription(pjp, requiresSubscription));

        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("no access with blank feature -> generic upgrade message")
    void shouldUseGenericMessageWhenFeatureIsBlank() {
        setRequestUserId(1L);
        when(requiresSubscription.value()).thenReturn("enterprise");
        when(requiresSubscription.feature()).thenReturn("");
        when(subscriptionService.hasAccess(1L, "enterprise")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> aspect.checkSubscription(pjp, requiresSubscription));

        assertEquals(402, ex.getCode());
        assertTrue(ex.getMessage().contains("企业"));
    }
}
