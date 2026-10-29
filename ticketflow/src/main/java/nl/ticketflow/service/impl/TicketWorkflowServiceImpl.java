package nl.ticketflow.service.impl;

import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketEventType;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.entity.UserRole;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.InvalidStatusTransitionException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.TicketMapper;
import nl.ticketflow.repository.TicketRepository;
import nl.ticketflow.repository.UserAccountRepository;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.SlaPolicy;
import nl.ticketflow.service.TicketWorkflowService;
import nl.ticketflow.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketWorkflowServiceImpl implements TicketWorkflowService {

    private static final Set<UserRole> AGENT_OR_ADMIN = Set.of(UserRole.AGENT, UserRole.ADMIN);
    private static final Set<UserRole> ALL_ROLES = Set.of(UserRole.REPORTER, UserRole.AGENT, UserRole.ADMIN);
    private static final Duration REOPEN_WINDOW = Duration.ofDays(7);

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final TicketEventRecorder ticketEventRecorder;
    private final UserAccountRepository userAccountRepository;
    private final SlaPolicy slaPolicy;

    public TicketWorkflowServiceImpl(TicketRepository ticketRepository, TicketMapper ticketMapper,
                                     CurrentUserService currentUserService, UserService userService,
                                     TicketEventRecorder ticketEventRecorder,
                                     UserAccountRepository userAccountRepository, SlaPolicy slaPolicy) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.ticketEventRecorder = ticketEventRecorder;
        this.userAccountRepository = userAccountRepository;
        this.slaPolicy = slaPolicy;
    }

    @Override
    @Transactional
    public TicketResponse changeStatus(UUID ticketId, TicketStatus newStatus) {
        // Creates the UserAccount on first login, so changedBy of the event always exists in the database
        UserAccount currentUser = userService.getOrCreateCurrentUser();
        UserRole role = currentUserService.getCurrentRole();

        Ticket ticket = ticketRepository.findByIdAndDeletedAtIsNull(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket niet gevonden"));

        if (role == UserRole.REPORTER && !ticket.getReporter().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("U heeft geen toegang tot dit ticket");
        }

        TicketStatus currentStatus = ticket.getStatus();
        Set<UserRole> allowedRoles = allowedRoles(currentStatus, newStatus);
        if (currentStatus == TicketStatus.CLOSED || allowedRoles.isEmpty()) {
            throw new InvalidStatusTransitionException(currentStatus.name(), newStatus.name());
        }
        if (!allowedRoles.contains(role)) {
            throw new ForbiddenException("U heeft geen rechten om de status van dit ticket te wijzigen");
        }

        if (currentStatus == TicketStatus.OPEN && newStatus == TicketStatus.IN_PROGRESS
                && ticket.getAssignee() == null) {
            throw new BusinessRuleException(
                    "Een ticket moet eerst worden toegewezen voordat het in behandeling kan worden genomen");
        }
        boolean reopen = currentStatus == TicketStatus.RESOLVED && newStatus == TicketStatus.OPEN;
        if (reopen && !withinReopenWindow(ticket.getResolvedAt())) {
            throw new BusinessRuleException("Een ticket kan alleen binnen 7 dagen na oplossing worden heropend");
        }

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(now());
        }
        if (reopen) {
            ticket.setResolvedAt(null);
        }
        // A reopen is recorded only as REOPENED, not also as STATUS_CHANGED
        TicketEventType eventType = reopen ? TicketEventType.REOPENED : TicketEventType.STATUS_CHANGED;
        ticketEventRecorder.record(ticket, eventType, currentStatus.name(), newStatus.name(), currentUser);
        // Flush so @PreUpdate sets updatedAt before the response is built
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    @Transactional
    public TicketResponse changePriority(UUID ticketId, TicketPriority newPriority) {
        UserAccount currentUser = userService.getOrCreateCurrentUser();
        Ticket ticket = findTicketOrThrow(ticketId);
        checkAgentOrAdmin("Alleen agents en beheerders mogen de prioriteit wijzigen");
        checkNotClosed(ticket);

        TicketPriority currentPriority = ticket.getPriority();
        if (currentPriority == newPriority) {
            return ticketMapper.toResponse(ticket);
        }

        ticket.setPriority(newPriority);
        // The SLA counts from creation, not from the moment the priority changes
        ticket.setDueAt(slaPolicy.calculateDueAt(ticket.getCreatedAt(), newPriority));
        ticketEventRecorder.record(ticket, TicketEventType.PRIORITY_CHANGED, currentPriority.name(),
                newPriority.name(), currentUser);
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    @Transactional
    public TicketResponse assign(UUID ticketId, UUID assigneeId) {
        UserAccount currentUser = userService.getOrCreateCurrentUser();
        Ticket ticket = findTicketOrThrow(ticketId);
        checkAgentOrAdmin("Alleen agents en beheerders mogen tickets toewijzen");
        checkNotClosed(ticket);

        UserAccount assignee = userAccountRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Gebruiker niet gevonden"));
        // Same role source as GET, the role stored on the UserAccount, synced from the token
        if (!AGENT_OR_ADMIN.contains(assignee.getRole())) {
            throw new BusinessRuleException("Een ticket kan alleen worden toegewezen aan een agent of beheerder");
        }

        UserAccount currentAssignee = ticket.getAssignee();
        if (currentAssignee != null && currentAssignee.getId().equals(assignee.getId())) {
            return ticketMapper.toResponse(ticket);
        }

        ticket.setAssignee(assignee);
        String oldValue = currentAssignee != null ? currentAssignee.getUsername() : null;
        ticketEventRecorder.record(ticket, TicketEventType.ASSIGNED, oldValue, assignee.getUsername(), currentUser);
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket niet gevonden"));
    }

    private void checkAgentOrAdmin(String message) {
        if (!AGENT_OR_ADMIN.contains(currentUserService.getCurrentRole())) {
            throw new ForbiddenException(message);
        }
    }

    private void checkNotClosed(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Een gesloten ticket kan niet meer worden gewijzigd");
        }
    }

    // Roles allowed to move a ticket from one status to another; an empty set means the transition is undefined
    private Set<UserRole> allowedRoles(TicketStatus from, TicketStatus to) {
        if (from == TicketStatus.OPEN && to == TicketStatus.IN_PROGRESS) {
            return AGENT_OR_ADMIN;
        }
        if (from == TicketStatus.IN_PROGRESS && to == TicketStatus.WAITING_FOR_REPORTER) {
            return AGENT_OR_ADMIN;
        }
        if (from == TicketStatus.WAITING_FOR_REPORTER && to == TicketStatus.IN_PROGRESS) {
            return AGENT_OR_ADMIN;
        }
        if (from == TicketStatus.IN_PROGRESS && to == TicketStatus.RESOLVED) {
            return AGENT_OR_ADMIN;
        }
        // A REPORTER is already limited to their own ticket by the ownership check
        if (from == TicketStatus.RESOLVED && to == TicketStatus.CLOSED) {
            return ALL_ROLES;
        }
        if (from == TicketStatus.RESOLVED && to == TicketStatus.OPEN) {
            return Set.of(UserRole.REPORTER);
        }
        if (from == TicketStatus.OPEN && to == TicketStatus.CLOSED) {
            return Set.of(UserRole.ADMIN);
        }
        if (from == TicketStatus.IN_PROGRESS && to == TicketStatus.CLOSED) {
            return Set.of(UserRole.ADMIN);
        }
        if (from == TicketStatus.WAITING_FOR_REPORTER && to == TicketStatus.CLOSED) {
            return Set.of(UserRole.ADMIN);
        }
        return Set.of();
    }

    // Exactly REOPEN_WINDOW after resolving is still allowed
    private boolean withinReopenWindow(LocalDateTime resolvedAt) {
        return resolvedAt != null && !now().isAfter(resolvedAt.plus(REOPEN_WINDOW));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
