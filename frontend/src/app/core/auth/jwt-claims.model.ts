export interface JwtClaims {
  sub: string;
  /** Backend emits ["ROLE_THERAPIST", ...permissions]. Use extractRole() to get AppRole. */
  roles: string[];
  mustChangePassword?: boolean;
  profileComplete?: boolean;
  exp: number;
  iat: number;
}
