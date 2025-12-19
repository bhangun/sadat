package tech.kayys.wayang.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public class PageResult<T> {
    private List<T> content;
    private long totalCount;

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public boolean hasNextPage() {
        return false;
    }

    public boolean hasPreviousPage() {
        return false;
    }
}
