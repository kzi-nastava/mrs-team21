package com.ftn.drumigo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * When the driver activation email contains an ngrok (or https) link like
 * https://xxx.ngrok-free.app/activate-driver/TOKEN, opening it in a browser hits this endpoint.
 * We redirect to the app custom scheme so the mobile app can open and handle set-password.
 */
@RestController
public class DriverActivationRedirectController {

    @GetMapping("/activate-driver/{token}")
    public void redirectToApp(@PathVariable String token, HttpServletResponse response) throws IOException {
        response.sendRedirect("drumigo://activate-driver/" + token);
    }
}
