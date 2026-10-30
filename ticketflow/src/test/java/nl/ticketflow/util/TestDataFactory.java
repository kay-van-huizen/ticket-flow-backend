package nl.ticketflow.util;

import nl.ticketflow.entity.Category;
import nl.ticketflow.entity.Comment;
import nl.ticketflow.entity.Label;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;
import nl.ticketflow.service.SlaPolicy;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TestDataFactory {

    // Same ids as the seed users in data.sql
    public static final UUID ADMIN_ID = UUID.fromString("f47bf1c8-96ca-44f2-8b35-cb41fa32a648");
    public static final UUID AGENT_ID = UUID.fromString("dbfc6416-851c-4af9-865d-b2a3029742f5");
    public static final UUID AGENT2_ID = UUID.fromString("4cad0239-a5e8-4e40-bb7b-556f74aecd95");
    public static final UUID REPORTER_ID = UUID.fromString("133e7a3f-47e9-4598-8b1c-6d6319799bfe");
    public static final UUID REPORTER2_ID = UUID.fromString("01d4d383-0bf5-42ac-829e-57135206aa21");

    public static final LocalDateTime TICKET_CREATED_AT = LocalDateTime.of(2026, 3, 1, 9, 0);

    private static final SlaPolicy SLA_POLICY = new SlaPolicy();

    private TestDataFactory() {
    }

    /** Creates a user with the given role and username and a random id. */
    public static UserAccount user(UserRole role, String username) {
        return user(UUID.randomUUID(), role, username);
    }

    /** Creates the seed reporter melder1. */
    public static UserAccount reporter() {
        return user(REPORTER_ID, UserRole.REPORTER, "melder1");
    }

    /** Creates the seed reporter melder2. */
    public static UserAccount reporter2() {
        return user(REPORTER2_ID, UserRole.REPORTER, "melder2");
    }

    /** Creates the seed agent agent1. */
    public static UserAccount agent() {
        return user(AGENT_ID, UserRole.AGENT, "agent1");
    }

    /** Creates the seed agent agent2. */
    public static UserAccount agent2() {
        return user(AGENT2_ID, UserRole.AGENT, "agent2");
    }

    /** Creates the seed admin. */
    public static UserAccount admin() {
        return user(ADMIN_ID, UserRole.ADMIN, "admin");
    }

    /** Creates an active category with the given title. */
    public static Category category(String title) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setTitle(title);
        category.setDescription(title + " omschrijving");
        return category;
    }

    /** Creates a label with the given name. */
    public static Label label(String name) {
        Label label = new Label();
        label.setId(UUID.randomUUID());
        label.setName(name);
        return label;
    }

    /** Creates an unassigned ticket without category, created at TICKET_CREATED_AT with its SLA due date. */
    public static Ticket ticket(UserAccount reporter, TicketStatus status, TicketPriority priority) {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTitle("Laptop start niet op");
        ticket.setDescription("Mijn laptop blijft hangen op het logo.");
        ticket.setReporter(reporter);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setCreatedAt(TICKET_CREATED_AT);
        ticket.setDueAt(SLA_POLICY.calculateDueAt(TICKET_CREATED_AT, priority));
        return ticket;
    }

    /** Creates a comment by the author on the ticket, public or internal. */
    public static Comment comment(Ticket ticket, UserAccount author, boolean internal) {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody("Heb je de laptop al opnieuw opgestart?");
        comment.setInternal(internal);
        comment.setCreatedAt(TICKET_CREATED_AT.plusHours(1));
        return comment;
    }

    private static UserAccount user(UUID id, UserRole role, String username) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Test " + username);
        user.setRole(role);
        return user;
    }
}
