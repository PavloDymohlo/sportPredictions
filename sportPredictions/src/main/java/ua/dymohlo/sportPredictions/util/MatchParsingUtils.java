package ua.dymohlo.sportPredictions.util;

import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchParsingUtils {

    private static final String OPEN_BRACKET = "(";
    private static final String UNAVAILABLE_SCORE = "н/в";
    private static final int INVALID_SCORE = -1;

    private MatchParsingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String extractTeamName(String teamEntry) {
        if (teamEntry == null) return "";
        int bracketIndex = teamEntry.indexOf(OPEN_BRACKET);
        String mainPart = bracketIndex > 0 ? teamEntry.substring(0, bracketIndex).trim() : teamEntry.trim();
        int lastSpace = mainPart.lastIndexOf(' ');
        if (lastSpace <= 0) return mainPart;
        return mainPart.substring(0, lastSpace).trim();
    }

    public static boolean teamsMatch(Object matchResult, Object userPrediction) {
        if (!(matchResult instanceof List) || !(userPrediction instanceof List)) return false;
        List<?> matchList = (List<?>) matchResult;
        List<?> predictionList = (List<?>) userPrediction;
        if (matchList.size() != 2 || predictionList.size() != 2) return false;
        String t1Result = extractTeamName(String.valueOf(matchList.get(0)));
        String t1Pred   = extractTeamName(String.valueOf(predictionList.get(0)));
        String t2Result = extractTeamName(String.valueOf(matchList.get(1)));
        String t2Pred   = extractTeamName(String.valueOf(predictionList.get(1)));
        return !t1Result.isEmpty() && t1Result.equalsIgnoreCase(t1Pred)
                && t2Result.equalsIgnoreCase(t2Pred);
    }

    public static boolean matchesAreEqual(Object matchResult, Object userPrediction) {
        if (!teamsMatch(matchResult, userPrediction)) {
            return false;
        }

        List<?> matchList = (List<?>) matchResult;
        List<?> predictionList = (List<?>) userPrediction;

        String team1Result = String.valueOf(matchList.get(0));
        String team1Prediction = String.valueOf(predictionList.get(0));
        String team2Result = String.valueOf(matchList.get(1));
        String team2Prediction = String.valueOf(predictionList.get(1));

        int team1ResultScore = extractScore(team1Result);
        int team1PredictionScore = extractScore(team1Prediction);
        int team2ResultScore = extractScore(team2Result);
        int team2PredictionScore = extractScore(team2Prediction);

        if (team1ResultScore == INVALID_SCORE || team2ResultScore == INVALID_SCORE ||
                team1PredictionScore == INVALID_SCORE || team2PredictionScore == INVALID_SCORE) {
            return false;
        }

        return team1ResultScore == team1PredictionScore &&
                team2ResultScore == team2PredictionScore;
    }

    public static int extractScore(String teamResult) {
        if (teamResult == null || UNAVAILABLE_SCORE.equals(teamResult)) {
            return INVALID_SCORE;
        }
        int bracketIndex = teamResult.indexOf(OPEN_BRACKET);
        String mainPart = bracketIndex > 0 ? teamResult.substring(0, bracketIndex).trim() : teamResult.trim();
        String[] parts = mainPart.split(" ");
        if (parts.length == 0) {
            return INVALID_SCORE;
        }
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return INVALID_SCORE;
        }
    }

    public static String extractScoreString(String teamWithScore) {
        if (teamWithScore == null) return "0";
        int bracketIndex = teamWithScore.indexOf(OPEN_BRACKET);
        String mainPart = bracketIndex > 0 ? teamWithScore.substring(0, bracketIndex).trim() : teamWithScore.trim();
        String[] parts = mainPart.split(" ");
        if (parts.length == 0) return "0";
        return parts[parts.length - 1];
    }

    public static String competitionKey(String country, String name) {
        return country + "|" + name;
    }

    public static List<Object> extractUserPredictions(PredictionRequest predictions) {
        List<Object> userPredictions = new ArrayList<>();

        if (predictions == null || predictions.getPredictions() == null) {
            return userPredictions;
        }

        for (Object prediction : predictions.getPredictions()) {
            if (prediction instanceof List) {
                userPredictions.add(prediction);
            }
        }

        return userPredictions;
    }

    public static List<Object> extractMatchesFromTournaments(List<Map<String, Object>> tournaments) {
        List<Object> allMatches = new ArrayList<>();

        for (Map<String, Object> tournament : tournaments) {
            if (tournament.containsKey("match")) {
                Object matchesObj = tournament.get("match");
                if (matchesObj instanceof List) {
                    allMatches.addAll((List<Object>) matchesObj);
                }
            }
        }

        return allMatches;
    }

    public static int calculateAccuracyPercent(long correct, long total) {
        if (total == 0) {
            return 0;
        }
        double percent = ((double) correct / total) * 100;
        return (int) Math.round(percent);
    }
}