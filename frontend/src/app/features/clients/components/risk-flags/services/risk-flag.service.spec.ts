import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RiskFlagService } from './risk-flag.service';
import {
  CreateRiskFlagPayload,
  ResolveRiskFlagPayload,
  RiskFlag,
  RiskFlagType,
} from '../models/risk-flag.model';

describe('RiskFlagService', () => {
  let service: RiskFlagService;
  let httpMock: HttpTestingController;

  const CLIENT_ID = 'client-uuid-1';
  const FLAG_ID = 'flag-uuid-1';

  const mockRiskFlagType: RiskFlagType = {
    id: 'type-uuid-1',
    name: 'Self-Harm Risk',
    displayOrder: 1,
    active: true,
  };

  const mockActiveFlag: RiskFlag = {
    id: FLAG_ID,
    clientId: CLIENT_ID,
    flagTypeId: 'type-uuid-1',
    flagTypeName: 'Self-Harm Risk',
    status: 'ACTIVE',
    clinicalNote: 'Patient expressed concern',
    reviewDate: '2026-06-01',
    createdByUserId: 'user-uuid-1',
    createdAt: '2026-05-01T10:00:00Z',
    resolvedByUserId: null,
    resolvedAt: null,
    resolutionNote: null,
  };

  const mockResolvedFlag: RiskFlag = {
    ...mockActiveFlag,
    status: 'RESOLVED',
    resolvedByUserId: 'user-uuid-2',
    resolvedAt: '2026-05-13T09:00:00Z',
    resolutionNote: 'Risk has been managed',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RiskFlagService],
    });
    service = TestBed.inject(RiskFlagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listActive', () => {
    it('shouldReturnActiveFlagsForClient_whenClientIdIsProvided', () => {
      // Arrange
      const expectedFlags = [mockActiveFlag];

      // Act
      let result: RiskFlag[] | undefined;
      service.listActive(CLIENT_ID).subscribe(flags => (result = flags));

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags`);
      expect(req.request.method).toBe('GET');
      req.flush(expectedFlags);
      expect(result).toEqual(expectedFlags);
    });

    it('shouldReturnEmptyArray_whenClientHasNoActiveFlags', () => {
      // Arrange & Act
      let result: RiskFlag[] | undefined;
      service.listActive(CLIENT_ID).subscribe(flags => (result = flags));

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags`);
      req.flush([]);
      expect(result).toEqual([]);
    });

    it('shouldReturnFlagsWithNullClinicalNote_whenCallerLacksReadRiskFlagNotesPermission', () => {
      // Arrange
      const flagWithoutNote: RiskFlag = { ...mockActiveFlag, clinicalNote: null };

      // Act
      let result: RiskFlag[] | undefined;
      service.listActive(CLIENT_ID).subscribe(flags => (result = flags));

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags`);
      req.flush([flagWithoutNote]);
      expect(result![0].clinicalNote).toBeNull();
    });
  });

  describe('listAll', () => {
    it('shouldReturnAllFlagsIncludingResolved_whenClientIdIsProvided', () => {
      // Arrange
      const allFlags = [mockActiveFlag, mockResolvedFlag];

      // Act
      let result: RiskFlag[] | undefined;
      service.listAll(CLIENT_ID).subscribe(flags => (result = flags));

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags/history`);
      expect(req.request.method).toBe('GET');
      req.flush(allFlags);
      expect(result).toEqual(allFlags);
      expect(result!.length).toBe(2);
    });

    it('shouldUseHistoryEndpoint_whenListAllIsCalled', () => {
      // Act
      service.listAll(CLIENT_ID).subscribe();

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags/history`);
      expect(req.request.url).toContain('/history');
      req.flush([]);
    });
  });

  describe('create', () => {
    it('shouldPostPayloadToCorrectEndpointAndReturnCreatedFlag_whenPayloadIsValid', () => {
      // Arrange
      const payload: CreateRiskFlagPayload = {
        flagTypeId: 'type-uuid-1',
        clinicalNote: 'Initial observation',
        reviewDate: '2026-06-01',
      };

      // Act
      let result: RiskFlag | undefined;
      service.create(CLIENT_ID, payload).subscribe(flag => (result = flag));

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(mockActiveFlag);
      expect(result).toEqual(mockActiveFlag);
    });

    it('shouldSendNullClinicalNote_whenClinicalNoteIsNull', () => {
      // Arrange
      const payload: CreateRiskFlagPayload = {
        flagTypeId: 'type-uuid-1',
        clinicalNote: null,
        reviewDate: '2026-06-01',
      };

      // Act
      service.create(CLIENT_ID, payload).subscribe();

      // Assert
      const req = httpMock.expectOne(`/api/v1/clients/${CLIENT_ID}/risk-flags`);
      expect(req.request.body.clinicalNote).toBeNull();
      req.flush(mockActiveFlag);
    });
  });

  describe('resolve', () => {
    it('shouldPatchResolveEndpointAndReturnResolvedFlag_whenFlagExists', () => {
      // Arrange
      const payload: ResolveRiskFlagPayload = {
        resolutionNote: 'Risk has been managed',
      };

      // Act
      let result: RiskFlag | undefined;
      service.resolve(CLIENT_ID, FLAG_ID, payload).subscribe(flag => (result = flag));

      // Assert
      const req = httpMock.expectOne(
        `/api/v1/clients/${CLIENT_ID}/risk-flags/${FLAG_ID}/resolve`
      );
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(payload);
      req.flush(mockResolvedFlag);
      expect(result).toEqual(mockResolvedFlag);
      expect(result!.status).toBe('RESOLVED');
    });

    it('shouldIncludeFlagIdInUrl_whenResolvingFlag', () => {
      // Arrange
      const anotherFlagId = 'flag-uuid-2';
      const payload: ResolveRiskFlagPayload = { resolutionNote: 'Resolved' };

      // Act
      service.resolve(CLIENT_ID, anotherFlagId, payload).subscribe();

      // Assert
      const req = httpMock.expectOne(
        `/api/v1/clients/${CLIENT_ID}/risk-flags/${anotherFlagId}/resolve`
      );
      expect(req.request.url).toContain(anotherFlagId);
      req.flush(mockResolvedFlag);
    });
  });

  describe('listTypes', () => {
    it('shouldReturnActiveFlagTypes_whenCalled', () => {
      // Arrange
      const types = [
        mockRiskFlagType,
        { id: 'type-uuid-2', name: 'Crisis History', displayOrder: 2, active: true },
      ];

      // Act
      let result: RiskFlagType[] | undefined;
      service.listTypes().subscribe(t => (result = t));

      // Assert
      const req = httpMock.expectOne('/api/v1/risk-flag-types');
      expect(req.request.method).toBe('GET');
      req.flush(types);
      expect(result).toEqual(types);
      expect(result!.length).toBe(2);
    });

    it('shouldUseRiskFlagTypesEndpoint_notClientScopedEndpoint', () => {
      // Act
      service.listTypes().subscribe();

      // Assert
      const req = httpMock.expectOne('/api/v1/risk-flag-types');
      expect(req.request.url).not.toContain('/clients/');
      req.flush([]);
    });

    it('shouldReturnEmptyArray_whenNoTypesConfigured', () => {
      // Act
      let result: RiskFlagType[] | undefined;
      service.listTypes().subscribe(t => (result = t));

      // Assert
      const req = httpMock.expectOne('/api/v1/risk-flag-types');
      req.flush([]);
      expect(result).toEqual([]);
    });
  });
});
