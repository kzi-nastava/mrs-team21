package com.ftn.drumigo.controller;

import com.ftn.drumigo.config.SecurityConfig;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.ride.request.RideStopRequest;
import com.ftn.drumigo.dto.ride.response.RideResponse;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.GlobalExceptionHandler;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.mapper.DriverMapper;
import com.ftn.drumigo.mapper.PanicEventMapper;
import com.ftn.drumigo.mapper.ReviewMapper;
import com.ftn.drumigo.mapper.RideInconsistencyMapper;
import com.ftn.drumigo.mapper.RideMapper;
import com.ftn.drumigo.mapper.VehicleMapper;
import com.ftn.drumigo.repository.UserRepository;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.security.JwtFilter;
import com.ftn.drumigo.service.RideService;
import com.ftn.drumigo.service.RideTrackingSimulationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RideController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "jwt.secret=testSecretKeyThatIsAtLeast32CharactersLongForHS256",
    "jwt.expiration=3600000",
    "maintenance.basic.username=admin",
    "maintenance.basic.password=admin"
})
class RideControllerStopRideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private RideService rideService;
    @MockitoBean private RideMapper rideMapper;
    @MockitoBean private RideInconsistencyMapper rideInconsistencyMapper;
    @MockitoBean private ReviewMapper reviewMapper;
    @MockitoBean private PanicEventMapper panicEventMapper;
    @MockitoBean private DriverMapper driverMapper;
    @MockitoBean private VehicleMapper vehicleMapper;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private RideTrackingSimulationService rideTrackingSimulationService;
    @MockitoBean private JwtFilter jwtFilter;

    @BeforeEach
    void setUpJwtFilterToContinueChain() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void shouldStopRideAndReturnRideResponse_whenRequestIsValid() throws Exception {
        String requestJson = """
            {
              "stopAddress": "Bulevar Oslobodjenja 1",
              "stopLat": 45.25100000,
              "stopLng": 19.84500000
            }
            """;

        Ride stoppedRide = new Ride();
        stoppedRide.setId(10L);
        stoppedRide.setStatus(RideStatus.FINISHED);

        RideResponse response = new RideResponse(
            10L,
            "FINISHED",
            Instant.parse("2026-02-18T10:00:00Z"),
            null,
            Instant.parse("2026-02-18T10:10:00Z"),
            Instant.parse("2026-02-18T10:30:00Z"),
            Instant.parse("2026-02-18T10:30:00Z"),
            55L,
            "Marko",
            "Jovanovic",
            null,
            new BigDecimal("560.00"),
            new BigDecimal("6.20"),
            1200,
            Instant.parse("2026-02-18T10:32:00Z"),
            false,
            false,
            List.of()
        );

        when(rideService.stopRide(eq(10L), eq(55L), any(RideStopRequest.class))).thenReturn(stoppedRide);
        when(rideService.getRideWaypoints(stoppedRide)).thenReturn(List.<RideWaypoint>of());
        when(rideMapper.toResponse(eq(stoppedRide), any())).thenReturn(response);

        mockMvc.perform(put("/api/rides/{id}/stop", 10L)
                .with(authenticatedUser(55L, "DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.status").value("FINISHED"))
            .andExpect(jsonPath("$.driverId").value(55));

        ArgumentCaptor<RideStopRequest> requestCaptor = ArgumentCaptor.forClass(RideStopRequest.class);
        verify(rideService).stopRide(eq(10L), eq(55L), requestCaptor.capture());
        RideStopRequest captured = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("Bulevar Oslobodjenja 1", captured.stopAddress());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("45.25100000"), captured.stopLat());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("19.84500000"), captured.stopLng());
    }

    @Test
    void shouldReturnForbidden_whenUserIsNotAuthenticated() throws Exception {
        String requestJson = """
            {
              "stopAddress": "Bulevar Oslobodjenja 1",
              "stopLat": 45.25100000,
              "stopLng": 19.84500000
            }
            """;

        mockMvc.perform(put("/api/rides/{id}/stop", 10L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnValidationError_whenStopAddressIsBlank() throws Exception {
        String invalidRequestJson = """
            {
              "stopAddress": "   ",
              "stopLat": 45.25100000,
              "stopLng": 19.84500000
            }
            """;

        mockMvc.perform(put("/api/rides/{id}/stop", 10L)
                .with(authenticatedUser(55L, "DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.validationErrors[0].field").value("stopAddress"));
    }

    @Test
    void shouldReturnValidationError_whenLatitudeIsMissing() throws Exception {
        String invalidRequestJson = """
            {
              "stopAddress": "Bulevar Oslobodjenja 1",
              "stopLng": 19.84500000
            }
            """;

        mockMvc.perform(put("/api/rides/{id}/stop", 10L)
                .with(authenticatedUser(55L, "DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnBadRequest_whenServiceRejectsStopRide() throws Exception {
        String requestJson = """
            {
              "stopAddress": "Stop",
              "stopLat": 45.25100000,
              "stopLng": 19.84500000
            }
            """;
        when(rideService.stopRide(eq(10L), eq(55L), any(RideStopRequest.class)))
            .thenThrow(new BadRequestException("Ride must be ACTIVE to be stopped. Current status: ACCEPTED"));

        mockMvc.perform(put("/api/rides/{id}/stop", 10L)
                .with(authenticatedUser(55L, "DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Ride must be ACTIVE to be stopped. Current status: ACCEPTED"));
    }

    @Test
    void shouldReturnNotFound_whenRideDoesNotExist() throws Exception {
        String requestJson = """
            {
              "stopAddress": "Stop",
              "stopLat": 45.25100000,
              "stopLng": 19.84500000
            }
            """;
        when(rideService.stopRide(eq(404L), eq(55L), any(RideStopRequest.class)))
            .thenThrow(new ResourceNotFoundException("Ride not found with id: 404"));

        mockMvc.perform(put("/api/rides/{id}/stop", 404L)
                .with(authenticatedUser(55L, "DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Ride not found with id: 404"));
    }

    private RequestPostProcessor authenticatedUser(Long userId, String role) {
        CustomUserDetails principal = new CustomUserDetails(userId, "user@test.local", role);
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        return authentication(auth);
    }
}
