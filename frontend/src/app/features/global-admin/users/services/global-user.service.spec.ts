import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GlobalUserService } from './global-user.service';
import { GlobalUserPage } from '../models/global-user.model';

describe('GlobalUserService', () => {
  let service: GlobalUserService;
  let httpMock: HttpTestingController;
  const BASE_URL = '/api/v1/global-admin/users';
  const emptyPage: GlobalUserPage = { content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(GlobalUserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends paging and sort params only when no filters are set', () => {
    service.listUsers({ page: 0, size: 20, sort: 'createdAt,desc' }).subscribe();
    const req = httpMock.expectOne((r) => r.url === BASE_URL);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('sort')).toBe('createdAt,desc');
    expect(req.request.params.has('role')).toBeFalse();
    expect(req.request.params.has('active')).toBeFalse();
    expect(req.request.params.has('search')).toBeFalse();
    req.flush(emptyPage);
  });

  it('includes role, active and trimmed search filters', () => {
    service
      .listUsers({ page: 1, size: 10, sort: 'email,asc', role: 'THERAPIST', active: false, search: '  ann ' })
      .subscribe();
    const req = httpMock.expectOne((r) => r.url === BASE_URL);
    expect(req.request.params.get('role')).toBe('THERAPIST');
    expect(req.request.params.get('active')).toBe('false');
    expect(req.request.params.get('search')).toBe('ann');
    req.flush(emptyPage);
  });

  it('suspends a user with POST /{id}/suspension', () => {
    service.suspendUser('u1').subscribe();
    const req = httpMock.expectOne(`${BASE_URL}/u1/suspension`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('reactivates a user with DELETE /{id}/suspension', () => {
    service.reactivateUser('u1').subscribe();
    const req = httpMock.expectOne(`${BASE_URL}/u1/suspension`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('deletes a user with DELETE /{id}', () => {
    service.deleteUser('u1').subscribe();
    const req = httpMock.expectOne(`${BASE_URL}/u1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('loads summary stats with GET /stats', () => {
    let result: unknown;
    service.getStats().subscribe((r) => (result = r));
    const req = httpMock.expectOne(`${BASE_URL}/stats`);
    expect(req.request.method).toBe('GET');
    req.flush({ total: 10, online: 3, blocked: 2 });
    expect(result).toEqual({ total: 10, online: 3, blocked: 2 });
  });
});
