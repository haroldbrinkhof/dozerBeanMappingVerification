package be.catsandcoding.dozer.mappings;

public class Success {
    private Boolean success;
    private boolean failure;

    public Boolean isSuccess() {
        return success;
    }

    public void setSuccess(Boolean customField) {
        this.success = customField;
    }

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }
}
