import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AppModeService } from './app-mode.service';

describe('AppModeService', () => {
  let service: AppModeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AppModeService
      ]
    });
    service = TestBed.inject(AppModeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('init', () => {
    it('sets mode to SOLO when api returns SOLO', async () => {
      // Arrange
      const initPromise = service.init();

      // Act
      const req = httpMock.expectOne('/api/v1/meta');
      req.flush({ mode: 'SOLO', version: '1.0.0' });
      await initPromise;

      // Assert
      expect(service.mode).toBe('SOLO');
    });

    it('sets mode to CLINIC when api returns CLINIC', async () => {
      // Arrange
      const initPromise = service.init();

      // Act
      const req = httpMock.expectOne('/api/v1/meta');
      req.flush({ mode: 'CLINIC', version: '1.0.0' });
      await initPromise;

      // Assert
      expect(service.mode).toBe('CLINIC');
    });

    it('mode remains null when api returns a 500 error', async () => {
      // Arrange
      const initPromise = service.init();

      // Act
      const req = httpMock.expectOne('/api/v1/meta');
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
      await initPromise;

      // Assert
      expect(service.mode).toBeNull();
    });

    it('mode remains null when api returns a network error', async () => {
      // Arrange
      const initPromise = service.init();

      // Act
      const req = httpMock.expectOne('/api/v1/meta');
      req.error(new ProgressEvent('network error'));
      await initPromise;

      // Assert
      expect(service.mode).toBeNull();
    });

    it('mode$ emits the mode value after init resolves', async () => {
      // Arrange
      const emittedValues: Array<string | null> = [];
      const sub = service.mode$.subscribe(m => emittedValues.push(m));
      const initPromise = service.init();

      // Act
      const req = httpMock.expectOne('/api/v1/meta');
      req.flush({ mode: 'CLINIC', version: '2.0.0' });
      await initPromise;

      // Assert
      expect(emittedValues).toContain('CLINIC');
      sub.unsubscribe();
    });
  });

  describe('mode getter', () => {
    it('returns null before init is called', () => {
      expect(service.mode).toBeNull();
    });
  });
});
