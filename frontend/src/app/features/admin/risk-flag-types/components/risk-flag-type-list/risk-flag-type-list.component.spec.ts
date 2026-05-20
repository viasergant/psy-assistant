import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { RiskFlagTypeListComponent } from './risk-flag-type-list.component';
import { RiskFlagTypeAdminService } from '../../services/risk-flag-type-admin.service';
import { RiskFlagType } from '../../models/risk-flag-type-admin.model';

const ACTIVE_FLAG_TYPE: RiskFlagType = {
  id: 'type-uuid-1',
  name: 'Self-Harm Risk',
  displayOrder: 1,
  active: true,
};

const INACTIVE_FLAG_TYPE: RiskFlagType = {
  id: 'type-uuid-2',
  name: 'Crisis History',
  displayOrder: 2,
  active: false,
};

describe('RiskFlagTypeListComponent', () => {
  let component: RiskFlagTypeListComponent;
  let fixture: ComponentFixture<RiskFlagTypeListComponent>;
  let svcSpy: jasmine.SpyObj<RiskFlagTypeAdminService>;

  beforeEach(async () => {
    svcSpy = jasmine.createSpyObj<RiskFlagTypeAdminService>('RiskFlagTypeAdminService', [
      'listAll',
      'deactivate',
    ]);
    svcSpy.listAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [RiskFlagTypeListComponent, TranslocoModule],
      providers: [{ provide: RiskFlagTypeAdminService, useValue: svcSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RiskFlagTypeListComponent);
    component = fixture.componentInstance;
  });

  // --- Initialisation ---

  it('shouldCallListAllOnInit_whenComponentCreated', () => {
    // Act
    fixture.detectChanges();

    // Assert
    expect(svcSpy.listAll).toHaveBeenCalledOnceWith();
  });

  it('shouldPopulateFlagTypes_whenListAllSucceeds', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([ACTIVE_FLAG_TYPE, INACTIVE_FLAG_TYPE]));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.flagTypes.length).toBe(2);
    expect(component.loading).toBe(false);
    expect(component.loadError).toBeNull();
  });

  it('shouldSetLoadError_whenListAllFails', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(throwError(() => new Error('network error')));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.loadError).toBeTruthy();
    expect(component.loading).toBe(false);
    expect(component.flagTypes.length).toBe(0);
  });

  // --- Template rendering ---

  it('shouldRenderTableRows_whenFlagTypesAreLoaded', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([ACTIVE_FLAG_TYPE]));

    // Act
    fixture.detectChanges();

    // Assert
    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows.length).toBe(1);
  });

  it('shouldShowEmptyMessage_whenNoFlagTypesExist', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([]));

    // Act
    fixture.detectChanges();

    // Assert
    const table = fixture.debugElement.query(By.css('table'));
    expect(table).toBeNull();
  });

  it('shouldShowDeactivateButtonOnlyForActiveFlagTypes_whenFlagTypesIncludeMixed', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([ACTIVE_FLAG_TYPE, INACTIVE_FLAG_TYPE]));

    // Act
    fixture.detectChanges();

    // Assert
    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    const activeRowActions = rows[0].queryAll(By.css('button'));
    const inactiveRowActions = rows[1].queryAll(By.css('button'));
    expect(activeRowActions.length).toBe(1);
    expect(inactiveRowActions.length).toBe(0);
  });

  // --- Deactivate action ---

  it('shouldCallDeactivateAndReloadList_whenDeactivateButtonIsClicked', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([ACTIVE_FLAG_TYPE]));
    svcSpy.deactivate.and.returnValue(of(undefined));
    fixture.detectChanges();

    // Act
    component.deactivate(ACTIVE_FLAG_TYPE);

    // Assert
    expect(svcSpy.deactivate).toHaveBeenCalledOnceWith(ACTIVE_FLAG_TYPE.id);
    expect(svcSpy.listAll).toHaveBeenCalledTimes(2); // once on init, once after deactivate
  });

  it('shouldSetActionError_whenDeactivateFails', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([ACTIVE_FLAG_TYPE]));
    svcSpy.deactivate.and.returnValue(throwError(() => new Error('server error')));
    fixture.detectChanges();

    // Act
    component.deactivate(ACTIVE_FLAG_TYPE);

    // Assert
    expect(component.actionError).toBeTruthy();
  });

  // --- Form dialog ---

  it('shouldShowFormDialog_whenOpenCreateIsCalled', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([]));
    fixture.detectChanges();

    // Act
    component.openCreate();
    fixture.detectChanges();

    // Assert
    expect(component.showForm).toBe(true);
    const dialog = fixture.debugElement.query(By.css('app-risk-flag-type-form-dialog'));
    expect(dialog).not.toBeNull();
  });

  it('shouldHideFormDialogAndReload_whenOnSavedIsCalled', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([]));
    fixture.detectChanges();
    component.showForm = true;

    // Act
    component.onSaved();

    // Assert
    expect(component.showForm).toBe(false);
    expect(svcSpy.listAll).toHaveBeenCalledTimes(2);
  });

  it('shouldHideFormDialog_whenCancelledEventIsEmitted', () => {
    // Arrange
    svcSpy.listAll.and.returnValue(of([]));
    fixture.detectChanges();
    component.showForm = true;
    fixture.detectChanges();

    // Act
    const dialog = fixture.debugElement.query(By.css('app-risk-flag-type-form-dialog'));
    dialog.triggerEventHandler('cancelled', null);

    // Assert
    expect(component.showForm).toBe(false);
  });
});
