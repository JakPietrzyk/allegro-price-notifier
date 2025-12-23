import { TestBed } from '@angular/core/testing';
import { ProductService } from './product.service';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ProductObservation } from '../../models/product-observation.model';
import { ProductDetails } from '../../models/product-details.model';
import {PRODUCT_CONSTANTS} from '../../core/constants/product.constants';

describe('ProductService', () => {
  let service: ProductService;
  let httpClientMock: any;

  beforeEach(() => {
    httpClientMock = {
      get: vi.fn(),
      post: vi.fn(),
      delete: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        ProductService,
        { provide: HttpClient, useValue: httpClientMock }
      ]
    });

    service = TestBed.inject(ProductService);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call getAll with correct URL', () => {
    const mockData: ProductObservation[] = [];
    httpClientMock.get.mockReturnValue(of(mockData));

    service.getAll().subscribe(res => {
      expect(res).toEqual(mockData);
    });

    expect(httpClientMock.get).toHaveBeenCalledWith(PRODUCT_CONSTANTS.API.BASE);
  });

  it('should call getById with correct URL', () => {
    const mockDetails: ProductDetails = { id: 1 } as any;
    const testId = 123;
    httpClientMock.get.mockReturnValue(of(mockDetails));

    service.getById(testId).subscribe(res => {
      expect(res).toEqual(mockDetails);
    });

    expect(httpClientMock.get).toHaveBeenCalledWith(PRODUCT_CONSTANTS.API.BY_ID(testId));
  });

  it('should call addByName with correct URL and payload', () => {
    const name = 'Laptop';
    httpClientMock.post.mockReturnValue(of(void 0));

    service.addByName(name).subscribe();

    expect(httpClientMock.post).toHaveBeenCalledWith(
      PRODUCT_CONSTANTS.API.SEARCH,
      { productName: name }
    );
  });

  it('should call addByUrl with correct URL and payload', () => {
    const url = 'http://shop.com/item';
    httpClientMock.post.mockReturnValue(of(void 0));

    service.addByUrl(url).subscribe();

    expect(httpClientMock.post).toHaveBeenCalledWith(
      PRODUCT_CONSTANTS.API.URL_ADD,
      { productUrl: url }
    );
  });

  it('should call delete with correct URL', () => {
    const idToDelete = 55;
    httpClientMock.delete.mockReturnValue(of(void 0));

    service.delete(idToDelete).subscribe();

    expect(httpClientMock.delete).toHaveBeenCalledWith(PRODUCT_CONSTANTS.API.BY_ID(idToDelete));
  });
});
