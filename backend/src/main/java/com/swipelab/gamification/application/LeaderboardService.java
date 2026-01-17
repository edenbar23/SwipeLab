package com.swipelab.gamification.application;

import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;

    public List<User> getGlobalLeaderboard(int limit) {
        return userRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "points"))).getContent();
    }

    public List<User> getStreakLeaderboard(int limit) {
        return userRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "currentStreak"))).getContent();
    }
}
