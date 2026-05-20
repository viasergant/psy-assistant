import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { RiskFlagsPanelComponent } from './risk-flags-panel.component';
import { RiskFlagService } from '../services/risk-flag.service';
import { RiskFlag } from '../models/risk-flag.model';

const ACTIVE_FLAG: RiskFlag = {
  id: 'flag-1',
  clientId: 'client-1',
  flagTypeId: 'type-1',
  flagTypeName: 'Self-Harm Risk',
  status: 'ACTIVE',
  clinicalNote: 'Observed in session.',
  reviewDate: '2026-06-01',
  createdByUserId: 'user-1',
  createdAt: '2026-05-13T10:00:00Z',
  resolvedByUserId: null,
  resolvedAt: null,
  resolutionNote: null,
};

const RESOLVED_FLAG: RiskFlag = {
  ...ACTIVE_FLAG,
  id: 'flag-2',
  status: 'RESOLVED',
  resolvedByUserId: 'user-1',
  resolvedAt: '2026-05-14T09:00:00Z',
  resolutionNote: 'Safety plan agreed.',
};

describe('RiskFlagsPanelComponent', () => {
  let component: RiskFlagsPanelComponent;
  let fixture: ComponentFixture<RiskFlagsPanelComponent>;
  let riskFlagServiceSpy: jasmine.SpyObj<RiskFlagService>;

  beforeEach(async () => {
    riskFlagServiceSpy = jasmine.createSpyObj<RiskFlagService>('RiskFlagService', [
      'listActive',
      'listAll',
      'create',
      'resolve',
    ]);

    await TestBed.configureTestingModule({
      imports: [RiskFlagsPanelComponent, TranslocoModule],
      providers: [{ provide: RiskFlagService, useValue: riskFlagServiceSpy }],
    }).compileComponents();
  });

  function createComponent(overrides: { canManage?: boolean; canReadNotes?: boolean } = {}): void {
    fixture = TestBed.createComponent(RiskFlagsPanelComponent);
    component = fixture.componentInstance;
    component.clientId = 'client-1';
    component.canManage = overrides.canManage ?? false;
    component.canReadNotes = overrides.canReadNotes ?? false;
  }

  // --- Loading strategy ---

  it('shouldCallListActive_whenCanReadNotesIsFalse', () => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([]));
    createComponent({ canReadNotes: false });

    // Act
    fixture.detectChanges();

    // Assert
    expect(riskFlagServiceSpy.listActive).toHaveBeenCalledOnceWith('client-1');
    expect(riskFlagServiceSpy.listAll).not.toHaveBeenCalled();
  });

  it('shouldCallListAll_whenCanReadNotesIsTrue', () => {
    // Arrange
    riskFlagServiceSpy.listAll.and.returnValue(of([]));
    createComponent({ canReadNotes: true });

    // Act
    fixture.detectChanges();

    // Assert
    expect(riskFlagServiceSpy.listAll).toHaveBeenCalledOnceWith('client-1');
    expect(riskFlagServiceSpy.listActive).not.toHaveBeenCalled();
  });

  it('shouldFilterToActiveOnly_whenCanReadNotesIsTrueAndBothStatusesReturned', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listAll.and.returnValue(of([ACTIVE_FLAG, RESOLVED_FLAG]));
    createComponent({ canReadNotes: true });
    fixture.detectChanges();
    tick();

    // Assert — only the active flag should be in the list
    expect(component.flags.length).toBe(1);
    expect(component.flags[0].status).toBe('ACTIVE');
  }));

  // --- Display ---

  it('shouldDisplayActiveFlagCard_whenActiveFlagIsLoaded', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([ACTIVE_FLAG]));
    createComponent();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const chip = fixture.debugElement.query(By.css('.flag-type-chip'));
    expect(chip).toBeTruthy();
    expect(chip.nativeElement.textContent.trim()).toBe('Self-Harm Risk');
  }));

  it('shouldShowEmptyState_whenNoFlagsAreReturned', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([]));
    createComponent();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const emptyState = fixture.debugElement.query(By.css('.empty-state'));
    expect(emptyState).toBeTruthy();
  }));

  it('shouldShowClinicalNote_whenCanReadNotesIsTrueAndNoteIsPresent', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listAll.and.returnValue(of([ACTIVE_FLAG]));
    createComponent({ canReadNotes: true });
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const noteBlock = fixture.debugElement.query(By.css('.clinical-note-block'));
    expect(noteBlock).toBeTruthy();
  }));

  it('shouldHideClinicalNote_whenCanReadNotesIsFalse', fakeAsync(() => {
    // Arrange
    const flagWithoutNote: RiskFlag = { ...ACTIVE_FLAG, clinicalNote: null };
    riskFlagServiceSpy.listActive.and.returnValue(of([flagWithoutNote]));
    createComponent({ canReadNotes: false });
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const noteBlock = fixture.debugElement.query(By.css('.clinical-note-block'));
    expect(noteBlock).toBeNull();
  }));

  // --- Add-flag button visibility ---

  it('shouldShowAddFlagButton_whenCanManageIsTrue', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([]));
    createComponent({ canManage: true });
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const addButton = fixture.debugElement.query(By.css('.btn-primary'));
    expect(addButton).toBeTruthy();
  }));

  it('shouldHideAddFlagButton_whenCanManageIsFalse', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([]));
    createComponent({ canManage: false });
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const addButton = fixture.debugElement.query(By.css('.btn-primary'));
    expect(addButton).toBeNull();
  }));

  // --- Resolve button visibility ---

  it('shouldShowResolveButton_whenCanManageIsTrueAndFlagIsActive', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([ACTIVE_FLAG]));
    createComponent({ canManage: true });
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const resolveButton = fixture.debugElement.query(By.css('.btn-resolve'));
    expect(resolveButton).toBeTruthy();
  }));

  it('shouldHideResolveButton_whenCanManageIsFalse', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([ACTIVE_FLAG]));
    createComponent({ canManage: false });
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const resolveButton = fixture.debugElement.query(By.css('.btn-resolve'));
    expect(resolveButton).toBeNull();
  }));

  // --- Dialog state management ---

  it('shouldSetShowAddDialogTrue_whenOpenAddDialogIsCalled', () => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([]));
    createComponent({ canManage: true });
    fixture.detectChanges();

    // Act
    component.openAddDialog();

    // Assert
    expect(component.showAddDialog).toBeTrue();
  });

  it('shouldReloadFlagsAndCloseDialog_whenFlagIsAdded', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([]));
    createComponent({ canManage: true });
    fixture.detectChanges();
    component.showAddDialog = true;

    // Act
    component.onFlagAdded();
    tick();

    // Assert
    expect(component.showAddDialog).toBeFalse();
    expect(riskFlagServiceSpy.listActive).toHaveBeenCalledTimes(2);
  }));

  it('shouldSetResolvingFlagIdAndShowResolveDialog_whenOpenResolveDialogIsCalled', () => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([ACTIVE_FLAG]));
    createComponent({ canManage: true });
    fixture.detectChanges();

    // Act
    component.openResolveDialog(ACTIVE_FLAG);

    // Assert
    expect(component.resolvingFlagId).toBe('flag-1');
    expect(component.showResolveDialog).toBeTrue();
  });

  it('shouldClearResolvingFlagIdAndCloseDialog_whenCloseResolveDialogIsCalled', () => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([ACTIVE_FLAG]));
    createComponent({ canManage: true });
    fixture.detectChanges();
    component.openResolveDialog(ACTIVE_FLAG);

    // Act
    component.closeResolveDialog();

    // Assert
    expect(component.resolvingFlagId).toBeNull();
    expect(component.showResolveDialog).toBeFalse();
  });

  it('shouldReloadFlagsAndCloseDialog_whenFlagIsResolved', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(of([ACTIVE_FLAG]));
    createComponent({ canManage: true });
    fixture.detectChanges();
    component.openResolveDialog(ACTIVE_FLAG);

    // Act
    component.onFlagResolved();
    tick();

    // Assert
    expect(component.showResolveDialog).toBeFalse();
    expect(riskFlagServiceSpy.listActive).toHaveBeenCalledTimes(2);
  }));

  // --- Error handling ---

  it('shouldSetErrorMessage_whenLoadFlagsFails', fakeAsync(() => {
    // Arrange
    riskFlagServiceSpy.listActive.and.returnValue(throwError(() => new Error('network error')));
    createComponent();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    expect(component.error).toBeTruthy();
    expect(component.loading).toBeFalse();
  }));
});
