import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token;
  const user = authService.currentUserValue;

  let headers = req.headers;

  // Inject Bearer token if it exists
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  // Inject User ID directly so downstream microservices (like project-service) can recognize the user context
  if (user) {
    headers = headers.set('X-User-Id', user.id);
  }

  const clonedRequest = req.clone({ headers });
  return next(clonedRequest);
};