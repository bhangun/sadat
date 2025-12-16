package tech.kayys.wayang.plugin;

/**
 * FAQ
 */
public class FAQ {
    private String question;
    private String answer;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public static class Builder {
        private String question;
        private String answer;

        public Builder setQuestion(String question) {
            this.question = question;
            return this;
        }

        public Builder setAnswer(String answer) {
            this.answer = answer;
            return this;
        }

        public FAQ build() {
            FAQ faq = new FAQ();
            faq.setQuestion(question);
            faq.setAnswer(answer);
            return faq;
        }
    }
}
