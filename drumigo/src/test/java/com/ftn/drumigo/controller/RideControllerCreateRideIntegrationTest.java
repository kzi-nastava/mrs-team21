package com.ftn.drumigo.controller;

import com.ftn.drumigo.domain.Location;
import com.ftn.drumigo.domain.Ride;
import com.ftn.drumigo.domain.RideWaypoint;
import com.ftn.drumigo.domain.enums.RideStatus;
import com.ftn.drumigo.exception.BadRequestException;
import com.ftn.drumigo.exception.ResourceNotFoundException;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.RideService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class



RideControllerCreateRideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Test
    void createRide_whenPassengerRoleAndValidRequest_returnsCreatedRide() throws Exception {
        Ride ride = acceptedRide(101L, 7L);
        List<RideWaypoint> waypoints = List.of(
            waypoint(ride, 1, "Bulevar Oslobodjenja 1"),
            waypoint(ride, 2, "Narodnog Fronta 10")
        );
        when(rideService.create(eq(7L), any())).thenReturn(ride);
        when(rideService.getRideWaypoints(ride)).thenReturn(waypoints);

        mockMvc.perform(post("/api/rides")
                .with(authenticatedUser(7L, "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRideRequestJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(101))
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.driverId").value(44))
            .andExpect(jsonPath("$.waypoints.length()").value(2));
    }

    @Test
    void createRide_whenUnauthenticated_returns4xxAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/rides")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRideRequestJson()))
            .andExpect(status().is4xxClientError());

        verify(rideService, never()).create(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRide_whenDriverRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/rides")
                .with(authenticatedUser(77L, "DRIVER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRideRequestJson()))
            .andExpect(status().isForbidden());

        verify(rideService, never()).create(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRide_whenRequestValidationFails_returnsBadRequest() throws Exception {
        String invalidJson = """
            {
              "waypoints": [],
              "vehicleType": null
            }
            """;

        mockMvc.perform(post("/api/rides")
                .with(authenticatedUser(7L, "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(rideService, never()).create(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRide_whenServiceThrowsResourceNotFound_returnsNotFound() throws Exception {
        when(rideService.create(eq(7L), any()))
            .thenThrow(new ResourceNotFoundException("Vehicle type not found: STANDARD"));

        mockMvc.perform(post("/api/rides")
                .with(authenticatedUser(7L, "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRideRequestJson()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Vehicle type not found: STANDARD"));
    }

    @Test
    void createRide_whenServiceThrowsBadRequest_returnsBadRequest() throws Exception {
        when(rideService.create(eq(7L), any()))
            .thenThrow(new BadRequestException("Ride must have at least 2 waypoints (start and destination)"));

        mockMvc.perform(post("/api/rides")
                .with(authenticatedUser(7L, "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRideRequestJson()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Ride must have at least 2 waypoints (start and destination)"));
    }

    @Test
    void createRide_passesAuthenticatedPassengerIdToService() throws Exception {
        Ride ride = acceptedRide(202L, 9L);
        when(rideService.create(eq(9L), any())).thenReturn(ride);
        when(rideService.getRideWaypoints(ride)).thenReturn(List.of());

        mockMvc.perform(post("/api/rides")
                .with(authenticatedUser(9L, "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateRideRequestJson()))
            .andExpect(status().isCreated());

        ArgumentCaptor<Long> passengerIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(rideService).create(passengerIdCaptor.capture(), org.mockito.ArgumentMatchers.any());
        org.junit.jupiter.api.Assertions.assertEquals(9L, passengerIdCaptor.getValue());
    }

    private static String validCreateRideRequestJson() {
        return """
            {
              "waypoints": [
                {"address":"Bulevar Oslobodjenja 1","lat":45.2551,"lng":19.8452,"order":1},
                {"address":"Narodnog Fronta 10","lat":45.2466,"lng":19.8369,"order":2}
              ],
              "vehicleType":"STANDARD",
              "babyTransport":false,
              "petTransport":false,
              "linkedPassengerEmails":[]
            }
            """;
    }

    private static RequestPostProcessor authenticatedUser(Long userId, String role) {
        CustomUserDetails principal = new CustomUserDetails(userId, role.toLowerCase() + "@mail.com", role);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private static Ride acceptedRide(Long rideId, Long orderingPassengerId) {
        com.ftn.drumigo.domain.users.Passenger orderingPassenger = new com.ftn.drumigo.domain.users.Passenger();
        orderingPassenger.setId(orderingPassengerId);
        orderingPassenger.setName("Passenger");
        orderingPassenger.setSurname("Test");

        com.ftn.drumigo.domain.users.Driver driver = new com.ftn.drumigo.domain.users.Driver();
        driver.setId(44L);
        driver.setName("Driver");
        driver.setSurname("Assigned");

        Ride ride = new Ride();
        ride.setId(rideId);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setOrderingPassenger(orderingPassenger);
        ride.setDriver(driver);
        ride.setRequestedAt(Instant.now());
        ride.setBabyTransport(false);
        ride.setPetTransport(false);
        ride.setTotalCost(BigDecimal.valueOf(900));
        ride.setTotalDistanceKm(BigDecimal.valueOf(6.2));
        ride.setEstimatedDurationSec(900);
        ride.setEstimatedArrivalAt(Instant.now().plusSeconds(900));
        return ride;
    }

    private static RideWaypoint waypoint(Ride ride, int order, String address) {
        Location location = new Location();
        location.setId((long) order);
        location.setAddress(address);
        location.setLat(BigDecimal.valueOf(45.20 + order));
        location.setLng(BigDecimal.valueOf(19.80 + order));

        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setWaypointOrder(order);
        waypoint.setLocation(location);
        return waypoint;
    }
}