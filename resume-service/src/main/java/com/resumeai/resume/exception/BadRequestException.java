package com.resumeai.resume.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { 
    	super(message); 
    }
}
