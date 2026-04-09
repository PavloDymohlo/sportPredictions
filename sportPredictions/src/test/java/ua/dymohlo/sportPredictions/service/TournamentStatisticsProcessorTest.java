package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.entity.*;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentStatisticsProcessorTest {

    @Mock
    private GroupCompetitionRepository groupCompetitionRepository;
    @Mock
    private GroupUserStatisticsRepository groupUserStatisticsRepository;
    @Mock
    private GroupTournamentRepository groupTournamentRepository;
    @Mock
    private MatchDataRepository matchDataRepository;
    @Mock
    private MatchParser matchParser;
    @Mock
    private PredictionService predictionService;

    @InjectMocks
    private TournamentStatisticsProcessor processor;

    private User makeUser(Long id, String name) {
        return User.builder().id(id).userName(name).build();
    }

    private UserGroup makeGroup(Long id, String name, List<User> members) {
        User leader = members.isEmpty() ? makeUser(0L, "leader") : members.get(0);
        return UserGroup.builder()
                .id(id).groupName(name).groupLeader(leader)
                .users(new ArrayList<>(members))
                .build();
    }

    private GroupTournament makeTournament(Long id, UserGroup group, LocalDate start, LocalDate finish,
                                           CompetitionStatus status) {
        return GroupTournament.builder()
                .id(id).userGroup(group)
                .startDate(start).finishDate(finish)
                .status(status)
                .build();
    }

    // ─── process — already up to date ─────────────────────────────────────────

    @Test
    void process_alreadyUpToDate_skips() {
        User u = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", List.of(u));
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(10),
                CompetitionStatus.ACTIVE);
        // lastProcessedDate = yesterday → nothing to do
        LocalDate yesterday = LocalDate.now().minusDays(1);
        t.setLastProcessedDate(yesterday);

        processor.process(t, yesterday, LocalDate.now().minusDays(3));

        verifyNoInteractions(matchDataRepository);
    }

    // ─── process — missing match data pauses processing ───────────────────────

    @Test
    void process_missingMatchData_stopsAndDoesNotSave() {
        User u = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", List.of(u));
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate finish = LocalDate.now().plusDays(5);
        GroupTournament t = makeTournament(1L, group, start, finish, CompetitionStatus.ACTIVE);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate ttlLimit = LocalDate.now().minusDays(3);

        when(matchDataRepository.existsByMatchDate(any())).thenReturn(false);

        processor.process(t, yesterday, ttlLimit);

        // Tournament should NOT be saved because processing was paused
        verify(groupTournamentRepository, never()).save(any());
    }

    // ─── process — full flow with stats update ────────────────────────────────

    @Test
    void process_singleDay_calculatesAndSavesStats() {
        User u = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", List.of(u));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        GroupTournament t = makeTournament(1L, group, yesterday, yesterday, CompetitionStatus.ACTIVE);

        Competition comp = Competition.builder().id(1L).country("England").name("PL").code("PL").build();
        GroupCompetition gc = GroupCompetition.builder().id(1L).groupTournament(t).competition(comp).build();

        List<Object> matchResults = List.of(List.of("Arsenal 2", "Chelsea 1"));
        List<Map<String, Object>> matchData = List.of(Map.of("match", matchResults));

        PredictionRequest preds = PredictionRequest.builder()
                .userName("alice")
                .matchDate(yesterday.toString())
                .predictions(List.of(List.of("Arsenal 2", "Chelsea 1")))
                .build();

        when(matchDataRepository.existsByMatchDate(yesterday)).thenReturn(true);
        when(groupCompetitionRepository.findByGroupTournament(t)).thenReturn(List.of(gc));
        when(matchParser.getUserMatches(any(), any())).thenReturn(matchData);
        when(predictionService.getUserPredictions("alice", yesterday.toString()))
                .thenReturn(Optional.of(preds));
        when(groupUserStatisticsRepository.findByGroupTournamentAndUser(t, u))
                .thenReturn(Optional.empty());
        when(groupUserStatisticsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(groupUserStatisticsRepository.findByGroupTournamentOrderedByRanking(t))
                .thenReturn(List.of());
        when(groupTournamentRepository.save(any())).thenReturn(t);

        processor.process(t, yesterday, LocalDate.now().minusDays(3));

        verify(groupUserStatisticsRepository).save(any(GroupUserStatistics.class));
        verify(groupTournamentRepository).save(t);
        assertThat(t.getLastProcessedDate()).isEqualTo(yesterday);
    }

    // ─── process — user has no predictions ────────────────────────────────────

    @Test
    void process_userHasNoPredictions_doesNotUpdateStats() {
        User u = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", List.of(u));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        GroupTournament t = makeTournament(1L, group, yesterday, yesterday, CompetitionStatus.ACTIVE);

        Competition comp = Competition.builder().id(1L).country("England").name("PL").code("PL").build();
        GroupCompetition gc = GroupCompetition.builder().id(1L).groupTournament(t).competition(comp).build();

        List<Object> matchResults = List.of(List.of("Arsenal 2", "Chelsea 1"));
        List<Map<String, Object>> matchData = List.of(Map.of("match", matchResults));

        when(matchDataRepository.existsByMatchDate(yesterday)).thenReturn(true);
        when(groupCompetitionRepository.findByGroupTournament(t)).thenReturn(List.of(gc));
        when(matchParser.getUserMatches(any(), any())).thenReturn(matchData);
        when(predictionService.getUserPredictions("alice", yesterday.toString()))
                .thenReturn(Optional.empty());
        when(groupUserStatisticsRepository.findByGroupTournamentOrderedByRanking(t))
                .thenReturn(List.of());
        when(groupTournamentRepository.save(any())).thenReturn(t);

        processor.process(t, yesterday, LocalDate.now().minusDays(3));

        verify(groupUserStatisticsRepository, never()).save(any());
    }
}
