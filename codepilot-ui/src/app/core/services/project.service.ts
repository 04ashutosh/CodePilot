import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly API_URL = '/api/projects';

  constructor(private http: HttpClient) {}

  public getProjects(workspaceId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.API_URL}?workspaceId=${workspaceId}`);
  }

  public createProject(name: string, description: string, workspaceId: string): Observable<Project> {
    return this.http.post<Project>(this.API_URL, {
      name,
      description,
      workspaceId
    });
  }

  public getProjectById(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.API_URL}/${id}`);
  }

  public updateProject(id: string, name: string, description: string, workspaceId: string): Observable<Project> {
    return this.http.put<Project>(`${this.API_URL}/${id}`, {
      name,
      description,
      workspaceId
    });
  }

  public deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}