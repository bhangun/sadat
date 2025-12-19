package tech.kayys.wayang.schema.governance;

import java.util.List;

public class Trust {
    private Double rating = 0.0;
    private List<String> disputes;

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        if (rating != null && (rating < 0 || rating > 1)) {
            throw new IllegalArgumentException("Trust rating must be between 0 and 1");
        }
        this.rating = rating;
    }

    public List<String> getDisputes() {
        return disputes;
    }

    public void setDisputes(List<String> disputes) {
        this.disputes = disputes;
    }
}
