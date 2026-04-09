package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.dto.response.UserRankingResponse;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRankingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRankingService userRankingService;

    private User makeUser(Long id, String name, long score, long predCount, int percent) {
        return User.builder()
                .id(id).userName(name)
                .totalScore(score)
                .predictionCount(predCount)
                .percentGuessedMatches(percent)
                .rankingPosition(0L)
                .build();
    }

    @Test
    void getAllUsers_returnsCorrectlyMappedResponses() {
        User u1 = makeUser(1L, "alice", 100, 50, 80);
        User u2 = makeUser(2L, "bob", 80, 40, 60);
        when(userRepository.findAllRanked()).thenReturn(List.of(u1, u2));
        when(userRepository.saveAll(any())).thenReturn(List.of(u1, u2));

        List<UserRankingResponse> result = userRankingService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserName()).isEqualTo("alice");
        assertThat(result.get(0).getTotalScore()).isEqualTo(100);
        assertThat(result.get(1).getUserName()).isEqualTo("bob");
    }

    @Test
    void getAllUsers_assignsConsecutiveRankingPositions() {
        User u1 = makeUser(1L, "alice", 100, 50, 80);
        User u2 = makeUser(2L, "bob", 80, 40, 60);
        User u3 = makeUser(3L, "charlie", 60, 30, 40);
        when(userRepository.findAllRanked()).thenReturn(List.of(u1, u2, u3));
        when(userRepository.saveAll(any())).thenReturn(List.of(u1, u2, u3));

        userRankingService.getAllUsers();

        assertThat(u1.getRankingPosition()).isEqualTo(1L);
        assertThat(u2.getRankingPosition()).isEqualTo(2L);
        assertThat(u3.getRankingPosition()).isEqualTo(3L);
    }

    @Test
    void getAllUsers_savesAllUsersWithUpdatedPositions() {
        User u1 = makeUser(1L, "alice", 100, 50, 80);
        User u2 = makeUser(2L, "bob", 80, 40, 60);
        when(userRepository.findAllRanked()).thenReturn(List.of(u1, u2));
        when(userRepository.saveAll(any())).thenReturn(List.of(u1, u2));

        userRankingService.getAllUsers();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(u1, u2);
    }

    @Test
    void getAllUsers_emptyList_returnsEmpty() {
        when(userRepository.findAllRanked()).thenReturn(List.of());
        when(userRepository.saveAll(any())).thenReturn(List.of());

        List<UserRankingResponse> result = userRankingService.getAllUsers();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllUsers_mappingIncludesAllResponseFields() {
        User u = makeUser(1L, "alice", 150, 75, 70);
        when(userRepository.findAllRanked()).thenReturn(List.of(u));
        when(userRepository.saveAll(any())).thenReturn(List.of(u));

        List<UserRankingResponse> result = userRankingService.getAllUsers();

        UserRankingResponse response = result.get(0);
        assertThat(response.getUserName()).isEqualTo("alice");
        assertThat(response.getTotalScore()).isEqualTo(150);
        assertThat(response.getPredictionCount()).isEqualTo(75);
        assertThat(response.getPercentGuessedMatches()).isEqualTo(70);
        assertThat(response.getRankingPosition()).isEqualTo(1L);
    }
}
