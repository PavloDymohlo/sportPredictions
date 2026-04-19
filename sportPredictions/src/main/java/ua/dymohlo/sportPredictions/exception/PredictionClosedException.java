package ua.dymohlo.sportPredictions.exception;

public class PredictionClosedException extends RuntimeException {
    public PredictionClosedException(String message) {
        super(message);
    }
}
