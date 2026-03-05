package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;


@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final UserCompetitionRepository userCompetitionRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;

    @Transactional
    public Competition findOrCreate(String country, String name, String code) {
        return competitionRepository.findByCountryAndName(country, name)
                .orElseGet(() -> competitionRepository.save(
                        Competition.builder().country(country).name(name).code(code).build()));
    }

    public boolean isCompetitionUnused(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .map(competition -> userCompetitionRepository.countByCompetition(competition) == 0)
                .orElse(true);
    }

    @Transactional
    public void deleteIfUnused(Competition competition) {
        if (!groupCompetitionRepository.existsByCompetition(competition)
                && userCompetitionRepository.countByCompetition(competition) == 0) {
            competitionRepository.delete(competition);
            log.info("Deleted unused competition '{}' ({})", competition.getName(), competition.getCode());
        }
    }
}