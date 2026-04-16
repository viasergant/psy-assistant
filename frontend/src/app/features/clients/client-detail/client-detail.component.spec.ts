import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { ClientDetailComponent } from './client-detail.component';
import { ClientService } from '../services/client.service';
import { ClientDetail } from '../models/client.model';

describe('ClientDetailComponent', () => {
  let fixture: ComponentFixture<ClientDetailComponent>;
  let component: ClientDetailComponent;
  let clientServiceSpy: jasmine.SpyObj<ClientService>;
  let revokeObjectUrlSpy: jasmine.Spy;

  const baseClient: ClientDetail = {
    id: '11111111-1111-1111-1111-111111111111',
    fullName: 'Anna Kovalenko',
    clientCode: 'CL-11111111',
    version: 3,
    sourceLeadId: null,
    ownerId: null,
    assignedTherapistId: null,
    notes: 'Initial notes',
    preferredName: null,
    dateOfBirth: null,
    sexOrGender: null,
    pronouns: null,
    email: null,
    phone: null,
    secondaryPhone: null,
    addressLine1: null,
    addressLine2: null,
    city: null,
    region: null,
    postalCode: null,
    country: null,
    referralSource: null,
    referralContactName: null,
    referralNotes: null,
    preferredCommunicationMethod: null,
    allowPhone: false,
    allowSms: false,
    allowEmail: false,
    allowVoicemail: false,
    emergencyContactName: null,
    emergencyContactRelationship: null,
    emergencyContactPhone: null,
    emergencyContactEmail: null,
    tags: ['adult'],
    photoUrl: null,
    canEditProfile: true,
    canEditTags: true,
    canUploadPhoto: true,
    isAtRisk: false,
    createdAt: '2026-03-30T00:00:00Z',
    updatedAt: '2026-03-30T00:00:00Z',
    createdBy: 'system'
  };

  beforeEach(async () => {
    localStorage.clear();

    clientServiceSpy = jasmine.createSpyObj<ClientService>('ClientService', [
      'getClient',
      'updateClient',
      'updateTags',
      'uploadPhoto',
      'getPhoto'
    ]);
    clientServiceSpy.getClient.and.returnValue(of(baseClient));
    clientServiceSpy.updateClient.and.callFake((_id, payload) =>
      of({ ...baseClient, fullName: payload.fullName, version: 4 })
    );
    clientServiceSpy.updateTags.and.callFake((_id, payload) =>
      of({ ...baseClient, tags: payload.tags, version: 4 })
    );
    clientServiceSpy.uploadPhoto.and.returnValue(
      of({ ...baseClient, photoUrl: '/api/v1/clients/111/photo', version: 4 })
    );
    clientServiceSpy.getPhoto.and.returnValue(of(new Blob(['photo'], { type: 'image/png' })));

    spyOn(URL, 'createObjectURL').and.returnValue('blob:test-photo');
    revokeObjectUrlSpy = spyOn(URL, 'revokeObjectURL').and.stub();

    await TestBed.configureTestingModule({
      imports: [ClientDetailComponent],
      providers: [
        { provide: ClientService, useValue: clientServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (_name: string) => baseClient.id
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('loads client profile on init', () => {
    expect(clientServiceSpy.getClient).toHaveBeenCalledWith(baseClient.id);
    expect(component.client?.fullName).toBe('Anna Kovalenko');
  });

  it('saves edited profile through update api', () => {
    component.startEdit();
    component.profileForm.patchValue({ fullName: 'Anna Updated' });

    component.save();

    expect(clientServiceSpy.updateClient).toHaveBeenCalled();
    expect(component.client?.fullName).toBe('Anna Updated');
    expect(component.editing).toBeFalse();
  });

  it('adds and saves tags through tags api', () => {
    component.tagInput = 'priority';
    component.addTag();
    component.saveTags();

    expect(clientServiceSpy.updateTags).toHaveBeenCalled();
    expect(component.client?.tags).toContain('priority');
  });

  it('returns filtered tag hints and applies selected hint', () => {
    component.tagInput = 'tr';

    expect(component.filteredTagHints).toContain('trauma');

    component.applyTagHint('trauma');

    expect(component.client?.tags).toContain('trauma');
    expect(component.tagInput).toBe('');
  });

  it('rejects oversize photo before upload', () => {
    const bigBlob = new Blob([new Uint8Array(5 * 1024 * 1024 + 1)], { type: 'image/png' });
    const bigFile = new File([bigBlob], 'large.png', { type: 'image/png' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [bigFile] });

    component.onPhotoSelected({ target: input } as unknown as Event);

    expect(clientServiceSpy.uploadPhoto).not.toHaveBeenCalled();
    expect(component.photoError).toContain('5 MB');
  });

  it('uploads valid photo through upload api', () => {
    const file = new File([new Blob(['ok'], { type: 'image/png' })], 'avatar.png', {
      type: 'image/png'
    });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });

    component.onPhotoSelected({ target: input } as unknown as Event);

    expect(clientServiceSpy.uploadPhoto).toHaveBeenCalled();
    expect(clientServiceSpy.getPhoto).toHaveBeenCalledWith(baseClient.id);
    expect(component.photoSrc).toBe('blob:test-photo');
    expect(component.client?.photoUrl).toBe('/api/v1/clients/111/photo');
  });

  it('revokes object url on destroy', () => {
    component['photoSrc'] = 'blob:test-photo';
    component['photoObjectUrl'] = 'blob:test-photo';

    component.ngOnDestroy();

    expect(revokeObjectUrlSpy).toHaveBeenCalledWith('blob:test-photo');
  });
});
