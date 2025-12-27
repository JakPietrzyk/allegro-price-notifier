import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

import { DashboardComponent } from './dashboard.component';
import { ProductService } from '../../services/product/product.service';
import { AuthService } from '../../services/auth/auth.service';
import {DASHBOARD_CONSTANTS} from '../../core/constants/dashboard.component';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const productServiceMock = {
    getAll: vi.fn(),
    addByName: vi.fn(),
    addByUrl: vi.fn(),
    delete: vi.fn()
  };

  const authServiceMock = {
    logout: vi.fn()
  };

  const mockProducts = [
    {
      id: 1,
      productName: 'Test Product 1',
      currentPrice: 100,
      productUrl: 'http://test.pl',
      lastChecked: new Date().toISOString()
    },
    {
      id: 2,
      productName: 'Test Product 2',
      currentPrice: 200,
      productUrl: 'http://test2.pl',
      lastChecked: new Date().toISOString()
    }
  ];

  beforeEach(async () => {
    vi.clearAllMocks();

    productServiceMock.getAll.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: ProductService, useValue: productServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load products on init', () => {
    productServiceMock.getAll.mockReturnValue(of(mockProducts));

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.products()).toEqual(mockProducts);
    expect(productServiceMock.getAll).toHaveBeenCalledTimes(1);
  });

  it('should handle error when loading products fails', () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});

    productServiceMock.getAll.mockReturnValue(throwError(() => new Error('Network error')));

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.products()).toEqual([]);
  });

  it('should call authService.logout when logout is clicked', () => {
    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });

  it('should add product by name and reload list on success', () => {
    const productName = 'PlayStation 5';
    component.newProductName.set(productName);

    productServiceMock.addByName.mockReturnValue(of({}));
    productServiceMock.getAll.mockReturnValue(of([mockProducts[0]]));

    component.addProductByName();

    expect(productServiceMock.addByName).toHaveBeenCalledWith(productName);
    expect(component.newProductName()).toBe('');
    expect(productServiceMock.getAll).toHaveBeenCalled();
  });

  it('should NOT call service if product name is empty', () => {
    component.newProductName.set('');
    component.addProductByName();
    expect(productServiceMock.addByName).not.toHaveBeenCalled();
  });

  it('should show alert on add by name error', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    component.newProductName.set('Bad Product');
    productServiceMock.addByName.mockReturnValue(throwError(() => new Error('Fail')));

    component.addProductByName();

    expect(component.loading()).toBe(false);
  });

  it('should add product by URL and reload list on success', () => {
    const url = 'https://ceneo.pl/123';
    component.newProductUrl.set(url);

    productServiceMock.addByUrl.mockReturnValue(of({}));
    productServiceMock.getAll.mockReturnValue(of([]));

    component.addProductByUrl();

    expect(productServiceMock.addByUrl).toHaveBeenCalledWith(url);
    expect(component.newProductUrl()).toBe('');
  });

  it('should delete product if confirmed by user', () => {
    const productId = 123;
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    const initialProduct = { ...mockProducts[0], id: productId };
    component.products.set([initialProduct]);

    productServiceMock.delete.mockReturnValue(of(undefined));

    const mockEvent = { stopPropagation: vi.fn() } as unknown as Event;

    component.deleteProduct(productId, mockEvent);

    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(confirmSpy).toHaveBeenCalledWith(DASHBOARD_CONSTANTS.MESSAGES.CONFIRM_DELETE);
    expect(productServiceMock.delete).toHaveBeenCalledWith(productId);

    expect(component.products().length).toBe(0);
  });

  it('should NOT delete product if user cancels confirmation', () => {
    const productId = 123;
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const mockEvent = { stopPropagation: vi.fn() } as unknown as Event;

    component.deleteProduct(productId, mockEvent);

    expect(productServiceMock.delete).not.toHaveBeenCalled();
  });

  it('should show alert if delete fails', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.spyOn(console, 'error').mockImplementation(() => {});

    productServiceMock.delete.mockReturnValue(throwError(() => new Error('Delete error')));
    const mockEvent = { stopPropagation: vi.fn() } as unknown as Event;

    component.deleteProduct(1, mockEvent);

    expect(component.loading()).toBe(false);
  });
});
