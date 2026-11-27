/** Platform contact details as returned by the public and global admin endpoints. */
export interface PlatformContacts {
  phone: string | null;
  email: string | null;
  telegramChannels: string[];
}
