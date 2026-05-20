export interface JwtClaims {
  sub: string;
  /** Backend emits multiple ROLE_X entries for multi-role users, followed by permission strings. */
  roles: string[];
  mustChangePassword?: boolean;
  profileComplete?: boolean;
  exp: number;
  iat: number;
}
