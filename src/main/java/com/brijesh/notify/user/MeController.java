package com.brijesh.notify.user;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public String me(@AuthenticationPrincipal UserDetails userDetails) {
        return "You are authenticated as: "+userDetails.getUsername();
    }
}
