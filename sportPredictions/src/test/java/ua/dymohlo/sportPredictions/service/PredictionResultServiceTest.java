package ua.dymohlo.sportPredictions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.entity.Prediction;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserCompetition;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;
import ua.dymohlo.sportPredictions.repository.PredictionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionResultServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCompetitionRepository userCompetitionRepository;
    @Mock
    private MatchParser matchParser;
    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private MatchDataRepository matchDataRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PredictionResultService predictionResultService;

    @org.junit.jupiter.api.BeforeEach
    void injectObjectMapper() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                predictionResultService, "objectMapper", objectMapper);
    }

    private User makeUser(Long id, String name) {
        return User.builder()
                .id(id).userName(name)
                .totalScore(0L).predictionCount(0L).percentGuessedMatches(0)
                .build();
    }

    private Prediction makePrediction(User user, String predictionsJson) {
        return Prediction.builder()
                .id(1L).user(user)
                .matchDate(LocalDate.now().minusDays(1))
                .predictionsData(predictionsJson)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── no match data ────────────────────────────────────────────────────────

    @Test
    void countAllUsersPredictionsResult_noMatchData_skipsProcessing() {
        when(matchDataRepository.existsByMatchDate(any())).thenReturn(false);

        predictionResultService.countAllUsersPredictionsResult();

        verifyNoInteractions(predictionRepository);
        verifyNoInteractions(userRepository);
    }

    // ─── no predictions for yesterday ────────────────────────────────────────

    @Test
    void countAllUsersPredictionsResult_noPredictions_doesNothing() {
        when(matchDataRepository.existsByMatchDate(any())).thenReturn(true);
        when(predictionRepository.findByMatchDateWithUser(any())).thenReturn(List.of());

        predictionResultService.countAllUsersPredictionsResult();

        verify(userRepository, never()).save(any());
    }

    // ─── correct prediction → score incremented ───────────────────────────────

    @Test
    void countAllUsersPredictionsResult_correctPrediction_incrementsScore() throws Exception {
        User user = makeUser(1L, "alice");

        List<Object> predictions = List.of(List.of("Arsenal 2", "Chelsea 1"));
        String predictionsJson = objectMapper.writeValueAsString(predictions);
        Prediction prediction = makePrediction(user, predictionsJson);

        Competition comp = Competition.builder()
                .id(1L).country("England").name("Premier League").code("PL").build();
        UserCompetition userComp = UserCompetition.builder()
                .id(1L).user(user).competition(comp).build();

        List<Map<String, Object>> matchData = List.of(
                Map.of("country", "England", "tournament", "Premier League",
                        "match", List.of(List.of("Arsenal 2", "Chelsea 1")))
        );

        when(matchDataRepository.existsByMatchDate(any())).thenReturn(true);
        when(predictionRepository.findByMatchDateWithUser(any())).thenReturn(List.of(prediction));
        when(userCompetitionRepository.findByUser(user)).thenReturn(List.of(userComp));
        when(matchParser.getUserMatches(any(), any())).thenReturn(matchData);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        predictionResultService.countAllUsersPredictionsResult();

        assertThat(user.getTotalScore()).isEqualTo(1L);
        verify(userRepository).save(user);
    }

    // ─── no subscriptions → score unchanged ───────────────────────────────────

    @Test
    void countAllUsersPredictionsResult_noSubscriptions_scoreUnchanged() throws Exception {
        User user = makeUser(1L, "alice");

        String predictionsJson = objectMapper.writeValueAsString(
                List.of(List.of("Arsenal 2", "Chelsea 1")));
        Prediction prediction = makePrediction(user, predictionsJson);

        when(matchDataRepository.existsByMatchDate(any())).thenReturn(true);
        when(predictionRepository.findByMatchDateWithUser(any())).thenReturn(List.of(prediction));
        when(userCompetitionRepository.findByUser(user)).thenReturn(List.of());

        predictionResultService.countAllUsersPredictionsResult();

        assertThat(user.getTotalScore()).isEqualTo(0L);
        verify(userRepository, never()).save(any());
    }

    // ─── no matches for user tournaments → score unchanged ────────────────────

    @Test
    void countAllUsersPredictionsResult_noMatchesForUserTournaments_scoreUnchanged() throws Exception {
        User user = makeUser(1L, "alice");

        String predictionsJson = objectMapper.writeValueAsString(
                List.of(List.of("Arsenal 2", "Chelsea 1")));
        Prediction prediction = makePrediction(user, predictionsJson);

        Competition comp = Competition.builder()
                .id(1L).country("England").name("Premier League").code("PL").build();
        UserCompetition userComp = UserCompetition.builder()
                .id(1L).user(user).competition(comp).build();

        when(matchDataRepository.existsByMatchDate(any())).thenReturn(true);
        when(predictionRepository.findByMatchDateWithUser(any())).thenReturn(List.of(prediction));
        when(userCompetitionRepository.findByUser(user)).thenReturn(List.of(userComp));
        when(matchParser.getUserMatches(any(), any())).thenReturn(List.of());

        predictionResultService.countAllUsersPredictionsResult();

        assertThat(user.getTotalScore()).isEqualTo(0L);
        verify(userRepository, never()).save(any());
    }

    // ─── exception for one user → continues processing others ────────────────

    @Test
    void countAllUsersPredictionsResult_exceptionForOneUser_continuesOthers() throws Exception {
        User user1 = makeUser(1L, "alice");
        User user2 = makeUser(2L, "bob");

        String corruptJson = "not-valid-json";
        Prediction prediction1 = makePrediction(user1, corruptJson);

        List<Object> predictions2 = List.of(List.of("Real 1", "Barca 0"));
        String json2 = objectMapper.writeValueAsString(predictions2);
        Prediction prediction2 = Prediction.builder()
                .id(2L).user(user2)
                .matchDate(LocalDate.now().minusDays(1))
                .predictionsData(json2)
                .createdAt(LocalDateTime.now())
                .build();

        Competition comp = Competition.builder()
                .id(1L).country("Spain").name("La Liga").code("PD").build();
        UserCompetition userComp2 = UserCompetition.builder()
                .id(2L).user(user2).competition(comp).build();

        List<Map<String, Object>> matchData = List.of(
                Map.of("country", "Spain", "tournament", "La Liga",
                        "match", List.of(List.of("Real 1", "Barca 0")))
        );

        when(matchDataRepository.existsByMatchDate(any())).thenReturn(true);
        when(predictionRepository.findByMatchDateWithUser(any())).thenReturn(List.of(prediction1, prediction2));
        when(userCompetitionRepository.findByUser(user2)).thenReturn(List.of(userComp2));
        when(matchParser.getUserMatches(any(), any())).thenReturn(matchData);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        predictionResultService.countAllUsersPredictionsResult();

        // user2 should still be processed despite user1 failing
        assertThat(user2.getTotalScore()).isEqualTo(1L);
        verify(userRepository).save(user2);
    }
}
