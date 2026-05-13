import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RiskFlagTypeAdminService } from './risk-flag-type-admin.service';
import { RiskFlagType } from '../models/risk-flag-type-admin.model';

describe('RiskFlagTypeAdminService', () => {
  let service: RiskFlagTypeAdminService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/admin/risk-flag-types';

  const mockFlagType: RiskFlagType = {
    id: 'type-uuid-1',
    name: 'Self-Harm Risk',
    displayOrder: 1,
    active: true,
  };

  const mockInactiveFlagType: RiskFlagType = {
    id: 'type-uuid-2',
    name: 'Crisis History',
    displayOrder: 2,
    active: false,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RiskFlagTypeAdminService],
    });
    service = TestBed.inject(RiskFlagTypeAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listAll', () => {
    it('shouldReturnAllFlagTypesIncludingInactive_whenCalled', () => {
      // Arrange
      const expectedTypes = [mockFlagType, mockInactiveFlagType];

      // Act
      let result: RiskFlagType[] | undefined;
      service.listAll().subscribe(types => (result = types));

      // Assert
      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.method).toBe('GET');
      req.flush(expectedTypes);
      expect(result).toEqual(expectedTypes);
      expect(result!.length).toBe(2);
    });

    it('shouldReturnEmptyArray_whenNoFlagTypesConfigured', () => {
      // Act
      let result: RiskFlagType[] | undefined;
      service.listAll().subscribe(types => (result = types));

      // Assert
      const req = httpMock.expectOne(BASE_URL);
      req.flush([]);
      expect(result).toEqual([]);
    });

    it('shouldUseAdminEndpoint_notPublicEndpoint', () => {
      // Act
      service.listAll().subscribe();

      // Assert
      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.url).toContain('/admin/');
      req.flush([]);
    });
  });

  describe('create', () => {
    it('shouldPostNameAndDisplayOrderAndReturnCreatedFlagType_whenValidInput', () => {
      // Arrange
      const name = 'New Risk Type';
      const displayOrder = 5;

      // Act
      let result: RiskFlagType | undefined;
      service.create(name, displayOrder).subscribe(ft => (result = ft));

      // Assert
      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ name, displayOrder });
      req.flush({ id: 'type-uuid-3', name, displayOrder, active: true });
      expect(result!.name).toBe(name);
      expect(result!.displayOrder).toBe(displayOrder);
      expect(result!.active).toBe(true);
    });

    it('shouldSendDisplayOrderZero_whenDisplayOrderIsZero', () => {
      // Act
      service.create('Zero Order', 0).subscribe();

      // Assert
      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.body.displayOrder).toBe(0);
      req.flush({ id: 'type-uuid-4', name: 'Zero Order', displayOrder: 0, active: true });
    });
  });

  describe('deactivate', () => {
    it('shouldPatchDeactivateEndpointWithFlagTypeId_whenIdIsProvided', () => {
      // Arrange
      const id = 'type-uuid-1';

      // Act
      let completed = false;
      service.deactivate(id).subscribe(() => (completed = true));

      // Assert
      const req = httpMock.expectOne(`${BASE_URL}/${id}/deactivate`);
      expect(req.request.method).toBe('PATCH');
      req.flush(null);
      expect(completed).toBe(true);
    });

    it('shouldIncludeIdInDeactivateUrl_whenDeactivatingSpecificFlagType', () => {
      // Arrange
      const id = 'type-uuid-2';

      // Act
      service.deactivate(id).subscribe();

      // Assert
      const req = httpMock.expectOne(`${BASE_URL}/${id}/deactivate`);
      expect(req.request.url).toContain(id);
      expect(req.request.url).toContain('/deactivate');
      req.flush(null);
    });

    it('shouldSendEmptyBody_whenDeactivating', () => {
      // Act
      service.deactivate('type-uuid-1').subscribe();

      // Assert
      const req = httpMock.expectOne(`${BASE_URL}/type-uuid-1/deactivate`);
      expect(req.request.body).toEqual({});
      req.flush(null);
    });
  });
});
