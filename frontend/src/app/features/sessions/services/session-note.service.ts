import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import {
  CreateNoteRequest,
  NoteTemplate,
  NoteVersion,
  SessionNote,
  UpdateNoteRequest,
} from '../models/session-note.model';

/**
 * HTTP client for session note management endpoints.
 *
 * Authentication tokens are appended by the global JwtInterceptor.
 * RBAC is enforced server-side; this service forwards 403 responses as-is
 * so calling components can render an appropriate permission-denied state.
 */
@Injectable({ providedIn: 'root' })
export class SessionNoteService {
  private readonly base = '/api/sessions';
  private readonly templatesBase = '/api/notes/templates';

  constructor(private http: HttpClient) {}

  getNotes(sessionId: string): Observable<SessionNote[]> {
    return this.http
      .get<SessionNote[]>(`${this.base}/${sessionId}/notes`)
      .pipe(catchError(err => throwError(() => err)));
  }

  createNote(sessionId: string, request: CreateNoteRequest): Observable<SessionNote> {
    return this.http.post<SessionNote>(`${this.base}/${sessionId}/notes`, request);
  }

  updateNote(
    sessionId: string,
    noteId: string,
    request: UpdateNoteRequest
  ): Observable<SessionNote> {
    return this.http.put<SessionNote>(`${this.base}/${sessionId}/notes/${noteId}`, request);
  }

  getVersionHistory(sessionId: string, noteId: string): Observable<NoteVersion[]> {
    return this.http.get<NoteVersion[]>(
      `${this.base}/${sessionId}/notes/${noteId}/history`
    );
  }

  getTemplates(): Observable<NoteTemplate[]> {
    return this.http.get<NoteTemplate[]>(this.templatesBase);
  }

  isForbiddenError(err: unknown): boolean {
    return err instanceof HttpErrorResponse && err.status === 403;
  }
}
