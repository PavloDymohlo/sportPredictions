package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupService userGroupService;

    @Transactional
    public void updateLanguage(String username, String language) {
        var user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setLanguage(language);
        userRepository.save(user);
        log.info("Language updated for user '{}': {}", username, language);
    }

    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<UserGroup> ledGroups = userGroupRepository.findByGroupLeader(user);
        for (UserGroup group : ledGroups) {
            List<User> otherMembers = group.getUsers().stream()
                    .filter(u -> !u.getId().equals(user.getId()))
                    .sorted(Comparator.comparing(User::getUserName))
                    .toList();

            if (otherMembers.isEmpty()) {
                userGroupService.deleteGroupInternal(group);
            } else {
                group.setGroupLeader(otherMembers.get(0));
                group.getUsers().removeIf(u -> u.getId().equals(user.getId()));
                userGroupRepository.save(group);
                log.info("Leadership of group '{}' transferred to '{}'",
                        group.getGroupName(), otherMembers.get(0).getUserName());
            }
        }

        userRepository.delete(user);
        log.info("User '{}' deleted", username);
    }
}