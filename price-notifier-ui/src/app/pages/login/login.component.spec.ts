import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { of, throwError } from 'rxjs';
import {LOGIN_CONSTANTS} from '../../core/constants/login.constants';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  const authServiceMock = {
    login: vi.fn(),
    isLoggedIn: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    authServiceMock.isLoggedIn.mockReturnValue(false);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect if already logged in on init', () => {
    authServiceMock.isLoggedIn.mockReturnValue(true);
    component.ngOnInit();
  });

  it('should call authService.login on submit with correct credentials', () => {
    // Arrange
    const testEmail = 'test@test.pl';
    const testPass = '12345';

    component.email.set(testEmail);
    component.password.set(testPass);

    authServiceMock.login.mockReturnValue(of({}));

    // Act
    component.onSubmit();

    // Assert
    expect(component.loading()).toBe(false);
    expect(authServiceMock.login).toHaveBeenCalledWith({ email: testEmail, password: testPass });
  });

  it('should show error message on login failure', () => {
    // Arrange
    component.email.set('bad@user.com');
    component.password.set('wrongpass');

    authServiceMock.login.mockReturnValue(throwError(() => new Error('Auth failed')));

    // Act
    component.onSubmit();

    // Assert
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe(LOGIN_CONSTANTS.MESSAGES.ERRORS.INVALID_CREDENTIALS);
  });
});
