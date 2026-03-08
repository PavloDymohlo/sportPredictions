package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.dto.response.GroupRankingResponse;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.GroupUserStatistics;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.GroupUserStatisticsRepository;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupStatisticsService {

    private final UserGroupRepository userGroupRepository;
    private final GroupUserStatisticsRepository groupUserStatisticsRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final TournamentLifecycleService tournamentLifecycleService;
    private final TournamentStatisticsProcessor processor;

    public void calculateAllGroupsStatistics() {
        log.info("🎯 ========== STARTING GROUP STATISTICS CALCULATION ==========");

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate predictionTtlLimit = today.minusDays(3);

        log.info("📅 Processing up to: {}", yesterday);

        List<GroupTournament> activeTournaments = groupTournamentRepository.findByStatus(CompetitionStatus.ACTIVE);
        log.info("🏆 Found {} ACTIVE tournaments to process", activeTournaments.size());

        for (GroupTournament tournament : activeTournaments) {
            try {
                processor.process(tournament, yesterday, predictionTtlLimit);
            } catch (Exception e) {
                log.error("❌ Error processing tournament {}: {}", tournament.getId(), e.getMessage(), e);
            }
        }

        tournamentLifecycleService.updateTournamentStatuses();
        log.info("🎉 ========== GROUP STATISTICS CALCULATION COMPLETE ==========");
    }

    @Transactional(readOnly = true)
    public List<GroupRankingResponse> getGroupRanking(String groupName, Long tournamentId) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupTournament tournament = resolveTournament(group, tournamentId);
        if (tournament == null) {
            return buildEmptyRanking(group);
        }

        List<GroupUserStatistics> stats = groupUserStatisticsRepository
                .findByGroupTournamentOrderedByRanking(tournament);

        Set<Long> usersWithStats = stats.stream()
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());

        List<GroupRankingResponse> result = stats.stream()
                .map(s -> GroupRankingResponse.builder()
                        .rankingPosition(s.getRankingPosition())
                        .userName(s.getUser().getUserName())
                        .correctPredictions(s.getCorrectPredictions())
                        .predictionCount(s.getPredictionCount())
                        .accuracyPercent(s.getAccuracyPercent())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        if (group.getUsers() != null) {
            long nextPosition = result.isEmpty() ? 1 : result.size() + 1;
            for (User member : group.getUsers()) {
                if (!usersWithStats.contains(member.getId())) {
                    result.add(GroupRankingResponse.builder()
                            .rankingPosition(nextPosition++)
                            .userName(member.getUserName())
                            .correctPredictions(0L)
                            .predictionCount(0L)
                            .accuracyPercent(0)
                            .build());
                }
            }
        }

        return result;
    }

    private GroupTournament resolveTournament(UserGroup group, Long tournamentId) {
        if (tournamentId != null) {
            GroupTournament tournament = groupTournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
            if (!tournament.getUserGroup().getId().equals(group.getId())) {
                throw new IllegalArgumentException("Tournament does not belong to this group");
            }
            return tournament;
        }

        List<GroupTournament> tournaments = groupTournamentRepository.findByUserGroup(group);
        return tournaments.stream()
                .filter(t -> t.getStatus() == CompetitionStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> tournaments.stream()
                        .filter(t -> t.getStatus() == CompetitionStatus.FINISHED)
                        .max(Comparator.comparing(GroupTournament::getFinishDate))
                        .orElse(null));
    }

    private List<GroupRankingResponse> buildEmptyRanking(UserGroup group) {
        if (group.getUsers() == null) return List.of();
        long position = 1;
        List<GroupRankingResponse> result = new ArrayList<>();
        for (User member : group.getUsers()) {
            result.add(GroupRankingResponse.builder()
                    .rankingPosition(position++)
                    .userName(member.getUserName())
                    .correctPredictions(0L)
                    .predictionCount(0L)
                    .accuracyPercent(0)
                    .build());
        }
        return result;
    }
}
