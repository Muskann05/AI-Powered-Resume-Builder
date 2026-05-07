package com.resumeai.resume.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { 
    	super(message); 
    }
}
