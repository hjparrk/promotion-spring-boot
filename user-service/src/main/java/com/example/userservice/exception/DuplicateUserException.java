package com.example.userservice.exception;

public class DuplicateUserException extends RuntimeException{
    public DuplicateUserException(String message) { super(message);}
}
