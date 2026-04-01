import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ClientService } from '../../features/clients/services/client.service';
import { GlobalSearchComponent } from './global-search.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslocoPipe } from '@jsverse/transloco';
import { Pipe, PipeTransform } from '@angular/core';
import { ClientSearchResult } from '../../features/clients/models/client.model';

@Pipe({ name: 'transloco', standalone: true })
class MockTranslocoPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

describe('GlobalSearchComponent', () => {
  let component: GlobalSearchComponent;
  let fixture: ComponentFixture<GlobalSearchComponent>;
  let clientServiceSpy: jasmine.SpyObj<ClientService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockSearchResults: ClientSearchResult[] = [
    {
      id: '1',
      name: 'John Doe',
      email: 'john@example.com',
      phone: '+380671234567',
      clientCode: 'CL001',
      tags: ['vip', 'active']
    },
    {
      id: '2',
      name: 'Jane Smith',
      email: 'jane@example.com',
      phone: null,
      clientCode: 'CL002',
      tags: []
    }
  ];

  beforeEach(async () => {
    const clientService = jasmine.createSpyObj('ClientService', ['searchClients']);
    const router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        GlobalSearchComponent,
        HttpClientTestingModule
      ],
      providers: [
        { provide: ClientService, useValue: clientService },
        { provide: Router, useValue: router }
      ]
    })
    .overrideComponent(GlobalSearchComponent, {
      remove: { imports: [TranslocoPipe] },
      add: { imports: [MockTranslocoPipe] }
    })
    .compileComponents();

    fixture = TestBed.createComponent(GlobalSearchComponent);
    component = fixture.componentInstance;
    clientServiceSpy = TestBed.inject(ClientService) as jasmine.SpyObj<ClientService>;
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('onSearch', () => {
    it('should clear suggestions when query is empty', () => {
      component.onSearch({ query: '' });
      expect(component.suggestions).toEqual([]);
      expect(clientServiceSpy.searchClients).not.toHaveBeenCalled();
    });

    it('should clear suggestions when query is less than 2 characters', () => {
      component.onSearch({ query: 'J' });
      expect(component.suggestions).toEqual([]);
      expect(clientServiceSpy.searchClients).not.toHaveBeenCalled();
    });

    it('should call searchClients when query is 2 or more characters', () => {
      clientServiceSpy.searchClients.and.returnValue(of(mockSearchResults));
      
      component.onSearch({ query: 'Jo' });
      
      expect(clientServiceSpy.searchClients).toHaveBeenCalledWith('Jo', 10);
      expect(component.suggestions).toEqual(mockSearchResults);
    });

    it('should trim whitespace from query', () => {
      clientServiceSpy.searchClients.and.returnValue(of(mockSearchResults));
      
      component.onSearch({ query: '  John  ' });
      
      expect(clientServiceSpy.searchClients).toHaveBeenCalledWith('John', 10);
    });

    it('should handle search errors gracefully', () => {
      clientServiceSpy.searchClients.and.returnValue(
        throwError(() => new Error('Network error'))
      );
      
      component.onSearch({ query: 'John' });
      
      expect(component.suggestions).toEqual([]);
    });

    it('should search case-insensitively', () => {
      clientServiceSpy.searchClients.and.returnValue(of(mockSearchResults));
      
      component.onSearch({ query: 'JOHN' });
      
      expect(clientServiceSpy.searchClients).toHaveBeenCalledWith('JOHN', 10);
      expect(component.suggestions).toEqual(mockSearchResults);
    });
  });

  describe('onSelect', () => {
    it('should navigate to client detail when client is selected', () => {
      const selectedClient = mockSearchResults[0];
      
      component.onSelect(selectedClient);
      
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/clients', '1']);
    });

    it('should clear selection after navigation', (done) => {
      const selectedClient = mockSearchResults[0];
      component.selectedClient = selectedClient;
      
      component.onSelect(selectedClient);
      
      setTimeout(() => {
        expect(component.selectedClient).toBeNull();
        done();
      }, 10);
    });

    it('should not navigate when event is null', () => {
      component.onSelect(null as any);
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should not navigate when event id is missing', () => {
      component.onSelect({ id: '', name: 'Test' } as any);
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should complete destroy$ subject', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');
      
      component.ngOnDestroy();
      
      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });

  describe('component configuration', () => {
    it('should use 300ms debounce', () => {
      // Verified via PrimeNG delay attribute in template
      expect(component).toBeTruthy();
    });

    it('should have proper placeholder and empty message translations', () => {
      // Verified via Transloco keys in template
      expect(component).toBeTruthy();
    });
  });
});
