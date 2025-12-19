package tech.kayys.wayang.automation.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.automation.dto.ProcessTemplate;
import tech.kayys.wayang.automation.dto.ProcessType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BusinessProcessLibrary {

    public Uni<List<ProcessTemplate>> getTemplates(String industry, ProcessType type) {
        // Simple mock implementation
        List<ProcessTemplate> templates = new ArrayList<>();

        if (type == null || type == ProcessType.APPROVAL) {
            templates.add(new ProcessTemplate(
                    "tpl-approval-v1",
                    "Standard Multi-level Approval",
                    "A standard multi-level approval process",
                    industry != null ? industry : "General",
                    ProcessType.APPROVAL,
                    Map.of("levels", 2)));
        }

        if (type == null || type == ProcessType.DOCUMENT_PROCESSING) {
            templates.add(new ProcessTemplate(
                    "tpl-doc-v1",
                    "Invoice Processing",
                    "Extract data from invoices using OCR",
                    industry != null ? industry : "Finance",
                    ProcessType.DOCUMENT_PROCESSING,
                    Map.of("engine", "tesseract")));
        }

        return Uni.createFrom().item(templates);
    }
}
