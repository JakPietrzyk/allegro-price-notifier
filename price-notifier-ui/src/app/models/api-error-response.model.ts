import {ErrorCode} from './errors/error-codes';

export interface ApiErrorResponse {
  message: string;
  code: ErrorCode;
  status: number;
  timestamp: string;
}
