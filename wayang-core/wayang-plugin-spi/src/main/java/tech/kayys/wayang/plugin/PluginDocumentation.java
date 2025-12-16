package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.List;

import tech.kayys.wayang.plugin.node.NodeExample;


/**
 * Plugin Documentation
 */
public class PluginDocumentation {
    
    private String description;
    
 
    private List<NodeExample> examples = new ArrayList<>();
    
    private String gettingStartedUrl;
    private String apiDocumentationUrl;
    private String videoTutorialUrl;
    
 
    private List<FAQ> faqs = new ArrayList<>();


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NodeExample> getExamples() {
        return examples;
    }

    public void setExamples(List<NodeExample> examples) {
        this.examples = examples;
    }

    public String getGettingStartedUrl() {
        return gettingStartedUrl;
    }

    public void setGettingStartedUrl(String gettingStartedUrl) {
        this.gettingStartedUrl = gettingStartedUrl;
    }

    public String getApiDocumentationUrl() {
        return apiDocumentationUrl;
    }

    public void setApiDocumentationUrl(String apiDocumentationUrl) {
        this.apiDocumentationUrl = apiDocumentationUrl;
    }

    public String getVideoTutorialUrl() {
        return videoTutorialUrl;
    }

    public void setVideoTutorialUrl(String videoTutorialUrl) {
        this.videoTutorialUrl = videoTutorialUrl;
    }

    public List<FAQ> getFaqs() {
        return faqs;
    }

    public void setFaqs(List<FAQ> faqs) {
        this.faqs = faqs;
    }

       public static Builder builder() {
        return new Builder();
    }



    public static class Builder {
        private String description;
        private List<NodeExample> examples = new ArrayList<>();
        private String gettingStartedUrl;
        private String apiDocumentationUrl;
        private String videoTutorialUrl;
        private List<FAQ> faqs = new ArrayList<>();

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder examples(List<NodeExample> examples) {
            this.examples = examples;
            return this;
        }

        public Builder gettingStartedUrl(String gettingStartedUrl) {
            this.gettingStartedUrl = gettingStartedUrl;
            return this;
        }

        public Builder apiDocumentationUrl(String apiDocumentationUrl) {
            this.apiDocumentationUrl = apiDocumentationUrl;
            return this;
        }

        public Builder videoTutorialUrl(String videoTutorialUrl) {
            this.videoTutorialUrl = videoTutorialUrl;
            return this;
        }

        public Builder faqs(List<FAQ> faqs) {
            this.faqs = faqs;
            return this;
        }

        public PluginDocumentation build() {
            PluginDocumentation documentation = new PluginDocumentation();
            documentation.setDescription(description);
            documentation.setExamples(examples);
            documentation.setGettingStartedUrl(gettingStartedUrl);
            documentation.setApiDocumentationUrl(apiDocumentationUrl);
            documentation.setVideoTutorialUrl(videoTutorialUrl);
            documentation.setFaqs(faqs);
            return documentation;
        }
    }
}