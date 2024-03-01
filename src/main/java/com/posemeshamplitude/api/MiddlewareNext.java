package com.posemeshamplitude.api;

public interface MiddlewareNext {
    public void run(MiddlewarePayload curPayload);
}
