package com.resumeai.section.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { 
    	super(message); 
    }
}
