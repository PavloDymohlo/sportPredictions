package ua.dymohlo.sportPredictions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.entity.Prediction;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.PredictionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchParser matchParser;
    @Mock
    private PredictionRepository predictionRepository;

    // Use a real ObjectMapper to avoid stubbing issues
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PredictionService predictionService;

    // Re-inject objectMapper since @InjectMocks uses the declared field type
    @org.junit.jupiter.api.BeforeEach
    void injectObjectMapper() {
        org.springframework.test.util.ReflectionTestUtils.setField(predictionService, "objectMapper", objectMapper);
    }

    private User makeUser(Long id, String name) {
        return User.builder().id(id).userName(name).predictionCount(0L).build();
    }

    // ─── saveUserPredictions ──────────────────────────────────────────────────

    @Test
    void saveUserPredictions_newPrediction_savesAndUpdatesPredictionCount() {
        User user = makeUser(1L, "alice");
        List<Object> preds = List.of(List.of("Arsenal 2", "Chelsea 1"));
        PredictionRequest req = PredictionRequest.builder()
                .userName("alice")
                .matchDate("2025-03-01")
                .predictions(preds)
                .build();

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(matchParser.countTotalMatches(preds)).thenReturn(1L);
        when(predictionRepository.findByUserAndMatchDate(user, LocalDate.of(2025, 3, 1)))
                .thenReturn(Optional.empty());
        when(predictionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenReturn(user);

        predictionService.saveUserPredictions(req, "alice");

        assertThat(user.getPredictionCount()).isEqualTo(1L);
        verify(userRepository).save(user);
        verify(predictionRepository).save(any(Prediction.class));
    }

    @Test
    void saveUserPredictions_existingPrediction_updatesExisting() {
        User user = makeUser(1L, "alice");
        List<Object> preds = List.of(List.of("Real 1", "Barca 0"));
        LocalDate date = LocalDate.of(2025, 3, 1);

        Prediction existing = Prediction.builder()
                .id(1L).user(user).matchDate(date)
                .predictionsData("[]")
                .build();

        PredictionRequest req = PredictionRequest.builder()
                .userName("alice").matchDate("2025-03-01").predictions(preds).build();

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(matchParser.countTotalMatches(preds)).thenReturn(1L);
        when(predictionRepository.findByUserAndMatchDate(user, date)).thenReturn(Optional.of(existing));
        when(predictionRepository.save(any())).thenReturn(existing);
        when(userRepository.save(any())).thenReturn(user);

        predictionService.saveUserPredictions(req, "alice");

        ArgumentCaptor<Prediction> captor = ArgumentCaptor.forClass(Prediction.class);
        verify(predictionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L); // same existing entity
    }

    @Test
    void saveUserPredictions_userNotFound_throws() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                predictionService.saveUserPredictions(
                        PredictionRequest.builder()
                                .userName("ghost").matchDate("2025-03-01").predictions(List.of()).build(),
                        "ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── getUserPredictions ───────────────────────────────────────────────────

    @Test
    void getUserPredictions_found_returnsParsedPredictions() throws Exception {
        User user = makeUser(1L, "alice");
        LocalDate date = LocalDate.of(2025, 3, 1);
        List<Object> originalPreds = List.of(List.of("Arsenal 2", "Chelsea 1"));
        String json = objectMapper.writeValueAsString(originalPreds);

        Prediction stored = Prediction.builder()
                .id(1L).user(user).matchDate(date).predictionsData(json).build();

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(predictionRepository.findByUserAndMatchDate(user, date)).thenReturn(Optional.of(stored));

        Optional<PredictionRequest> result = predictionService.getUserPredictions("alice", "2025-03-01");

        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo("alice");
        assertThat(result.get().getMatchDate()).isEqualTo("2025-03-01");
        assertThat(result.get().getPredictions()).hasSize(1);
    }

    @Test
    void getUserPredictions_notFound_returnsEmpty() {
        User user = makeUser(1L, "alice");
        LocalDate date = LocalDate.of(2025, 3, 1);

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(predictionRepository.findByUserAndMatchDate(user, date)).thenReturn(Optional.empty());

        Optional<PredictionRequest> result = predictionService.getUserPredictions("alice", "2025-03-01");

        assertThat(result).isEmpty();
    }

    @Test
    void getUserPredictions_userNotFound_returnsEmpty() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        Optional<PredictionRequest> result = predictionService.getUserPredictions("ghost", "2025-03-01");

        assertThat(result).isEmpty();
    }

    // ─── cleanupOldPredictions ────────────────────────────────────────────────

    @Test
    void cleanupOldPredictions_callsDeleteWithCutoffDate() {
        predictionService.cleanupOldPredictions();

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(predictionRepository).deleteByMatchDateBefore(captor.capture());

        LocalDate expectedCutoff = LocalDate.now().minusDays(3);
        assertThat(captor.getValue()).isEqualTo(expectedCutoff);
    }
}
