package com.posemeshamplitude.api;

public interface Middleware {
	void run(MiddlewarePayload payload, MiddlewareNext next);
}