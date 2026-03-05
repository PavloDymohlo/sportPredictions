package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.dto.request.UserCompetitionListRequest;
import ua.dymohlo.sportPredictions.dto.response.CompetitionResponse;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserCompetition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCompetitionService {

    private final UserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionService competitionService;
    private final UserCompetitionRepository userCompetitionRepository;

    @Transactional(readOnly = true)
    public List<CompetitionResponse> getUserCompetitions(String userName) {
        log.info("🔍 Service: Looking for username: [{}]", userName);

        Optional<User> userOpt = userRepository.findByUserName(userName);
        if (userOpt.isEmpty()) {
            log.warn("❌ User not found: [{}]", userName);
            return Collections.emptyList();
        }

        User user = userOpt.get();
        log.info("✅ User found: {} (ID: {})", user.getUserName(), user.getId());

        List<UserCompetition> userCompetitions = userCompetitionRepository.findByUser(user);
        log.info("✅ Found {} user competitions", userCompetitions.size());

        return userCompetitions.stream()
                .map(uc -> {
                    Competition c = uc.getCompetition();
                    return CompetitionResponse.builder()
                            .id(c.getId())
                            .country(c.getCountry())
                            .name(c.getName())
                            .code(c.getCode())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void updateUserCompetitions(UserCompetitionListRequest request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserName()));

        List<Map<String, String>> competitionsList = request.getCompetitions();

        List<Competition> currentCompetitions = userCompetitionRepository.findByUser(user).stream()
                .map(UserCompetition::getCompetition)
                .toList();

        List<Competition> newCompetitions = new ArrayList<>();
        for (Map<String, String> compMap : competitionsList) {
            String country = compMap.get("country");
            String name = compMap.get("name");
            String code = compMap.get("code");

            if (country != null && name != null && code != null) {
                Competition comp = competitionService.findOrCreate(country, name, code);
                newCompetitions.add(comp);

                if (!currentCompetitions.contains(comp)) {
                    UserCompetition userComp = UserCompetition.builder()
                            .user(user)
                            .competition(comp)
                            .build();
                    userCompetitionRepository.save(userComp);
                }
            }
        }

        for (Competition oldComp : currentCompetitions) {
            if (!newCompetitions.contains(oldComp)) {
                userCompetitionRepository.deleteByUserAndCompetition(user, oldComp);

                if (competitionService.isCompetitionUnused(oldComp.getId())) {
                    competitionRepository.delete(oldComp);
                }
            }
        }
    }
}
