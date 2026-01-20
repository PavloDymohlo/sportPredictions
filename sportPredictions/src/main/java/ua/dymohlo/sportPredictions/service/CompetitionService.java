package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final UserCompetitionRepository userCompetitionRepository;

    public List<Competition> findAll() {
        return competitionRepository.findAll();
    }

    public Optional<Competition> findById(Long id) {
        return competitionRepository.findById(id);
    }

    @Transactional
    public Competition findOrCreate(String country, String name, String code) {
        return competitionRepository.findByCountryAndName(country, name)
                .orElseGet(() -> {
                    Competition newCompetition = Competition.builder()
                            .country(country)
                            .name(name)
                            .code(code)
                            .build();
                    return competitionRepository.save(newCompetition);
                });
    }

    public boolean isCompetitionInUse(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .map(competition -> userCompetitionRepository.countByCompetition(competition) > 0)
                .orElse(false);
    }

    @Transactional
    public void deleteById(Long id) {
        competitionRepository.deleteById(id);
    }

    @Transactional
    public boolean deleteIfNotInUse(Long id) {
        if (!isCompetitionInUse(id)) {
            deleteById(id);
            return true;
        }
        return false;
    }
}