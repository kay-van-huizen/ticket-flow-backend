package nl.ticketflow.service.impl;

import nl.ticketflow.dto.request.CreateTicketRequest;
import nl.ticketflow.dto.request.UpdateTicketRequest;
import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.dto.response.TicketStatsResponse;
import nl.ticketflow.entity.Category;
import nl.ticketflow.entity.CategoryStatus;
import nl.ticketflow.entity.Label;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketEventType;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.TicketMapper;
import nl.ticketflow.repository.CategoryRepository;
import nl.ticketflow.repository.LabelRepository;
import nl.ticketflow.repository.TicketRepository;
import nl.ticketflow.repository.TicketSpecifications;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.SlaPolicy;
import nl.ticketflow.service.TicketService;
import nl.ticketflow.service.UserService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    // Tickets in these statuses are never overdue and do not count as unassigned work
    private static final Set<TicketStatus> FINISHED_STATUSES = EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final SlaPolicy slaPolicy;
    private final TicketEventRecorder ticketEventRecorder;
    private final LabelRepository labelRepository;

    public TicketServiceImpl(TicketRepository ticketRepository, CategoryRepository categoryRepository,
                             TicketMapper ticketMapper, CurrentUserService currentUserService,
                             UserService userService, SlaPolicy slaPolicy,
                             TicketEventRecorder ticketEventRecorder, LabelRepository labelRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.ticketMapper = ticketMapper;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.slaPolicy = slaPolicy;
        this.ticketEventRecorder = ticketEventRecorder;
        this.labelRepository = labelRepository;
    }

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        // Creates the UserAccount on first login, so the reporter always exists in the database
        UserAccount reporter = userService.getOrCreateCurrentUser();
        Category category = resolveCategory(request.categoryId(), null);

        Ticket ticket = ticketMapper.toEntity(request, reporter, category);
        // New tickets always start as NORMAL and OPEN; dueAt follows with the SLA policy
        ticket.setPriority(TicketPriority.NORMAL);
        ticket.setStatus(TicketStatus.OPEN);
        Ticket saved = ticketRepository.save(ticket);
        // createdAt is set by @PrePersist during save; dirty checking persists dueAt on commit
        saved.setDueAt(slaPolicy.calculateDueAt(saved.getCreatedAt(), saved.getPriority()));
        ticketEventRecorder.record(saved, TicketEventType.CREATED, null, saved.getStatus().name(), reporter);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        checkAccess(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(TicketStatus status, TicketPriority priority, UUID categoryId,
                                            UUID assigneeId, UUID labelId) {
        List<Specification<Ticket>> specs = new ArrayList<>();
        specs.add(TicketSpecifications.notDeleted());
        if (status != null) {
            specs.add(TicketSpecifications.hasStatus(status));
        }
        if (priority != null) {
            specs.add(TicketSpecifications.hasPriority(priority));
        }
        if (categoryId != null) {
            specs.add(TicketSpecifications.hasCategory(categoryId));
        }
        if (assigneeId != null) {
            specs.add(TicketSpecifications.hasAssignee(assigneeId));
        }
        if (labelId != null) {
            specs.add(TicketSpecifications.hasLabel(labelId));
        }
        if (!currentUserService.isAgentOrAdmin()) {
            specs.add(TicketSpecifications.hasReporter(currentUserService.getCurrentUserId()));
        }

        // No pagination yet: this is a documented limitation of the project
        return ticketRepository.findAll(Specification.allOf(specs), Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getOverdueTickets() {
        return ticketRepository.findOverdue(LocalDateTime.now(), FINISHED_STATUSES).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatsResponse getStats() {
        // EnumMap keeps the declaration order in the JSON; missing enum values stay 0
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : ticketRepository.countPerStatus()) {
            byStatus.put((TicketStatus) row[0], (Long) row[1]);
        }

        Map<TicketPriority, Long> byPriority = new EnumMap<>(TicketPriority.class);
        for (TicketPriority priority : TicketPriority.values()) {
            byPriority.put(priority, 0L);
        }
        for (Object[] row : ticketRepository.countPerPriority()) {
            byPriority.put((TicketPriority) row[0], (Long) row[1]);
        }

        return new TicketStatsResponse(
                ticketRepository.countByDeletedAtIsNull(),
                byStatus,
                byPriority,
                ticketRepository.countOverdue(LocalDateTime.now(), FINISHED_STATUSES),
                ticketRepository.countByAssigneeIsNullAndStatusNotInAndDeletedAtIsNull(FINISHED_STATUSES)
        );
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(UUID id, UpdateTicketRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        checkAccess(ticket);
        if (!currentUserService.isAgentOrAdmin() && ticket.getStatus() != TicketStatus.OPEN) {
            throw new ForbiddenException("Je kunt een ticket alleen wijzigen zolang de status Open is");
        }

        Category category = resolveCategory(request.categoryId(), ticket.getCategory());
        ticketMapper.updateEntity(ticket, request, category);
        // Flush so @PreUpdate sets updatedAt before the response is built
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    @Transactional
    public void deleteTicket(UUID id) {
        // ADMIN-only is enforced in TicketController; an already deleted ticket gives 404
        Ticket ticket = findTicketOrThrow(id);
        ticket.setDeletedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public TicketResponse addLabel(UUID ticketId, UUID labelId) {
        // AGENT/ADMIN-only is enforced in TicketController
        UserAccount currentUser = userService.getOrCreateCurrentUser();
        Ticket ticket = findTicketOrThrow(ticketId);
        Label label = findLabelOrThrow(labelId);
        checkNotClosed(ticket);

        // Idempotent: an already attached label changes nothing and records no event
        if (!ticket.getLabels().add(label)) {
            return ticketMapper.toResponse(ticket);
        }

        ticketEventRecorder.record(ticket, TicketEventType.LABEL_ADDED, null, label.getName(), currentUser);
        return ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
    }

    @Override
    @Transactional
    public void removeLabel(UUID ticketId, UUID labelId) {
        UserAccount currentUser = userService.getOrCreateCurrentUser();
        Ticket ticket = findTicketOrThrow(ticketId);
        Label label = findLabelOrThrow(labelId);
        checkNotClosed(ticket);

        if (!ticket.getLabels().remove(label)) {
            throw new ResourceNotFoundException("Label is niet gekoppeld aan dit ticket");
        }

        ticketEventRecorder.record(ticket, TicketEventType.LABEL_REMOVED, label.getName(), null, currentUser);
        ticketRepository.save(ticket);
    }

    // AGENT and ADMIN see every ticket; a REPORTER gets 403 instead of 404 on someone else's ticket
    private void checkAccess(Ticket ticket) {
        if (!currentUserService.isAgentOrAdmin()
                && !ticket.getReporter().getId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("Je hebt geen toegang tot dit ticket");
        }
    }

    private void checkNotClosed(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Een gesloten ticket kan niet meer worden gewijzigd");
        }
    }

    private Category resolveCategory(UUID categoryId, Category currentCategory) {
        if (categoryId == null) {
            return null;
        }
        Category category = findCategoryOrThrow(categoryId);
        boolean unchanged = currentCategory != null && currentCategory.getId().equals(categoryId);
        if (!unchanged && category.getStatus() == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException("Een inactieve categorie kan niet aan een ticket worden gekoppeld");
        }
        return category;
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket niet gevonden"));
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie niet gevonden"));
    }

    private Label findLabelOrThrow(UUID id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label niet gevonden"));
    }
}
