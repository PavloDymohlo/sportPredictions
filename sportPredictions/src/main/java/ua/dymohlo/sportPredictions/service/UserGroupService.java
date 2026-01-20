package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroup;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;

    public CreateUserGroup createUserGroup(CreateUserGroup createUserGroup) {
        User user = userRepository.findByUserName(createUserGroup.getUserGroupName())
                .orElseThrow(() -> new IllegalArgumentException("User with username "
                        + createUserGroup.getUserGroupName() + " not found!"));

        userGroupRepository.findByGroupName(createUserGroup.getUserGroupName())
                .ifPresent(group -> {
                    throw new IllegalArgumentException("Group with this name already exists.");
                });

        UserGroup group = UserGroup.builder()
                .groupName(createUserGroup.getUserGroupLeaderName())
                .groupLeader(user)
                .build();
        userGroupRepository.save(group);
        return createUserGroup;
    }

    /**
     * all groups for concrete user
     */
    public List<CreateUserGroup> findAllUserGroup(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User with username "
                        + username + " not found!"));

        List<UserGroup> groups = userGroupRepository.findAllGroupsForUser(user);

        return groups.stream()
                .map(g -> CreateUserGroup.builder()
                        .userGroupName(g.getGroupName())
                        .userGroupLeaderName(g.getGroupLeader().getUserName())
                        .build())
                .collect(Collectors.toList());
    }

    public CreateUserGroup findUserGroup(String userGroupName) {
       return userGroupRepository.findByGroupName(userGroupName)
               .map(g->CreateUserGroup.builder()
                       .userGroupName(g.getGroupName())
                       .userGroupLeaderName(g.getGroupLeader().getUserName()))
                .orElseThrow(() -> new IllegalArgumentException("Group with this name not found")).build();
    }
}
