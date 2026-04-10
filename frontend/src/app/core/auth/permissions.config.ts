export type AppRole = 'THERAPIST' | 'FINANCE' | 'RECEPTION_ADMIN_STAFF' | 'SYSTEM_ADMINISTRATOR' | 'SUPERVISOR';

export const ROUTE_ROLES: Readonly<Record<string, AppRole[]>> = {
  leads:    ['SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF'],
  clients:  ['THERAPIST', 'SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF'],
  schedule: ['THERAPIST', 'SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF'],
  sessions: ['THERAPIST', 'SYSTEM_ADMINISTRATOR'],
  billing:  ['FINANCE', 'SYSTEM_ADMINISTRATOR'],
  reports:  ['SUPERVISOR', 'FINANCE', 'SYSTEM_ADMINISTRATOR', 'THERAPIST'],
  admin:    ['SYSTEM_ADMINISTRATOR'],
} as const;

export type PermissionKey =
  | 'VIEW_BILLING_ACTIONS'
  | 'VIEW_SESSION_NOTES'
  | 'BOOK_APPOINTMENT'
  | 'MANAGE_USERS'
  | 'VIEW_REPORTS';

export const PERMISSIONS: Readonly<Record<PermissionKey, AppRole[]>> = {
  VIEW_BILLING_ACTIONS: ['FINANCE', 'SYSTEM_ADMINISTRATOR'],
  VIEW_SESSION_NOTES:   ['THERAPIST', 'SYSTEM_ADMINISTRATOR'],
  BOOK_APPOINTMENT:     ['THERAPIST', 'SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF'],
  MANAGE_USERS:         ['SYSTEM_ADMINISTRATOR'],
  VIEW_REPORTS:         ['SUPERVISOR', 'FINANCE', 'SYSTEM_ADMINISTRATOR'],
} as const;

export interface NavItem {
  labelKey: string;
  route: string;
  roles: AppRole[];
  icon?: string;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { labelKey: 'nav.clients',  route: '/clients',  roles: ROUTE_ROLES['clients'],  icon: 'pi-users' },
  { labelKey: 'nav.leads',    route: '/leads',    roles: ROUTE_ROLES['leads'],    icon: 'pi-user-plus' },
  { labelKey: 'nav.schedule', route: '/schedule', roles: ROUTE_ROLES['schedule'], icon: 'pi-calendar' },
  { labelKey: 'nav.sessions', route: '/sessions', roles: ROUTE_ROLES['sessions'], icon: 'pi-comments' },
  { labelKey: 'nav.billing',  route: '/billing',  roles: ROUTE_ROLES['billing'],  icon: 'pi-wallet' },
  { labelKey: 'nav.reports',  route: '/reports',  roles: ROUTE_ROLES['reports'],  icon: 'pi-chart-bar' },
  { labelKey: 'nav.admin',    route: '/admin',    roles: ROUTE_ROLES['admin'],    icon: 'pi-cog' },
] as const;
