package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.entity.*;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentLifecycleServiceTest {

    @Mock
    private GroupTournamentRepository groupTournamentRepository;
    @Mock
    private GroupCompetitionRepository groupCompetitionRepository;
    @Mock
    private GroupUserStatisticsRepository groupUserStatisticsRepository;
    @Mock
    private CompetitionService competitionService;

    @InjectMocks
    private TournamentLifecycleService tournamentLifecycleService;

    private User makeUser(Long id, String name) {
        return User.builder().id(id).userName(name).build();
    }

    private UserGroup makeGroup(Long id, String name, User leader) {
        return UserGroup.builder().id(id).groupName(name).groupLeader(leader).build();
    }

    private GroupTournament makeTournament(Long id, UserGroup group, LocalDate start, LocalDate finish,
                                           CompetitionStatus status) {
        return GroupTournament.builder()
                .id(id).userGroup(group)
                .startDate(start).finishDate(finish)
                .status(status)
                .build();
    }

    // ─── updateTournamentStatuses ─────────────────────────────────────────────

    @Test
    void updateTournamentStatuses_futureStart_setsNotStarted() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(30),
                CompetitionStatus.ACTIVE);  // wrong status for future tournament

        when(groupTournamentRepository.findByStatusIn(anyList())).thenReturn(List.of(t));

        tournamentLifecycleService.updateTournamentStatuses();

        assertThat(t.getStatus()).isEqualTo(CompetitionStatus.NOT_STARTED);
        verify(groupTournamentRepository).save(t);
    }

    @Test
    void updateTournamentStatuses_currentDate_setsActive() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5),
                CompetitionStatus.NOT_STARTED);

        when(groupTournamentRepository.findByStatusIn(anyList())).thenReturn(List.of(t));

        tournamentLifecycleService.updateTournamentStatuses();

        assertThat(t.getStatus()).isEqualTo(CompetitionStatus.ACTIVE);
        verify(groupTournamentRepository).save(t);
    }

    @Test
    void updateTournamentStatuses_pastEnd_setsFinished() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1),
                CompetitionStatus.ACTIVE);

        when(groupTournamentRepository.findByStatusIn(anyList())).thenReturn(List.of(t));

        tournamentLifecycleService.updateTournamentStatuses();

        assertThat(t.getStatus()).isEqualTo(CompetitionStatus.FINISHED);
        verify(groupTournamentRepository).save(t);
    }

    @Test
    void updateTournamentStatuses_statusUnchanged_doesNotSave() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5),
                CompetitionStatus.ACTIVE);  // already correct

        when(groupTournamentRepository.findByStatusIn(anyList())).thenReturn(List.of(t));

        tournamentLifecycleService.updateTournamentStatuses();

        verify(groupTournamentRepository, never()).save(any());
    }

    @Test
    void updateTournamentStatuses_nullDates_setsNotStarted() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group, null, null, CompetitionStatus.ACTIVE);

        when(groupTournamentRepository.findByStatusIn(anyList())).thenReturn(List.of(t));

        tournamentLifecycleService.updateTournamentStatuses();

        assertThat(t.getStatus()).isEqualTo(CompetitionStatus.NOT_STARTED);
    }

    // ─── finalizeCompletedTournaments ─────────────────────────────────────────

    @Test
    void finalizeCompletedTournaments_setsWinnerToTopRanked() {
        User leader = makeUser(1L, "alice");
        User member = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1),
                CompetitionStatus.FINISHED);

        GroupUserStatistics winner = GroupUserStatistics.builder()
                .id(1L).groupTournament(t).user(leader)
                .rankingPosition(1L).correctPredictions(10L).predictionCount(15L).accuracyPercent(66)
                .build();
        GroupUserStatistics second = GroupUserStatistics.builder()
                .id(2L).groupTournament(t).user(member)
                .rankingPosition(2L).correctPredictions(7L).predictionCount(15L).accuracyPercent(46)
                .build();

        when(groupTournamentRepository.findByStatus(CompetitionStatus.FINISHED))
                .thenReturn(List.of(t));
        when(groupUserStatisticsRepository.findByGroupTournamentOrderedByRanking(t))
                .thenReturn(List.of(winner, second));

        tournamentLifecycleService.finalizeCompletedTournaments();

        assertThat(t.getWinner()).isEqualTo(leader);
        verify(groupTournamentRepository).save(t);
    }

    @Test
    void finalizeCompletedTournaments_alreadyHasWinner_skips() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1),
                CompetitionStatus.FINISHED);
        t.setWinner(leader);

        when(groupTournamentRepository.findByStatus(CompetitionStatus.FINISHED))
                .thenReturn(List.of(t));

        tournamentLifecycleService.finalizeCompletedTournaments();

        verify(groupUserStatisticsRepository, never()).findByGroupTournamentOrderedByRanking(any());
        verify(groupTournamentRepository, never()).save(any());
    }

    @Test
    void finalizeCompletedTournaments_noStats_skips() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1),
                CompetitionStatus.FINISHED);

        when(groupTournamentRepository.findByStatus(CompetitionStatus.FINISHED))
                .thenReturn(List.of(t));
        when(groupUserStatisticsRepository.findByGroupTournamentOrderedByRanking(t))
                .thenReturn(List.of());

        tournamentLifecycleService.finalizeCompletedTournaments();

        assertThat(t.getWinner()).isNull();
        verify(groupTournamentRepository, never()).save(any());
    }

    // ─── deleteExpiredFinishedTournaments ─────────────────────────────────────

    @Test
    void deleteExpiredFinishedTournaments_deletesExpiredTournaments() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = makeTournament(1L, group,
                LocalDate.now().minusDays(35), LocalDate.now().minusDays(4),
                CompetitionStatus.FINISHED);

        Competition comp = Competition.builder().id(1L).country("ENG").name("PL").code("PL").build();
        GroupCompetition gc = GroupCompetition.builder().id(1L).groupTournament(t).competition(comp).build();

        when(groupTournamentRepository.findFinishedTournamentsOlderThan(any()))
                .thenReturn(List.of(t));
        when(groupCompetitionRepository.findByGroupTournament(t)).thenReturn(List.of(gc));
        doNothing().when(groupUserStatisticsRepository).deleteByGroupTournament(t);
        doNothing().when(groupCompetitionRepository).deleteByGroupTournament(t);
        doNothing().when(groupTournamentRepository).delete(t);

        tournamentLifecycleService.deleteExpiredFinishedTournaments();

        verify(groupTournamentRepository).delete(t);
        verify(competitionService).deleteIfUnused(comp);
    }

    @Test
    void deleteExpiredFinishedTournaments_noExpired_deletesNothing() {
        when(groupTournamentRepository.findFinishedTournamentsOlderThan(any()))
                .thenReturn(List.of());

        tournamentLifecycleService.deleteExpiredFinishedTournaments();

        verify(groupTournamentRepository, never()).delete(any(GroupTournament.class));
    }
}
