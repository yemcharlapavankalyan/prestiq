package org.prestiq.recommendation.exception;

public class DownstreamServiceException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;

    public DownstreamServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = 503;
    }

    public DownstreamServiceException(String serviceName, int statusCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
