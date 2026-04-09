package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.dto.response.GroupRankingResponse;
import ua.dymohlo.sportPredictions.entity.*;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupStatisticsServiceTest {

    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private GroupUserStatisticsRepository groupUserStatisticsRepository;
    @Mock
    private GroupTournamentRepository groupTournamentRepository;
    @Mock
    private TournamentLifecycleService tournamentLifecycleService;
    @Mock
    private TournamentStatisticsProcessor processor;

    @InjectMocks
    private GroupStatisticsService groupStatisticsService;

    private User makeUser(Long id, String name) {
        return User.builder().id(id).userName(name).build();
    }

    private UserGroup makeGroup(Long id, String name, User leader, List<User> members) {
        return UserGroup.builder()
                .id(id).groupName(name).groupLeader(leader)
                .users(new ArrayList<>(members))
                .build();
    }

    private GroupTournament makeTournament(Long id, UserGroup group, CompetitionStatus status) {
        return GroupTournament.builder()
                .id(id).userGroup(group)
                .startDate(LocalDate.now().minusDays(10))
                .finishDate(LocalDate.now().plusDays(10))
                .status(status)
                .build();
    }

    // ─── calculateAllGroupsStatistics ─────────────────────────────────────────

    @Test
    void calculateAllGroupsStatistics_callsProcessorForEachActiveTournament() {
        User u = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", u, List.of(u));
        GroupTournament t1 = makeTournament(1L, group, CompetitionStatus.ACTIVE);
        GroupTournament t2 = makeTournament(2L, group, CompetitionStatus.ACTIVE);

        when(groupTournamentRepository.findByStatus(CompetitionStatus.ACTIVE))
                .thenReturn(List.of(t1, t2));

        groupStatisticsService.calculateAllGroupsStatistics();

        verify(processor, times(2)).process(any(), any(), any());
        verify(tournamentLifecycleService).updateTournamentStatuses();
    }

    @Test
    void calculateAllGroupsStatistics_processorThrows_continuesOtherTournaments() {
        User u = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", u, List.of(u));
        GroupTournament t1 = makeTournament(1L, group, CompetitionStatus.ACTIVE);
        GroupTournament t2 = makeTournament(2L, group, CompetitionStatus.ACTIVE);

        when(groupTournamentRepository.findByStatus(CompetitionStatus.ACTIVE))
                .thenReturn(List.of(t1, t2));
        doThrow(new RuntimeException("fail")).when(processor).process(eq(t1), any(), any());

        groupStatisticsService.calculateAllGroupsStatistics();

        // Should still process t2 despite t1 failing
        verify(processor).process(eq(t2), any(), any());
        verify(tournamentLifecycleService).updateTournamentStatuses();
    }

    @Test
    void calculateAllGroupsStatistics_noActiveTournaments_doesNotCallProcessor() {
        when(groupTournamentRepository.findByStatus(CompetitionStatus.ACTIVE))
                .thenReturn(List.of());

        groupStatisticsService.calculateAllGroupsStatistics();

        verify(processor, never()).process(any(), any(), any());
        verify(tournamentLifecycleService).updateTournamentStatuses();
    }

    // ─── getGroupRanking — with specific tournamentId ─────────────────────────

    @Test
    void getGroupRanking_withTournamentId_returnsStatsAndEmptyMembers() {
        User leader = makeUser(1L, "alice");
        User member = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader, List.of(leader, member));
        GroupTournament tournament = makeTournament(1L, group, CompetitionStatus.ACTIVE);

        GroupUserStatistics statsAlice = GroupUserStatistics.builder()
                .id(1L).groupTournament(tournament).user(leader)
                .rankingPosition(1L).correctPredictions(5L).predictionCount(10L).accuracyPercent(50)
                .build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(groupTournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(groupUserStatisticsRepository.findByGroupTournamentOrderedByRanking(tournament))
                .thenReturn(List.of(statsAlice));

        List<GroupRankingResponse> result = groupStatisticsService.getGroupRanking("G1", 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserName()).isEqualTo("alice");
        assertThat(result.get(0).getCorrectPredictions()).isEqualTo(5L);
        // bob has no stats → added at the end with zeros
        assertThat(result.get(1).getUserName()).isEqualTo("bob");
        assertThat(result.get(1).getCorrectPredictions()).isEqualTo(0L);
    }

    @Test
    void getGroupRanking_tournamentBelongsToDifferentGroup_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group1 = makeGroup(1L, "G1", leader, List.of(leader));
        UserGroup group2 = makeGroup(2L, "G2", leader, List.of(leader));
        GroupTournament tournament = makeTournament(1L, group2, CompetitionStatus.ACTIVE);

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group1));
        when(groupTournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> groupStatisticsService.getGroupRanking("G1", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    // ─── getGroupRanking — without tournamentId ───────────────────────────────

    @Test
    void getGroupRanking_noTournamentId_picksActiveTournamentFirst() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader, List.of(leader));
        GroupTournament active = makeTournament(1L, group, CompetitionStatus.ACTIVE);
        GroupTournament finished = GroupTournament.builder()
                .id(2L).userGroup(group)
                .startDate(LocalDate.now().minusDays(30))
                .finishDate(LocalDate.now().minusDays(5))
                .status(CompetitionStatus.FINISHED)
                .build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(groupTournamentRepository.findByUserGroup(group)).thenReturn(List.of(active, finished));
        when(groupUserStatisticsRepository.findByGroupTournamentOrderedByRanking(active))
                .thenReturn(List.of());

        List<GroupRankingResponse> result = groupStatisticsService.getGroupRanking("G1", null);

        // Active tournament chosen — no stats yet, but alice is still added as empty
        verify(groupUserStatisticsRepository).findByGroupTournamentOrderedByRanking(active);
    }

    @Test
    void getGroupRanking_noTournaments_returnsEmptyRankingWithMembers() {
        User leader = makeUser(1L, "alice");
        User member = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader, List.of(leader, member));

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(groupTournamentRepository.findByUserGroup(group)).thenReturn(List.of());

        List<GroupRankingResponse> result = groupStatisticsService.getGroupRanking("G1", null);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getCorrectPredictions() == 0L);
    }

    @Test
    void getGroupRanking_groupNotFound_throws() {
        when(userGroupRepository.findByGroupName("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupStatisticsService.getGroupRanking("X", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group not found");
    }
}
