package com.example.carsharing.controller;

import com.example.carsharing.dto.payment.CreatePaymentRequestDto;
import com.example.carsharing.dto.payment.PaymentDto;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.example.carsharing.service.NotificationService;
import com.example.carsharing.service.impl.PaymentServiceImpl;
import com.example.carsharing.util.PaymentUtil;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {"classpath:database/payment/clean/remove-all-payments.sql",
        "classpath:database/payment/add-payments.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PaymentControllerTest {
    public static final String SESSION_URL_ABC = "https://session/abc";
    public static final String SESSION_URL_DEF = "https://session/def";
    public static final String SESSION_URL_XYZ = "https://session/xyz";
    private static final String SESSION_ID_ABC = "session-abc";
    private static final String SESSION_ID_XYZ = "session-xyz";
    private static final String SESSION_ID_DEF = "session-def";
    public static final String PAYLOAD = "payload";
    public static final String SIG_HEADER = "sigHeader";
    private static final BigDecimal FEE_200_00 = new BigDecimal("200.00");
    private static final BigDecimal FEE_199_99 = new BigDecimal("199.99");
    private static final BigDecimal FEE_150_00 = new BigDecimal("150.00");
    private static final BigDecimal FEE_100_00 = new BigDecimal("100.00");

    @Autowired
    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentUtil paymentUtil;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeAll
    static void beforeAll(
            @Autowired WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity()).build();
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("createPaymentSession() - Success: Creates Stripe session and returns PaymentDto")
    void createPaymentSession_ValidRequest_ReturnsPaymentDto() throws Exception {
        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(
                3L,
                Payment.Type.PAYMENT
        );
        Session fakeStripeSession = mock(Session.class);
        when(fakeStripeSession.getUrl()).thenReturn(SESSION_URL_XYZ);
        when(fakeStripeSession.getId()).thenReturn(SESSION_ID_XYZ);
        when(paymentUtil.calculateAmount(any(Rental.class), eq(Payment.Type.PAYMENT)))
                .thenReturn(FEE_199_99);
        when(paymentUtil.createStripeSession(any(BigDecimal.class), anyLong()))
                .thenReturn(fakeStripeSession);

        PaymentDto expected = new PaymentDto(
                10L,
                Payment.Status.PENDING,
                Payment.Type.PAYMENT,
                4L,
                SESSION_URL_XYZ,
                SESSION_ID_XYZ,
                FEE_199_99
        );

        String json = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(post("/payments")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();
        PaymentDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                PaymentDto.class);

        assertThat(actual)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("createPaymentSession() - Validation Error: Invalid request body returns 400")
    void createPaymentSession_InvalidRequestDto_ReturnsBadRequest() throws Exception {
        CreatePaymentRequestDto requestDto = new CreatePaymentRequestDto(
                null,
                Payment.Type.PAYMENT);
        String json = objectMapper.writeValueAsString(requestDto);
        mockMvc.perform(post("/payments")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("processSuccessfulPayment() - Success: Returns PaymentDto after successful payment")
    void processSuccessfulPayment_ValidSessionId_ReturnsPaymentDto() throws Exception {
        Session fakeSession = mock(Session.class);
        when(fakeSession.getPaymentStatus()).thenReturn("paid");
        when(paymentUtil.retrieveStripeSession(SESSION_ID_ABC)).thenReturn(fakeSession);
        PaymentDto expected = new PaymentDto(
                564L,
                Payment.Status.PAID,
                Payment.Type.PAYMENT,
                1L,
                SESSION_URL_ABC,
                SESSION_ID_ABC,
                FEE_100_00);

        MvcResult result = mockMvc.perform(get("/payments/success")
                        .param("session_id", SESSION_ID_ABC))
                .andExpect(status().isOk())
                .andReturn();

        PaymentDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                PaymentDto.class);

        verify(notificationService).send(anyString());

        assertThat(actual)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .ignoringFields("id")
                .isEqualTo(expected);

    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("processSuccessfulPayment() - Not Found: Session ID not found returns 404")
    void processSuccessfulPayment_SessionNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/payments/success")
                        .param("session_id", "non-exists-session"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("processCanceledPayment() - Success: Returns PaymentDto after canceled payment")
    void processCanceledPayment_ValidSessionId_ReturnsPaymentDto() throws Exception {
        PaymentDto expected = new PaymentDto(
                111L,
                Payment.Status.CANCELED,
                Payment.Type.PAYMENT,
                1L,
                SESSION_URL_ABC,
                SESSION_ID_ABC,
                FEE_100_00);

        MvcResult result = mockMvc.perform(get("/payments/cancel")
                        .param("session_id", SESSION_ID_ABC))
                .andExpect(status().isOk())
                .andReturn();

        PaymentDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                PaymentDto.class);

        verify(notificationService).send(anyString());

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("processCanceledPayment() - Not Found: Session ID not found returns 404")
    void processCanceledPayment_SessionNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/payments/cancel")
                        .param("session_id", "non-exists-session"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @WithUserDetails(value = "manager@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("getPayments() - Success: Manager retrieves all payments")
    void getPayments_ManagerGetsAllPayments_ReturnsList() throws Exception {
        List<PaymentDto> expected = List.of(
                new PaymentDto(
                        1L,
                        Payment.Status.PENDING,
                        Payment.Type.PAYMENT,
                        1L,
                        SESSION_URL_ABC,
                        SESSION_ID_ABC,
                        FEE_100_00),
                new PaymentDto(
                        2L,
                        Payment.Status.PAID,
                        Payment.Type.FINE,
                        2L,
                        SESSION_URL_DEF,
                        SESSION_ID_DEF,
                        FEE_150_00),
                new PaymentDto(
                        3L,
                        Payment.Status.PENDING,
                        Payment.Type.PAYMENT,
                        3L,
                        SESSION_URL_XYZ,
                        SESSION_ID_XYZ,
                        FEE_200_00));

        MvcResult result = mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        List<PaymentDto> actual = objectMapper.readValue(response,
                new TypeReference<>() {
                });

        assertThat(actual.size()).isEqualTo(3);
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @WithUserDetails(value = "customer@gmail.com",
            userDetailsServiceBeanName = "customUserDetailsService")
    @Test
    @DisplayName("getPayments() - Success: Customer retrieves only own payments")
    void getPayments_CustomerGetsOwnPayments_ReturnsList() throws Exception {
        MvcResult result = mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        List<PaymentDto> actual = objectMapper.readValue(response,
                new TypeReference<>() {
                });

        assertFalse(actual.isEmpty());
    }

    @Test
    @DisplayName("handleWebhook() - Success: Valid Stripe webhook payload returns 200")
    void handleWebhook_ValidPayload_ReturnsOk() throws Exception {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(PaymentServiceImpl.SESSION_COMPLETE);

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(SESSION_ID_ABC);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSession));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(PAYLOAD), eq(SIG_HEADER), anyString()))
                    .thenReturn(event);

            mockMvc.perform(post("/payments/webhook")
                            .content(PAYLOAD)
                            .header("Stripe-Signature", SIG_HEADER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("handleWebhook() - Error: Invalid Stripe signature returns 400")
    void handleWebhook_InvalidSignature_ReturnsBadRequest() throws Exception {
        SignatureVerificationException signatureException =
                mock(SignatureVerificationException.class);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(eq(PAYLOAD), eq(SIG_HEADER), anyString()))
                    .thenThrow(signatureException);

            mockMvc.perform(post("/payments/webhook")
                            .content(PAYLOAD)
                            .header("Stripe-Signature", SIG_HEADER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}
