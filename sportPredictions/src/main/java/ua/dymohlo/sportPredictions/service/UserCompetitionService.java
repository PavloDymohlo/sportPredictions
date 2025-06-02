package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.dto.request.UserCompetitionListRequest;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserCompetition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCompetitionService {

    private final UserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionService competitionService;
    private final UserCompetitionRepository userCompetitionRepository;

//    public List<Competition> getUserCompetitions(String userName) {
//        User user = userRepository.findByUserName(userName)
//                .orElseThrow(() -> new RuntimeException("User not found: " + userName));
//
//        return userCompetitionRepository.findByUser(user).stream()
//                .map(UserCompetition::getCompetition)
//                .collect(Collectors.toList());
//    }
    public List<Competition> getUserCompetitions(String userName) {
        log.info("username: "+userName);
    return Optional.ofNullable(userName)
            .flatMap(userRepository::findByUserName)
            .map(user -> userCompetitionRepository.findByUser(user)
                    .stream()
                    .map(UserCompetition::getCompetition)
                    .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
}

    @Transactional
    public void addCompetitionToUser(String userName, String country, String name) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        Competition competition = competitionService.findOrCreate(country, name);

        if (userCompetitionRepository.existsByUserAndCompetition(user, competition)) {
            return;
        }

        UserCompetition userCompetition = UserCompetition.builder()
                .user(user)
                .competition(competition)
                .build();

        userCompetitionRepository.save(userCompetition);
    }

    @Transactional
    public void removeCompetitionFromUser(String userName, long competitionId) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("Competition not found: " + competitionId));

        userCompetitionRepository.deleteByUserAndCompetition(user, competition);

        if (!competitionService.isCompetitionInUse(competitionId)) {
            competitionRepository.delete(competition);
        }
    }

    @Transactional
    public void updateUserCompetitions(UserCompetitionListRequest request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserName()));

        List<Map<String, String>> competitionsList = new ArrayList<>();

        for (Object obj : request.getCompetitions()) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> compMap = (Map<String, String>) obj;
                competitionsList.add(compMap);
            }
        }

        List<Competition> currentCompetitions = userCompetitionRepository.findByUser(user).stream()
                .map(UserCompetition::getCompetition)
                .collect(Collectors.toList());

        List<Competition> newCompetitions = new ArrayList<>();
        for (Map<String, String> compMap : competitionsList) {
            for (Map.Entry<String, String> entry : compMap.entrySet()) {
                String country = entry.getKey();
                String name = entry.getValue();
                Competition comp = competitionService.findOrCreate(country, name);
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

                if (!competitionService.isCompetitionInUse(oldComp.getId())) {
                    competitionRepository.delete(oldComp);
                }
            }
        }
    }
}


