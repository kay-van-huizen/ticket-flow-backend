package nl.ticketflow.service;

import nl.ticketflow.dto.response.TicketEventResponse;
import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.TicketPriority;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.InvalidStatusTransitionException;
import nl.ticketflow.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

public interface TicketWorkflowService {

    /**
     * Changes the status of a ticket according to the workflow rules, applying the role of the current user.
     *
     * @throws ResourceNotFoundException        if the ticket does not exist or is deleted
     * @throws ForbiddenException               if the current user may not perform this transition
     * @throws InvalidStatusTransitionException if the transition is not allowed by the workflow
     * @throws BusinessRuleException            if the transition violates another business rule
     */
    TicketResponse changeStatus(UUID ticketId, TicketStatus newStatus);

    /**
     * Changes the priority of a ticket and recalculates its due date from the creation time; only for agents and admins.
     *
     * @throws ResourceNotFoundException if the ticket does not exist or is deleted
     * @throws ForbiddenException        if the current user is not an agent or admin
     * @throws BusinessRuleException     if the ticket is closed
     */
    TicketResponse changePriority(UUID ticketId, TicketPriority newPriority);

    /**
     * Assigns a ticket to an agent or admin; only for agents and admins.
     *
     * @throws ResourceNotFoundException if the ticket or the assignee does not exist
     * @throws ForbiddenException        if the current user is not an agent or admin
     * @throws BusinessRuleException     if the ticket is closed or the assignee is not an agent or admin
     */
    TicketResponse assign(UUID ticketId, UUID assigneeId);

    /**
     * Returns the events of a ticket, oldest first; a reporter can only read the history of their own ticket.
     *
     * @throws ResourceNotFoundException if the ticket does not exist or is deleted
     * @throws ForbiddenException        if a reporter requests the history of someone else's ticket
     */
    List<TicketEventResponse> getHistory(UUID ticketId);
}
