package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private UserCompetitionRepository userCompetitionRepository;

    @Mock
    private GroupCompetitionRepository groupCompetitionRepository;

    @InjectMocks
    private CompetitionService competitionService;

    private Competition buildCompetition(Long id, String country, String name, String code) {
        return Competition.builder().id(id).country(country).name(name).code(code).build();
    }

    // ─── findOrCreate ─────────────────────────────────────────────────────────

    @Test
    void findOrCreate_existingCompetition_returnsExisting() {
        Competition existing = buildCompetition(1L, "England", "Premier League", "PL");
        when(competitionRepository.findByCountryAndName("England", "Premier League"))
                .thenReturn(Optional.of(existing));

        Competition result = competitionService.findOrCreate("England", "Premier League", "PL");

        assertThat(result).isSameAs(existing);
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void findOrCreate_notFound_createsAndReturnsNew() {
        when(competitionRepository.findByCountryAndName("Germany", "Bundesliga"))
                .thenReturn(Optional.empty());
        Competition created = buildCompetition(2L, "Germany", "Bundesliga", "BL1");
        when(competitionRepository.save(any(Competition.class))).thenReturn(created);

        Competition result = competitionService.findOrCreate("Germany", "Bundesliga", "BL1");

        assertThat(result.getCountry()).isEqualTo("Germany");
        assertThat(result.getName()).isEqualTo("Bundesliga");
        assertThat(result.getCode()).isEqualTo("BL1");
        verify(competitionRepository).save(any(Competition.class));
    }

    // ─── isCompetitionUnused ──────────────────────────────────────────────────

    @Test
    void isCompetitionUnused_notFound_returnsTrue() {
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(competitionService.isCompetitionUnused(99L)).isTrue();
    }

    @Test
    void isCompetitionUnused_usedByUser_returnsFalse() {
        Competition comp = buildCompetition(1L, "England", "PL", "PL");
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(comp));
        when(userCompetitionRepository.countByCompetition(comp)).thenReturn(2L);

        assertThat(competitionService.isCompetitionUnused(1L)).isFalse();
    }

    @Test
    void isCompetitionUnused_notUsedByAnyone_returnsTrue() {
        Competition comp = buildCompetition(1L, "England", "PL", "PL");
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(comp));
        when(userCompetitionRepository.countByCompetition(comp)).thenReturn(0L);

        assertThat(competitionService.isCompetitionUnused(1L)).isTrue();
    }

    // ─── deleteIfUnused ───────────────────────────────────────────────────────

    @Test
    void deleteIfUnused_usedByGroup_doesNotDelete() {
        Competition comp = buildCompetition(1L, "Spain", "LaLiga", "PD");
        when(groupCompetitionRepository.existsByCompetition(comp)).thenReturn(true);

        competitionService.deleteIfUnused(comp);

        verify(competitionRepository, never()).delete(any());
    }

    @Test
    void deleteIfUnused_usedByUser_doesNotDelete() {
        Competition comp = buildCompetition(1L, "Spain", "LaLiga", "PD");
        when(groupCompetitionRepository.existsByCompetition(comp)).thenReturn(false);
        when(userCompetitionRepository.countByCompetition(comp)).thenReturn(1L);

        competitionService.deleteIfUnused(comp);

        verify(competitionRepository, never()).delete(any());
    }

    @Test
    void deleteIfUnused_notUsedByAnyone_deletesCompetition() {
        Competition comp = buildCompetition(1L, "Spain", "LaLiga", "PD");
        when(groupCompetitionRepository.existsByCompetition(comp)).thenReturn(false);
        when(userCompetitionRepository.countByCompetition(comp)).thenReturn(0L);

        competitionService.deleteIfUnused(comp);

        verify(competitionRepository).delete(comp);
    }
}
