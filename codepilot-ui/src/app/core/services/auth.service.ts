import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse, User, Workspace } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = '/api/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        this.currentUserSubject.next(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('user');
      }
    }
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  public get token(): string | null {
    return localStorage.getItem('access_token');
  }

  public get refreshToken(): string | null {
    return localStorage.getItem('refresh_token');
  }

  public register(username: string, email: string, password: string, fullName: string): Observable<User> {
    return this.http.post<User>(`${this.API_URL}/register`, {
      username,
      email,
      password,
      fullName
    });
  }

  public getWorkspaces(): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(`${this.API_URL}/workspaces`);
  }

  public login(usernameOrEmail: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, {
      usernameOrEmail,
      password
    }).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  public refresh(): Observable<AuthResponse> {
    const token = this.refreshToken;
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, {
      refresh_token: token
    }).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  public logout(): void {
    const token = this.refreshToken;
    if (token) {
      this.http.post(`${this.API_URL}/logout`, { refresh_token: token }).subscribe({
        next: () => this.clearSession(),
        error: () => this.clearSession()
      });
    } else {
      this.clearSession();
    }
  }

  private handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem('access_token', response.access_token);
    localStorage.setItem('refresh_token', response.refresh_token);
    localStorage.setItem('user', JSON.stringify(response.user));
    this.currentUserSubject.next(response.user);
  }

  private clearSession(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
    this.currentUserSubject.next(null);
    window.location.href = '/login';
  }
}