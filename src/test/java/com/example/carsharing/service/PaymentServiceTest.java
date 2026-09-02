package com.example.carsharing.service;

import com.example.carsharing.dto.payment.CreatePaymentRequestDto;
import com.example.carsharing.dto.payment.PaymentDto;
import com.example.carsharing.dto.user.UserResponseDto;
import com.example.carsharing.exception.EntityNotFoundException;
import com.example.carsharing.exception.PaymentProcessingException;
import com.example.carsharing.mapper.PaymentMapper;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import com.example.carsharing.notification.RentalNotificationMessage;
import com.example.carsharing.repository.payment.PaymentRepository;
import com.example.carsharing.repository.rental.RentalRepository;
import com.example.carsharing.service.impl.PaymentServiceImpl;
import com.example.carsharing.util.PaymentUtil;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    public static final String SESSION_URL = "mock-session-url";
    private static final String SESSION_ID = "mock-session-id";
    public static final String PAYLOAD = "payload";
    public static final String SIG_HEADER = "sigHeader";
    private static final BigDecimal FEE_199_99 = new BigDecimal("199.99");
    public static final String MESSAGE = "mock-message";
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentUtil paymentUtil;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private UserService userService;
    @Mock
    private RentalNotificationMessage notificationMessageBuilder;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User mockUser(Long id, Role.RoleName roleName) {
        Role role = new Role();
        role.setRole(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@gmail.com");
        user.setPassword("12345678");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRoles(Set.of(role));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()));

        return user;
    }

    @Test
    @DisplayName("createPaymentSession() - Success: Creates Stripe session and returns PaymentDto")
    void createPaymentSession_ValidRequest_ReturnsPaymentDto() throws StripeException {

        CreatePaymentRequestDto requestDto =
                new CreatePaymentRequestDto(100L, Payment.Type.PAYMENT);

        Role customerRole = new Role();
        customerRole.setRole(Role.RoleName.CUSTOMER);

        User loggedUserEntity = new User();
        loggedUserEntity.setId(100L);
        loggedUserEntity.setRoles(Set.of(customerRole));

        Rental rental = new Rental();
        rental.setId(100L);
        rental.setRentalDate(LocalDate.of(2024, 1, 1));
        rental.setReturnDate(LocalDate.of(2024, 1, 5));
        rental.setActualReturnDate(null);
        rental.setUser(loggedUserEntity);

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(Payment.Status.PENDING);
        payment.setType(Payment.Type.PAYMENT);
        payment.setRental(rental);
        payment.setSessionUrl(SESSION_URL);
        payment.setSessionId(SESSION_ID);
        payment.setAmountToPay(FEE_199_99);

        UserResponseDto userResponseDto = new UserResponseDto(
                100L,
                "user@gmail.com",
                "Alice",
                "Murrey",
                Set.of(customerRole.getRole().name())
        );

        PaymentDto pendingPaymentDto = new PaymentDto(
                payment.getId(),
                payment.getStatus(),
                payment.getType(),
                rental.getId(),
                payment.getSessionUrl(),
                payment.getSessionId(),
                payment.getAmountToPay()
        );

        Session mockSession = mock(Session.class);

        when(rentalRepository.findById(requestDto.rentalId())).thenReturn(Optional.of(rental));
        when(userService.getLoggedUser()).thenReturn(userResponseDto);
        when(userService.getUser()).thenReturn(loggedUserEntity);
        when(paymentRepository.existsByRentalIdAndStatus(rental.getId(), payment.getStatus()))
                .thenReturn(false);
        when(paymentUtil.calculateAmount(rental, requestDto.type()))
                .thenReturn(new BigDecimal("100.00"));
        when(mockSession.getUrl()).thenReturn(SESSION_URL);
        when(mockSession.getId()).thenReturn(SESSION_ID);
        when(paymentUtil.createStripeSession(any(), anyLong())).thenReturn(mockSession);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(pendingPaymentDto);

        PaymentDto actual = paymentService.createPaymentSession(requestDto);
        assertThat(actual).isEqualTo(pendingPaymentDto);

        verify(rentalRepository).findById(requestDto.rentalId());
        verify(userService).getLoggedUser();
        verify(paymentUtil).calculateAmount(rental, requestDto.type());
        verify(paymentUtil).createStripeSession(any(), anyLong());
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentMapper).toDto(payment);
    }

    @Test
    @DisplayName("createPaymentSession() - Throws Exception: Return date before rental date")
    void createPaymentSession_InvalidDates_ThrowsPaymentProcessingException() {
        CreatePaymentRequestDto requestDto =
                new CreatePaymentRequestDto(100L, Payment.Type.PAYMENT);
        Role customerRole = new Role();
        customerRole.setRole(Role.RoleName.CUSTOMER);

        User loggedUserEntity = new User();
        loggedUserEntity.setId(100L);
        loggedUserEntity.setRoles(Set.of(customerRole));
        Rental rental = new Rental();
        rental.setId(100L);
        rental.setRentalDate(LocalDate.of(2024, 1, 1));
        rental.setReturnDate(LocalDate.of(2023, 12, 28));
        rental.setActualReturnDate(null);
        rental.setUser(loggedUserEntity);

        UserResponseDto userResponseDto = new UserResponseDto(
                100L,
                "user@gmail.com",
                "Alice",
                "Murrey",
                Set.of(customerRole.getRole().name())
        );

        when(rentalRepository.findById(requestDto.rentalId())).thenReturn(Optional.of(rental));
        when(userService.getLoggedUser()).thenReturn(userResponseDto);
        when(userService.getUser()).thenReturn(loggedUserEntity);

        PaymentProcessingException ex = assertThrows(PaymentProcessingException.class,
                () -> paymentService.createPaymentSession(requestDto));

        assertThat(ex.getMessage()).isEqualTo("Return date cannot be before rental date");
    }

    @Test
    @DisplayName("processSuccessfulPayment() - Success: Marks payment as PAID and sends notification")
    void processSuccessfulPayment_ValidSessionId_ReturnsPaymentDto() throws StripeException {
        Rental rental = new Rental();
        rental.setId(200L);
        rental.setRentalDate(LocalDate.of(2024, 2, 1));
        rental.setReturnDate(LocalDate.of(2024, 2, 10));
        rental.setActualReturnDate(LocalDate.of(2024, 2, 9));

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(Payment.Status.PENDING);
        payment.setType(Payment.Type.PAYMENT);
        payment.setRental(rental);
        payment.setSessionUrl(SESSION_URL);
        payment.setSessionId(SESSION_ID);
        payment.setAmountToPay(FEE_199_99);

        PaymentDto paymentDto = new PaymentDto(
                payment.getId(),
                Payment.Status.PAID,
                payment.getType(),
                rental.getId(),
                payment.getSessionUrl(),
                payment.getSessionId(),
                payment.getAmountToPay()
        );

        Session mockSession = mock(Session.class);
        when(mockSession.getPaymentStatus()).thenReturn("paid");

        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(paymentUtil.retrieveStripeSession(SESSION_ID)).thenReturn(mockSession);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(notificationMessageBuilder.buildSuccessfulPaymentMessage(payment)).thenReturn(MESSAGE);
        doNothing().when(notificationService).send(MESSAGE);
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        PaymentDto actual = paymentService.processSuccessfulPayment(SESSION_ID);

        assertThat(actual.status()).isEqualTo(Payment.Status.PAID);
        assertThat(actual.sessionId()).isEqualTo(SESSION_ID);

        verify(paymentRepository).findBySessionId(SESSION_ID);
        verify(paymentUtil).retrieveStripeSession(SESSION_ID);
        verify(paymentRepository).save(payment);
        verify(notificationService).send(MESSAGE);
        verify(paymentMapper).toDto(payment);
    }

    @Test
    @DisplayName("processSuccessfulPayment() - Throws Exception: Payment not found")
    void processSuccessfulPayment_PaymentNotFound_ThrowsEntityNotFoundException() {
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> paymentService.processSuccessfulPayment(SESSION_ID));

        assertThat(ex.getMessage()).isEqualTo(
                "Can't find payment with session id mock-session-id");
        verify(paymentRepository).findBySessionId(SESSION_ID);
    }

    @Test
    @DisplayName("processCanceledPayment() - Success: Cancels pending payment and sends notification")
    void processCanceledPayment_PendingPayment_ReturnsPaymentDto() {
        CreatePaymentRequestDto requestDto =
                new CreatePaymentRequestDto(300L, Payment.Type.PAYMENT);

        Rental rental = new Rental();
        rental.setId(300L);
        rental.setRentalDate(LocalDate.of(2024, 3, 1));
        rental.setReturnDate(LocalDate.of(2024, 3, 5));
        rental.setActualReturnDate(null);

        Payment payment = new Payment();
        payment.setId(3L);
        payment.setStatus(Payment.Status.PENDING);
        payment.setType(Payment.Type.PAYMENT);
        payment.setRental(rental);
        payment.setSessionUrl(SESSION_URL);
        payment.setSessionId(SESSION_ID);
        payment.setAmountToPay(FEE_199_99);

        PaymentDto paymentDto = new PaymentDto(
                payment.getId(),
                payment.getStatus(),
                payment.getType(),
                rental.getId(),
                payment.getSessionUrl(),
                payment.getSessionId(),
                payment.getAmountToPay()
        );
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(notificationMessageBuilder.buildCanceledPaymentMessage(payment)).thenReturn(MESSAGE);
        doNothing().when(notificationService).send(MESSAGE);
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        PaymentDto actual = paymentService.processCanceledPayment(SESSION_ID);

        assertThat(actual).isEqualTo(paymentDto);
        verify(paymentRepository).findBySessionId(SESSION_ID);
        verify(paymentRepository).save(payment);
        verify(notificationService).send(MESSAGE);
        verify(paymentMapper).toDto(payment);
    }

    @Test
    @DisplayName("getPayments() - Success: Customer gets own payments")
    void getPayments_CustomerGetsOwnPayments_ReturnsPaymentDtoList() {
        User user = mockUser(100L, Role.RoleName.CUSTOMER);

        Rental firstRental = new Rental();
        firstRental.setId(200L);
        firstRental.setRentalDate(LocalDate.of(2024, 2, 1));
        firstRental.setReturnDate(LocalDate.of(2024, 2, 10));
        firstRental.setActualReturnDate(LocalDate.of(2024, 2, 9));

        Payment firstPayment = new Payment();
        firstPayment.setId(1L);
        firstPayment.setStatus(Payment.Status.PENDING);
        firstPayment.setType(Payment.Type.PAYMENT);
        firstPayment.setRental(firstRental);
        firstPayment.setSessionUrl(SESSION_URL);
        firstPayment.setSessionId(SESSION_ID);
        firstPayment.setAmountToPay(FEE_199_99);

        PaymentDto firstPaymentDto = new PaymentDto(
                firstPayment.getId(),
                Payment.Status.PAID,
                firstPayment.getType(),
                firstRental.getId(),
                firstPayment.getSessionUrl(),
                firstPayment.getSessionId(),
                firstPayment.getAmountToPay()
        );

        Rental secondRental = new Rental();
        secondRental.setId(300L);
        secondRental.setRentalDate(LocalDate.of(2024, 3, 1));
        secondRental.setReturnDate(LocalDate.of(2024, 3, 5));
        secondRental.setActualReturnDate(null);

        Payment secondPayment = new Payment();
        secondPayment.setId(3L);
        secondPayment.setStatus(Payment.Status.PENDING);
        secondPayment.setType(Payment.Type.PAYMENT);
        secondPayment.setRental(secondRental);
        secondPayment.setSessionUrl(SESSION_URL);
        secondPayment.setSessionId(SESSION_ID);
        secondPayment.setAmountToPay(FEE_199_99);

        PaymentDto secondPaymentDto = new PaymentDto(
                secondPayment.getId(),
                secondPayment.getStatus(),
                secondPayment.getType(),
                secondRental.getId(),
                secondPayment.getSessionUrl(),
                secondPayment.getSessionId(),
                secondPayment.getAmountToPay()
        );
        Role customerRole = new Role();
        customerRole.setRole(Role.RoleName.CUSTOMER);
        UserResponseDto userResponseDto = new UserResponseDto(
                100L,
                "user@gmail.com",
                "Alice",
                "Murrey",
                Set.of(customerRole.getRole().name())
        );
        List<Payment> payments = List.of(firstPayment, secondPayment);

        when(userService.getUser()).thenReturn(user);
        when(paymentRepository.findAllByRentalUserId(100L)).thenReturn(payments);
        when(paymentMapper.toDto(firstPayment)).thenReturn(firstPaymentDto);
        when(paymentMapper.toDto(secondPayment)).thenReturn(secondPaymentDto);

        List<PaymentDto> actual = paymentService.getPayments(100L);
        assertEquals(2, actual.size());
        verify(userService, times(2)).getUser();
        verify(paymentRepository).findAllByRentalUserId(100L);
        verify(paymentMapper).toDto(firstPayment);
        verify(paymentMapper).toDto(secondPayment);
    }

    @Test
    @DisplayName("handleWebhookEvent() - Success:" +
            " Processes completed session and marks payment as PAID")
    void handleWebhookEvent_SessionCompleted_UpdatesPaymentStatus() {
        ReflectionTestUtils.setField(paymentService,
                "webhookSecret",
                "whsec_test_secret");
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(Payment.Status.PENDING);
        payment.setSessionId(SESSION_ID);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn(PaymentServiceImpl.SESSION_COMPLETE);

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(SESSION_ID);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSession));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(paymentRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        when(notificationMessageBuilder.buildSuccessfulPaymentMessage(payment))
                .thenReturn(MESSAGE);
        doNothing().when(notificationService).send(MESSAGE);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            paymentService.handleWebhookEvent(PAYLOAD, SIG_HEADER);

            assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);

            verify(paymentRepository).findBySessionId(SESSION_ID);
            verify(paymentRepository).save(payment);
            verify(notificationService).send(MESSAGE);
        }
    }

    @Test
    @DisplayName("handleWebhookEvent() - Throws Exception: Invalid Stripe signature")
    void handleWebhookEvent_InvalidSignature_ThrowsPaymentProcessingException() {
        try (MockedStatic<Webhook> webhook = mockStatic(Webhook.class)) {
            webhook.when(() -> Webhook.constructEvent(anyString(), anyString(), any()))
                    .thenThrow(new SignatureVerificationException("invalid", null));
            assertThatThrownBy(() -> paymentService.handleWebhookEvent(PAYLOAD, SIG_HEADER))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessageContaining("Invalid Stripe signature");
        }
    }
}
