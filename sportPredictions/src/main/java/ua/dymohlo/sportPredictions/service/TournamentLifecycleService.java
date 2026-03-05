package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.GroupCompetition;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.GroupUserStatistics;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.GroupUserStatisticsRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TournamentLifecycleService {

    private final GroupTournamentRepository groupTournamentRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupUserStatisticsRepository groupUserStatisticsRepository;
    private final CompetitionService competitionService;

    @Transactional
    public void updateTournamentStatuses() {
        LocalDate today = LocalDate.now();
        List<GroupTournament> allTournaments = groupTournamentRepository.findAll();

        for (GroupTournament tournament : allTournaments) {
            CompetitionStatus newStatus = determineTournamentStatus(tournament, today);
            if (tournament.getStatus() != newStatus) {
                log.info("📌 Tournament {} status changed: {} → {}",
                        tournament.getId(), tournament.getStatus(), newStatus);
                tournament.setStatus(newStatus);
                groupTournamentRepository.save(tournament);
            }
        }
    }

    private CompetitionStatus determineTournamentStatus(GroupTournament tournament, LocalDate today) {
        if (tournament.getStartDate() == null || tournament.getFinishDate() == null) {
            return CompetitionStatus.NOT_STARTED;
        }
        if (today.isBefore(tournament.getStartDate())) {
            return CompetitionStatus.NOT_STARTED;
        } else if (today.isAfter(tournament.getFinishDate())) {
            return CompetitionStatus.FINISHED;
        } else {
            return CompetitionStatus.ACTIVE;
        }
    }

    @Transactional
    public void finalizeCompletedTournaments() {
        log.info("🏆 ========== FINALIZING COMPLETED TOURNAMENTS ==========");

        List<GroupTournament> finishedTournaments = groupTournamentRepository.findByStatus(CompetitionStatus.FINISHED);
        log.info("🎖️ Found {} FINISHED tournaments", finishedTournaments.size());

        for (GroupTournament tournament : finishedTournaments) {
            if (tournament.getWinner() != null) {
                log.info("⏭️ Tournament {} already has winner: {}",
                        tournament.getId(), tournament.getWinner().getUserName());
                continue;
            }

            List<GroupUserStatistics> ranking = groupUserStatisticsRepository
                    .findByGroupTournamentOrderedByRanking(tournament);

            if (ranking.isEmpty()) {
                log.warn("⚠️ Tournament {} has no statistics", tournament.getId());
                continue;
            }

            GroupUserStatistics winnerStats = ranking.get(0);
            tournament.setWinner(winnerStats.getUser());
            groupTournamentRepository.save(tournament);

            log.info("🏆 Tournament {} WINNER: {} (correct: {}/{}, accuracy: {}%)",
                    tournament.getId(), winnerStats.getUser().getUserName(),
                    winnerStats.getCorrectPredictions(), winnerStats.getPredictionCount(),
                    winnerStats.getAccuracyPercent());
        }

        log.info("🎉 ========== FINALIZATION COMPLETE ==========");
    }

    @Transactional
    public void deleteExpiredFinishedTournaments() {
        log.info("🗑️ ========== DELETING EXPIRED FINISHED TOURNAMENTS ==========");

        LocalDate cutoffDate = LocalDate.now().minusDays(3);
        List<GroupTournament> expired = groupTournamentRepository.findFinishedTournamentsOlderThan(cutoffDate);
        log.info("🗑️ Found {} expired FINISHED tournaments (finished on or before {})", expired.size(), cutoffDate);

        for (GroupTournament tournament : expired) {
            deleteExpiredTournament(tournament);
        }

        log.info("🎉 ========== EXPIRED TOURNAMENTS CLEANUP COMPLETE ==========");
    }

    private void deleteExpiredTournament(GroupTournament tournament) {
        Set<Competition> competitionsToCheck = groupCompetitionRepository.findByGroupTournament(tournament).stream()
                .map(GroupCompetition::getCompetition)
                .collect(Collectors.toSet());

        groupUserStatisticsRepository.deleteByGroupTournament(tournament);
        groupCompetitionRepository.deleteByGroupTournament(tournament);
        groupTournamentRepository.delete(tournament);
        competitionsToCheck.forEach(competitionService::deleteIfUnused);

        log.info("🗑️ Deleted expired tournament id={} (finishDate: {})",
                tournament.getId(), tournament.getFinishDate());
    }
}
