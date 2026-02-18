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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RideControllerEndRideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Test
    void endRide_whenDriverIsAssignedAndRideIsActive_returnsOkResponse() throws Exception {
        Ride ride = finishedRide(55L, 77L);
        List<RideWaypoint> waypoints = List.of(waypoint(ride, 0, "Pickup"), waypoint(ride, 1, "Destination"));

        when(rideService.endRideByDriverId(55L, 77L)).thenReturn(ride);
        when(rideService.getRideWaypoints(ride)).thenReturn(waypoints);

        mockMvc.perform(put("/api/rides/{id}/end", 55L).with(authenticatedUser(77L, "DRIVER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(55))
            .andExpect(jsonPath("$.status").value("FINISHED"))
            .andExpect(jsonPath("$.driverId").value(77));

        verify(rideService).endRideByDriverId(55L, 77L);
    }

    @Test
    void endRide_whenUnauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/rides/{id}/end", 55L))
            .andExpect(status().is4xxClientError());

        verify(rideService, never()).endRideByDriverId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void endRide_whenAuthenticatedButPassengerRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/rides/{id}/end", 55L).with(authenticatedUser(77L, "PASSENGER")))
            .andExpect(status().isForbidden());

        verify(rideService, never()).endRideByDriverId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void endRide_whenRideDoesNotExist_returnsNotFoundWithErrorPayload() throws Exception {
        when(rideService.endRideByDriverId(404L, 77L))
            .thenThrow(new ResourceNotFoundException("Ride not found with id: 404"));

        mockMvc.perform(put("/api/rides/{id}/end", 404L).with(authenticatedUser(77L, "DRIVER")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Ride not found with id: 404"))
            .andExpect(jsonPath("$.path").value("/api/rides/404/end"));
    }

    @Test
    void endRide_whenRideCannotBeEnded_returnsBadRequestWithReason() throws Exception {
        String reason = "Ride must be ACTIVE to be ended. Current status: ACCEPTED";
        when(rideService.endRideByDriverId(66L, 77L)).thenThrow(new BadRequestException(reason));

        mockMvc.perform(put("/api/rides/{id}/end", 66L).with(authenticatedUser(77L, "DRIVER")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value(reason))
            .andExpect(jsonPath("$.path").value("/api/rides/66/end"));
    }

    @Test
    void endRide_passesAuthenticatedDriverIdToService() throws Exception {
        Ride ride = finishedRide(72L, 999L);
        when(rideService.getRideWaypoints(ride)).thenReturn(List.of());

        when(rideService.endRideByDriverId(72L, 999L)).thenReturn(ride);

        mockMvc.perform(put("/api/rides/{id}/end", 72L).with(authenticatedUser(999L, "DRIVER")))
            .andExpect(status().isOk());

        ArgumentCaptor<Long> rideIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> driverIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(rideService).endRideByDriverId(rideIdCaptor.capture(), driverIdCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(72L, rideIdCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(999L, driverIdCaptor.getValue());
    }

    private static RequestPostProcessor authenticatedUser(Long userId, String role) {
        CustomUserDetails principal = new CustomUserDetails(userId, role.toLowerCase() + "@mail.com", role);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private static Ride finishedRide(Long rideId, Long driverId) {
        com.ftn.drumigo.domain.users.Driver driver = new com.ftn.drumigo.domain.users.Driver();
        driver.setId(driverId);
        driver.setName("Driver");
        driver.setSurname("Test");

        Ride ride = new Ride();
        ride.setId(rideId);
        ride.setStatus(RideStatus.FINISHED);
        ride.setDriver(driver);
        ride.setRequestedAt(Instant.now().minusSeconds(600));
        ride.setStartTime(Instant.now().minusSeconds(300));
        ride.setEndTime(Instant.now());
        ride.setPaidAt(Instant.now());
        ride.setTotalCost(BigDecimal.valueOf(1230));
        ride.setTotalDistanceKm(BigDecimal.valueOf(8.4));
        ride.setEstimatedDurationSec(1200);
        ride.setEstimatedArrivalAt(Instant.now());
        ride.setBabyTransport(false);
        ride.setPetTransport(false);
        return ride;
    }

    private static RideWaypoint waypoint(Ride ride, int order, String address) {
        Location location = new Location();
        location.setId((long) (order + 1));
        location.setAddress(address);
        location.setLat(BigDecimal.valueOf(45.25 + order));
        location.setLng(BigDecimal.valueOf(19.82 + order));

        RideWaypoint waypoint = new RideWaypoint();
        waypoint.setRide(ride);
        waypoint.setWaypointOrder(order);
        waypoint.setLocation(location);
        return waypoint;
    }
}
