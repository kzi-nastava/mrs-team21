package com.ftn.drumigo.controller;

import com.ftn.drumigo.config.SecurityConfig;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.dto.ride.response.RideResponse;
import com.ftn.drumigo.exception.GlobalExceptionHandler;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
class RideControllerStartRideIntegrationTest {

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
    void shouldStartRideAndStartTrackingSimulation() throws Exception {
        Ride startedRide = new Ride();
        startedRide.setId(15L);
        startedRide.setStatus(RideStatus.ACTIVE);

        RideResponse response = new RideResponse(
            15L,
            "ACTIVE",
            Instant.parse("2026-02-18T10:00:00Z"),
            null,
            Instant.parse("2026-02-18T10:05:00Z"),
            null,
            null,
            55L,
            "Mika",
            "Mikic",
            22L,
            null,
            null,
            900,
            Instant.parse("2026-02-18T10:20:00Z"),
            false,
            false,
            List.of()
        );

        when(rideService.startRide(15L, 55L)).thenReturn(startedRide);
        when(rideService.getRideWaypoints(startedRide)).thenReturn(List.<RideWaypoint>of());
        when(rideMapper.toResponse(eq(startedRide), any())).thenReturn(response);

        mockMvc.perform(put("/api/rides/{id}/start", 15L)
                .with(authenticatedUser(55L, "DRIVER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(15))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(rideTrackingSimulationService).startSimulation(15L);
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
