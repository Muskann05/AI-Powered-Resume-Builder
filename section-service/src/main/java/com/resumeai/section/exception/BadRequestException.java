package com.resumeai.section.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { 
    	super(message); 
    }
}
