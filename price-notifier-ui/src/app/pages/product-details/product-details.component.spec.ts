import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { ProductService } from '../../services/product/product.service';
import { of, throwError } from 'rxjs';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { DatePipe } from '@angular/common';
import {ProductDetailsComponent} from './product-details.component';
import {ProductDetails} from '../../models/product-details.model';
import {Directive, Input} from '@angular/core';
import {BaseChartDirective} from 'ng2-charts';

@Directive({
  selector: 'canvas[baseChart]',
  standalone: true
})
class MockBaseChartDirective {
  @Input() data: any;
  @Input() options: any;
  @Input() type: any;
}

const mockHistory = [
  { price: 200, checkedAt: '2023-01-02T10:00:00.000Z' },
  { price: 150, checkedAt: '2023-01-01T10:00:00.000Z' }
];

const mockProductData: ProductDetails = {
  id: 123,
  productName: 'Test Product',
  productUrl: 'http://example.com',
  currentPrice: 199.99,
  priceHistory: mockHistory
};

describe('ProductDetailsComponent', () => {
  let component: ProductDetailsComponent;
  let fixture: ComponentFixture<ProductDetailsComponent>;
  let productServiceMock: any;
  let routeMock: any;

  beforeEach(async () => {
    productServiceMock = {
      getById: vi.fn().mockReturnValue(of(mockProductData))
    };

    routeMock = {
      snapshot: {
        paramMap: {
          get: vi.fn().mockReturnValue('123')
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [ProductDetailsComponent],
      providers: [
        provideRouter([]),
        DatePipe,
        { provide: ProductService, useValue: productServiceMock },
        { provide: ActivatedRoute, useValue: routeMock }
      ]
    })
      .overrideComponent(ProductDetailsComponent, {
        remove: { imports: [BaseChartDirective] },
        add: { imports: [MockBaseChartDirective] }
      }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailsComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load product details on init when ID is present', () => {
    fixture.detectChanges(); // Uruchamia ngOnInit

    expect(productServiceMock.getById).toHaveBeenCalledWith('123');
    expect(component.product()).toEqual(mockProductData);
    expect(component.loading()).toBe(false);
  });

  it('should NOT call service if ID is missing in route', () => {
    routeMock.snapshot.paramMap.get.mockReturnValue(null);

    fixture.detectChanges(); // ngOnInit

    expect(productServiceMock.getById).not.toHaveBeenCalled();
    expect(component.loading()).toBe(true);
  });

  it('should correctly calculate lowest historical price', () => {
    fixture.detectChanges();
    expect(component.lowestPrice()).toBe(150);
  });

  it('should use currentPrice as lowestPrice if history is empty', () => {
    const productWithoutHistory = { ...mockProductData, priceHistory: [] };
    productServiceMock.getById.mockReturnValue(of(productWithoutHistory));

    fixture.detectChanges();

    expect(component.lowestPrice()).toBe(199.99);
  });

  it('should process and sort chart data chronologically', () => {
    fixture.detectChanges();

    const chartData = component.lineChartData;
    const prices = chartData.datasets[0].data;
    const labels = chartData.labels;

    expect(prices[0]).toBe(150);
    expect(prices[1]).toBe(200);

    expect(labels?.length).toBe(2);
    expect(typeof labels?.[0]).toBe('string');
  });

  it('should stop loading on service error', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    productServiceMock.getById.mockReturnValue(throwError(() => new Error('Server Error')));

    fixture.detectChanges();

    expect(productServiceMock.getById).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(component.product()).toBeNull();
  });
});
