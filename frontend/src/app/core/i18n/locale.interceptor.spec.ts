import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { TranslocoService } from '@jsverse/transloco';
import { localeInterceptor } from './locale.interceptor';
import { of } from 'rxjs';

describe('localeInterceptor', () => {
  let translocoService: jasmine.SpyObj<TranslocoService>;

  beforeEach(() => {
    translocoService = jasmine.createSpyObj('TranslocoService', ['getActiveLang']);
    translocoService.getActiveLang.and.returnValue('uk');

    TestBed.configureTestingModule({
      providers: [
        { provide: TranslocoService, useValue: translocoService }
      ]
    });
  });

  it('should add Accept-Language header to backend requests', (done) => {
    const request = new HttpRequest('GET', '/api/v1/users/me');
    const nextHandler: HttpHandlerFn = (req) => {
      expect(req.headers.get('Accept-Language')).toBe('uk');
      done();
      return of({});
    };

    TestBed.runInInjectionContext(() => {
      localeInterceptor(request, nextHandler).subscribe();
    });
  });

  it('should NOT add header to translation file requests', (done) => {
    const request = new HttpRequest('GET', '/assets/i18n/en.json');
    const nextHandler: HttpHandlerFn = (req) => {
      expect(req.headers.has('Accept-Language')).toBe(false);
      done();
      return of({});
    };

    TestBed.runInInjectionContext(() => {
      localeInterceptor(request, nextHandler).subscribe();
    });
  });

  it('should NOT add header to external URLs', (done) => {
    const request = new HttpRequest('GET', 'https://external.api/data');
    const nextHandler: HttpHandlerFn = (req) => {
      expect(req.headers.has('Accept-Language')).toBe(false);
      done();
      return of({});
    };

    TestBed.runInInjectionContext(() => {
      localeInterceptor(request, nextHandler).subscribe();
    });
  });

  it('should use current active locale from Transloco', (done) => {
    translocoService.getActiveLang.and.returnValue('en');
    const request = new HttpRequest('GET', '/api/v1/schedule');
    const nextHandler: HttpHandlerFn = (req) => {
      expect(req.headers.get('Accept-Language')).toBe('en');
      done();
      return of({});
    };

    TestBed.runInInjectionContext(() => {
      localeInterceptor(request, nextHandler).subscribe();
    });
  });
});
