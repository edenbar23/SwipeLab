package com.swipelab.auth.external;

import com.swipelab.dto.request.ExternalLoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/external")
@RequiredArgsConstructor
public class ExternalAuthController {

    private final StardbiAuthService stardbiAuthService;

    @PostMapping("/stardbi/loginExternal")
    public ResponseEntity<Void> loginExternal(@Valid @RequestBody ExternalLoginRequest request) {
        boolean success = stardbiAuthService.loginExternal(request);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(401).build();
        }
    }
}
