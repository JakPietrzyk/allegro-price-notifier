import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import {GlobalErrorService} from '../../services/error/global-error.service';
import {ERROR_MESSAGES, ErrorCode} from '../../models/errors/error-codes';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const errorService = inject(GlobalErrorService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let userMessage = ERROR_MESSAGES[ErrorCode.INTERNAL_SERVER_ERROR];

      if (error.error && typeof error.error === 'object') {
        const backendCode = error.error.code;

        if (backendCode && ERROR_MESSAGES[backendCode as ErrorCode]) {
          userMessage = ERROR_MESSAGES[backendCode as ErrorCode];
        }
      }

      errorService.showError(userMessage);

      return throwError(() => error);
    })
  );
};
