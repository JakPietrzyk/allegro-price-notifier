import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {GlobalErrorService} from './global-error.service';

describe('GlobalErrorService', () => {
  let service: GlobalErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GlobalErrorService);

    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should update signal when showError is called', () => {
    service.showError('Test error');
    expect(service.error()).toBe('Test error');
  });

  it('should clear error when clear is called', () => {
    service.showError('Test');
    service.clear();
    expect(service.error()).toBeNull();
  });

  it('should auto-clear error after 5 seconds', () => {
    service.showError('Timeout test');
    expect(service.error()).toBe('Timeout test');

    vi.advanceTimersByTime(5000);

    expect(service.error()).toBeNull();
  });
});
