package nl.ticketflow.mapper;

import nl.ticketflow.dto.request.CreateLabelRequest;
import nl.ticketflow.dto.response.LabelResponse;
import nl.ticketflow.entity.Label;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {

    public Label toEntity(CreateLabelRequest request) {
        Label label = new Label();
        label.setName(request.name().trim());
        label.setColor(request.color());
        return label;
    }

    public LabelResponse toResponse(Label label) {
        return new LabelResponse(
                label.getId(),
                label.getName(),
                label.getColor()
        );
    }
}
