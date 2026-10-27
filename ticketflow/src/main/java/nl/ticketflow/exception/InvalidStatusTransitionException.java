package nl.ticketflow.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    private final String from;
    private final String to;

    public InvalidStatusTransitionException(String from, String to) {
        super("Statusovergang van " + from + " naar " + to + " is niet toegestaan");
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
