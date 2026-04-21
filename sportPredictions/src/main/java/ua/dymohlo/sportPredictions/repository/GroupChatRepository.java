package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.dymohlo.sportPredictions.entity.GroupChat;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {

    Page<GroupChat> findByGroupName(String groupName, Pageable pageable);
}
