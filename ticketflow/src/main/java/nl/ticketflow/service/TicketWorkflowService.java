package nl.ticketflow.service;

import nl.ticketflow.dto.response.TicketResponse;
import nl.ticketflow.entity.TicketStatus;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.ForbiddenException;
import nl.ticketflow.exception.InvalidStatusTransitionException;
import nl.ticketflow.exception.ResourceNotFoundException;

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
}
