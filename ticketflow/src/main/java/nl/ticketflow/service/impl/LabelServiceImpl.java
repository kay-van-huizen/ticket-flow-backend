package nl.ticketflow.service.impl;

import nl.ticketflow.dto.request.CreateLabelRequest;
import nl.ticketflow.dto.response.LabelResponse;
import nl.ticketflow.entity.Label;
import nl.ticketflow.exception.BusinessRuleException;
import nl.ticketflow.exception.DuplicateResourceException;
import nl.ticketflow.exception.ResourceNotFoundException;
import nl.ticketflow.mapper.LabelMapper;
import nl.ticketflow.repository.LabelRepository;
import nl.ticketflow.service.LabelService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;

    public LabelServiceImpl(LabelRepository labelRepository, LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelResponse> getAll() {
        return labelRepository.findAll(Sort.by(Sort.Order.asc("name").ignoreCase())).stream()
                .map(labelMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public LabelResponse create(CreateLabelRequest request) {
        String name = request.name().trim();
        if (labelRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Er bestaat al een label met de naam '" + name + "'");
        }

        Label label = labelMapper.toEntity(request);
        return labelMapper.toResponse(labelRepository.save(label));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label niet gevonden"));

        if (labelRepository.isInUse(id)) {
            throw new BusinessRuleException("Label is nog in gebruik en kan niet worden verwijderd");
        }

        labelRepository.delete(label);
    }
}
