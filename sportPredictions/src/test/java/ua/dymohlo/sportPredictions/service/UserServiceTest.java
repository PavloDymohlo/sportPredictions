package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private UserGroupService userGroupService;

    @InjectMocks
    private UserService userService;

    private User makeUser(Long id, String name) {
        return User.builder().id(id).userName(name).language("en").build();
    }

    // ─── updateLanguage ───────────────────────────────────────────────────────

    @Test
    void updateLanguage_updatesAndSavesLanguage() {
        User user = makeUser(1L, "alice");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.updateLanguage("alice", "uk");

        assertThat(user.getLanguage()).isEqualTo("uk");
        verify(userRepository).save(user);
    }

    @Test
    void updateLanguage_userNotFound_throws() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateLanguage("ghost", "uk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    // ─── deleteUser ───────────────────────────────────────────────────────────

    @Test
    void deleteUser_noGroups_deletesUser() {
        User user = makeUser(1L, "alice");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
        when(userGroupRepository.findByGroupLeader(user)).thenReturn(List.of());

        userService.deleteUser("alice");

        verify(userRepository).delete(user);
        verify(userGroupService, never()).deleteGroupInternal(any());
    }

    @Test
    void deleteUser_leadsGroupWithOtherMembers_transfersLeadership() {
        User alice = makeUser(1L, "alice");
        User bob = makeUser(2L, "bob");
        User charlie = makeUser(3L, "charlie");
        UserGroup group = UserGroup.builder()
                .id(1L).groupName("G1").groupLeader(alice)
                .users(new ArrayList<>(List.of(alice, charlie, bob)))
                .build();

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(alice));
        when(userGroupRepository.findByGroupLeader(alice)).thenReturn(List.of(group));
        when(userGroupRepository.save(any())).thenReturn(group);

        userService.deleteUser("alice");

        // Leadership transferred to first alphabetically among remaining members
        assertThat(group.getGroupLeader().getUserName()).isEqualTo("bob");
        assertThat(group.getUsers()).doesNotContain(alice);
        verify(userRepository).delete(alice);
    }

    @Test
    void deleteUser_leadsGroupAlone_deletesGroup() {
        User alice = makeUser(1L, "alice");
        UserGroup group = UserGroup.builder()
                .id(1L).groupName("G1").groupLeader(alice)
                .users(new ArrayList<>(List.of(alice)))
                .build();

        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(alice));
        when(userGroupRepository.findByGroupLeader(alice)).thenReturn(List.of(group));

        userService.deleteUser("alice");

        verify(userGroupService).deleteGroupInternal(group);
        verify(userRepository).delete(alice);
    }

    @Test
    void deleteUser_userNotFound_throws() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser("ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
