import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslocoService } from '@jsverse/transloco';
import { I18nService } from './i18n.service';
import { of } from 'rxjs';

describe('I18nService', () => {
  let service: I18nService;
  let translocoService: jasmine.SpyObj<TranslocoService>;

  beforeEach(() => {
    const translocoSpy = jasmine.createSpyObj('TranslocoService', [
      'setActiveLang',
      'getActiveLang',
      'load'
    ]);
    translocoSpy.load.and.returnValue(of({}));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        I18nService,
        { provide: TranslocoService, useValue: translocoSpy }
      ]
    });

    service = TestBed.inject(I18nService);
    translocoService = TestBed.inject(TranslocoService) as jasmine.SpyObj<TranslocoService>;

    // Clear cookies before each test
    document.cookie = 'pa_locale=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
  });

  describe('initialize', () => {
    it('should detect and set locale from cookie', async () => {
      // Set cookie
      document.cookie = 'pa_locale=uk; path=/';
      translocoService.getActiveLang.and.returnValue('uk');

      await service.initialize();

      expect(translocoService.setActiveLang).toHaveBeenCalledWith('uk');
      expect(translocoService.load).toHaveBeenCalledWith('uk');
    });

    it('should use browser language when cookie not present', async () => {
      // Mock navigator.language
      Object.defineProperty(window.navigator, 'language', {
        value: 'uk-UA',
        configurable: true
      });
      translocoService.getActiveLang.and.returnValue('uk');

      await service.initialize();

      expect(translocoService.setActiveLang).toHaveBeenCalledWith('uk');
    });

    it('should default to "en" when cookie and browser language unsupported', async () => {
      Object.defineProperty(window.navigator, 'language', {
        value: 'fr-FR',
        configurable: true
      });
      translocoService.getActiveLang.and.returnValue('en');

      await service.initialize();

      expect(translocoService.setActiveLang).toHaveBeenCalledWith('en');
    });
  });

  describe('setLanguage', () => {
    it('should update Transloco active language and write cookie', () => {
      translocoService.getActiveLang.and.returnValue('uk');

      service.setLanguage('uk');

      expect(translocoService.setActiveLang).toHaveBeenCalledWith('uk');
      expect(document.cookie).toContain('pa_locale=uk');
    });

    it('should fall back to "en" for unsupported locale', () => {
      translocoService.getActiveLang.and.returnValue('en');

      service.setLanguage('fr');

      expect(translocoService.setActiveLang).toHaveBeenCalledWith('en');
      expect(document.cookie).toContain('pa_locale=en');
    });
  });

  describe('getCurrentLocale', () => {
    it('should return current active locale from Transloco', () => {
      translocoService.getActiveLang.and.returnValue('uk');

      const locale = service.getCurrentLocale();

      expect(locale).toBe('uk');
    });
  });

  describe('getSupportedLocales', () => {
    it('should return array of supported locales', () => {
      const locales = service.getSupportedLocales();

      expect(locales).toEqual(['en', 'uk']);
    });
  });
});
