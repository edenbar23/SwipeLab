package com.swipelab.gamification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.domain.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GamificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private GamificationController gamificationController;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gamificationController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        userDetails = new User("testuser", "password", Collections.singletonList(new SimpleGrantedAuthority("USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getUserInfo_ShouldReturnGamificationUserInfoResponse() throws Exception {
        Gamification gamification = new Gamification();
        gamification.setScore(100L);
        gamification.setBadge("GOLD");
        gamification.setCurrentStreak(5);

        when(leaderboardService.getGamification("testuser")).thenReturn(gamification);

        mockMvc.perform(get("/api/v1/gamification/user-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(100L))
                .andExpect(jsonPath("$.badge").value("GOLD"))
                .andExpect(jsonPath("$.currentStreak").value(5));
    }

    @Test
    void getGlobalLeaderboard_ShouldReturnListOfGamification() throws Exception {
        Gamification gamification = new Gamification();
        gamification.setUsername("testuser");
        gamification.setScore(200L);

        when(leaderboardService.getGlobalLeaderboard(10)).thenReturn(Collections.singletonList(gamification));

        mockMvc.perform(get("/api/v1/gamification/leaderboard")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].score").value(200L));
    }
}
