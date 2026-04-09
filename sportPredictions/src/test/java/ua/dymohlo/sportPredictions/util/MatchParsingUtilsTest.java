package ua.dymohlo.sportPredictions.util;

import org.junit.jupiter.api.Test;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchParsingUtilsTest {

    // ─── extractTeamName ──────────────────────────────────────────────────────

    @Test
    void extractTeamName_nullReturnsEmpty() {
        assertThat(MatchParsingUtils.extractTeamName(null)).isEmpty();
    }

    @Test
    void extractTeamName_plainTeamWithScore() {
        // "Arsenal 2" → "Arsenal"
        assertThat(MatchParsingUtils.extractTeamName("Arsenal 2")).isEqualTo("Arsenal");
    }

    @Test
    void extractTeamName_teamWithBracketSuffix() {
        // "Real Madrid 3 (pen)" → last word before bracket is score → "Real Madrid"
        assertThat(MatchParsingUtils.extractTeamName("Real Madrid 3 (pen)")).isEqualTo("Real Madrid");
    }

    @Test
    void extractTeamName_singleWord() {
        assertThat(MatchParsingUtils.extractTeamName("Arsenal")).isEqualTo("Arsenal");
    }

    @Test
    void extractTeamName_trimOnly() {
        assertThat(MatchParsingUtils.extractTeamName("  Chelsea 1  ")).isEqualTo("Chelsea");
    }

    @Test
    void extractTeamName_multiWordName() {
        assertThat(MatchParsingUtils.extractTeamName("Manchester United 0")).isEqualTo("Manchester United");
    }

    // ─── extractScore ─────────────────────────────────────────────────────────

    @Test
    void extractScore_nullReturnsInvalid() {
        assertThat(MatchParsingUtils.extractScore(null)).isEqualTo(-1);
    }

    @Test
    void extractScore_unavailableScoreReturnsInvalid() {
        assertThat(MatchParsingUtils.extractScore("н/в")).isEqualTo(-1);
    }

    @Test
    void extractScore_normalEntry() {
        assertThat(MatchParsingUtils.extractScore("Arsenal 2")).isEqualTo(2);
    }

    @Test
    void extractScore_withBracketSuffix() {
        assertThat(MatchParsingUtils.extractScore("Barcelona 1 (aet)")).isEqualTo(1);
    }

    @Test
    void extractScore_noScorePartReturnsInvalid() {
        assertThat(MatchParsingUtils.extractScore("Arsenal")).isEqualTo(-1);
    }

    @Test
    void extractScore_zeroScore() {
        assertThat(MatchParsingUtils.extractScore("Juventus 0")).isEqualTo(0);
    }

    // ─── teamsMatch ───────────────────────────────────────────────────────────

    @Test
    void teamsMatch_sameTeams_returnsTrue() {
        List<String> matchResult = List.of("Arsenal 2", "Chelsea 1");
        List<String> prediction = List.of("Arsenal 3", "Chelsea 0");
        assertThat(MatchParsingUtils.teamsMatch(matchResult, prediction)).isTrue();
    }

    @Test
    void teamsMatch_differentTeams_returnsFalse() {
        List<String> matchResult = List.of("Arsenal 2", "Chelsea 1");
        List<String> prediction = List.of("Liverpool 1", "Chelsea 0");
        assertThat(MatchParsingUtils.teamsMatch(matchResult, prediction)).isFalse();
    }

    @Test
    void teamsMatch_notListArgs_returnsFalse() {
        assertThat(MatchParsingUtils.teamsMatch("not a list", List.of("A", "B"))).isFalse();
    }

    @Test
    void teamsMatch_wrongSize_returnsFalse() {
        List<String> single = List.of("Arsenal 2");
        List<String> valid = List.of("Arsenal 2", "Chelsea 1");
        assertThat(MatchParsingUtils.teamsMatch(single, valid)).isFalse();
    }

    @Test
    void teamsMatch_caseInsensitive() {
        List<String> matchResult = List.of("arsenal 2", "CHELSEA 1");
        List<String> prediction = List.of("Arsenal 3", "Chelsea 0");
        assertThat(MatchParsingUtils.teamsMatch(matchResult, prediction)).isTrue();
    }

    // ─── matchesAreEqual ──────────────────────────────────────────────────────

    @Test
    void matchesAreEqual_correctPrediction_returnsTrue() {
        List<String> matchResult = List.of("Arsenal 2", "Chelsea 1");
        List<String> prediction = List.of("Arsenal 2", "Chelsea 1");
        assertThat(MatchParsingUtils.matchesAreEqual(matchResult, prediction)).isTrue();
    }

    @Test
    void matchesAreEqual_wrongScore_returnsFalse() {
        List<String> matchResult = List.of("Arsenal 2", "Chelsea 1");
        List<String> prediction = List.of("Arsenal 1", "Chelsea 1");
        assertThat(MatchParsingUtils.matchesAreEqual(matchResult, prediction)).isFalse();
    }

    @Test
    void matchesAreEqual_unavailableScore_returnsFalse() {
        List<String> matchResult = List.of("Arsenal н/в", "Chelsea н/в");
        List<String> prediction = List.of("Arsenal 0", "Chelsea 0");
        assertThat(MatchParsingUtils.matchesAreEqual(matchResult, prediction)).isFalse();
    }

    @Test
    void matchesAreEqual_differentTeams_returnsFalse() {
        List<String> matchResult = List.of("Arsenal 2", "Chelsea 1");
        List<String> prediction = List.of("Liverpool 2", "Chelsea 1");
        assertThat(MatchParsingUtils.matchesAreEqual(matchResult, prediction)).isFalse();
    }

    // ─── competitionKey ───────────────────────────────────────────────────────

    @Test
    void competitionKey_buildsCorrectKey() {
        assertThat(MatchParsingUtils.competitionKey("England", "Premier League"))
                .isEqualTo("England|Premier League");
    }

    // ─── calculateAccuracyPercent ─────────────────────────────────────────────

    @Test
    void calculateAccuracyPercent_zeroTotal_returnsZero() {
        assertThat(MatchParsingUtils.calculateAccuracyPercent(0, 0)).isEqualTo(0);
    }

    @Test
    void calculateAccuracyPercent_allCorrect_returns100() {
        assertThat(MatchParsingUtils.calculateAccuracyPercent(5, 5)).isEqualTo(100);
    }

    @Test
    void calculateAccuracyPercent_halfCorrect_returns50() {
        assertThat(MatchParsingUtils.calculateAccuracyPercent(5, 10)).isEqualTo(50);
    }

    @Test
    void calculateAccuracyPercent_rounded() {
        // 2/3 = 66.666... → rounds to 67
        assertThat(MatchParsingUtils.calculateAccuracyPercent(2, 3)).isEqualTo(67);
    }

    // ─── extractUserPredictions ───────────────────────────────────────────────

    @Test
    void extractUserPredictions_nullRequest_returnsEmptyList() {
        assertThat(MatchParsingUtils.extractUserPredictions(null)).isEmpty();
    }

    @Test
    void extractUserPredictions_filtersOnlyLists() {
        List<Object> mixed = new ArrayList<>();
        mixed.add(List.of("Arsenal 2", "Chelsea 1"));
        mixed.add("not a list");
        mixed.add(List.of("Real 1", "Barca 0"));

        PredictionRequest req = PredictionRequest.builder()
                .userName("user1")
                .matchDate("2025-01-01")
                .predictions(mixed)
                .build();

        List<Object> result = MatchParsingUtils.extractUserPredictions(req);
        assertThat(result).hasSize(2);
    }

    @Test
    void extractUserPredictions_emptyPredictions_returnsEmptyList() {
        PredictionRequest req = PredictionRequest.builder()
                .userName("user1")
                .matchDate("2025-01-01")
                .predictions(List.of())
                .build();

        assertThat(MatchParsingUtils.extractUserPredictions(req)).isEmpty();
    }

    // ─── extractMatchesFromTournaments ────────────────────────────────────────

    @Test
    void extractMatchesFromTournaments_collectsMatchesFromAllTournaments() {
        List<Object> matches1 = List.of(List.of("Arsenal 2", "Chelsea 1"));
        List<Object> matches2 = List.of(List.of("Real 3", "Barca 2"), List.of("PSG 1", "Lyon 0"));

        Map<String, Object> t1 = new HashMap<>();
        t1.put("match", matches1);
        Map<String, Object> t2 = new HashMap<>();
        t2.put("match", matches2);

        List<Object> result = MatchParsingUtils.extractMatchesFromTournaments(List.of(t1, t2));
        assertThat(result).hasSize(3);
    }

    @Test
    void extractMatchesFromTournaments_noMatchKey_returnsEmpty() {
        Map<String, Object> t1 = new HashMap<>();
        t1.put("tournament", "Bundesliga");

        assertThat(MatchParsingUtils.extractMatchesFromTournaments(List.of(t1))).isEmpty();
    }

    // ─── utility class cannot be instantiated ────────────────────────────────

    @Test
    void constructor_throwsUnsupportedOperationException() throws Exception {
        var constructor = MatchParsingUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
