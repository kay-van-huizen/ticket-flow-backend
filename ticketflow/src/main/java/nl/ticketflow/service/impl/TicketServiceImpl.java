package nl.ticketflow.service.impl;

import nl.ticketflow.dto.request.CreateTicketRequest;
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
import nl.ticketflow.security.CurrentUserService;
import nl.ticketflow.service.TicketService;
import nl.ticketflow.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Category category = request.categoryId() == null ? null : findCategoryOrThrow(request.categoryId());
        if (category != null && category.getStatus() == CategoryStatus.INACTIVE) {
            throw new BusinessRuleException("Een inactieve categorie kan niet aan een ticket worden gekoppeld");
        }

        Ticket ticket = ticketMapper.toEntity(request, reporter, category);
        // New tickets always start as NORMAL and OPEN; dueAt follows with the SLA policy
        ticket.setPriority(TicketPriority.NORMAL);
        ticket.setStatus(TicketStatus.OPEN);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID id) {
        Ticket ticket = ticketRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket niet gevonden"));

        // A reporter gets 403 instead of 404 on someone else's ticket
        if (!currentUserService.isAgentOrAdmin()
                && !ticket.getReporter().getId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("Je hebt geen toegang tot dit ticket");
        }

        return ticketMapper.toResponse(ticket);
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie niet gevonden"));
    }
}
