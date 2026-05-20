import { normalizeRole, normalizeRoles, UserRole } from './user.model';

describe('normalizeRole', () => {
  it('shouldReturnSystemAdministrator_whenRoleIsLegacyAdmin', () => {
    expect(normalizeRole('ADMIN')).toBe('SYSTEM_ADMINISTRATOR');
  });

  it('shouldReturnTherapist_whenRoleIsLegacyUser', () => {
    expect(normalizeRole('USER')).toBe('THERAPIST');
  });

  it('shouldReturnRoleAsIs_whenRoleIsCanonical', () => {
    const canonicalRoles: UserRole[] = [
      'RECEPTION_ADMIN_STAFF',
      'THERAPIST',
      'SUPERVISOR',
      'FINANCE',
      'SYSTEM_ADMINISTRATOR',
    ];
    for (const role of canonicalRoles) {
      expect(normalizeRole(role)).toBe(role);
    }
  });
});

describe('normalizeRoles', () => {
  it('shouldReturnEmptyArray_whenInputIsUndefined', () => {
    expect(normalizeRoles(undefined)).toEqual([]);
  });

  it('shouldReturnEmptyArray_whenInputIsEmpty', () => {
    expect(normalizeRoles([])).toEqual([]);
  });

  it('shouldNormalizeLegacyAliases_whenPresentInArray', () => {
    const result = normalizeRoles(['ADMIN', 'USER']);
    expect(result).toEqual(['SYSTEM_ADMINISTRATOR', 'THERAPIST']);
  });

  it('shouldPreserveCanonicalRoles_whenInputIsAlreadyCanonical', () => {
    const input = ['THERAPIST', 'SUPERVISOR'];
    expect(normalizeRoles(input)).toEqual(['THERAPIST', 'SUPERVISOR']);
  });

  it('shouldHandleMixedLegacyAndCanonical_whenBothPresent', () => {
    const result = normalizeRoles(['ADMIN', 'SUPERVISOR', 'USER']);
    expect(result).toEqual(['SYSTEM_ADMINISTRATOR', 'SUPERVISOR', 'THERAPIST']);
  });

  it('shouldReturnSingleElementArray_whenInputHasOneRole', () => {
    expect(normalizeRoles(['FINANCE'])).toEqual(['FINANCE']);
  });
});
