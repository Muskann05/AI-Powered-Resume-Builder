package com.resumeai.auth.exception;

public class UnauthorizedException extends RuntimeException { 
	public UnauthorizedException(String m){ 
		super(m);
	} 
}
