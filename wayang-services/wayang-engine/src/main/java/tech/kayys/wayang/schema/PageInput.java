package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * PageInput - Pagination input
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageInput {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    private String sortBy = "createdAt";
    private String sortDir = "DESC";

    // Getters and setters...
    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }
}