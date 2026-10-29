package nl.ticketflow.service.impl;

import nl.ticketflow.dto.request.CreateTicketRequest;
import nl.ticketflow.dto.request.UpdateTicketRequest;
import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.Category;
import nl.ticketflow.entity.CategoryStatus;
import nl.ticketflow.entity.Ticket;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.entity.UserAccount;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.TicketMapper;
import nl.ticketflow.repository.CategoryRepository;
import nl.ticketflow.repository.TicketRepository;
import nl.ticketflow.repository.TicketSpecifications;
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.TicketService;
import nl.ticketflow.service.UserService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public TicketServiceImpl(TicketRepository ticketRepository, CategoryRepository categoryRepository,
                             TicketMapper ticketMapper, CurrentUserService currentUserService,
                             UserService userService) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.ticketMapper = ticketMapper;
        this.currentUserService = currentUserService;
        this.userService = userService;
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
        return ticketMapper.toResponse(ticketRepository.save(ticket));
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
                                            UUID assigneeId) {
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

    // AGENT and ADMIN see every ticket; a REPORTER gets 403 instead of 404 on someone else's ticket
    private void checkAccess(Ticket ticket) {
        if (!currentUserService.isAgentOrAdmin()
                && !ticket.getReporter().getId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("Je hebt geen toegang tot dit ticket");
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
}
