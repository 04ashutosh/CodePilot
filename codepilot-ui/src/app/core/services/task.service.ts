import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task, TaskPriority } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private readonly API_URL = '/api/tasks';
  private readonly PROJECT_API_URL = '/api/projects';

  constructor(private http: HttpClient) {}

  public getProjectTasks(projectId: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.PROJECT_API_URL}/${projectId}/tasks`);
  }

  public createProjectTask(projectId: string, title: string, description: string, priority: TaskPriority, repositoryId?: string): Observable<Task> {
    return this.http.post<Task>(`${this.PROJECT_API_URL}/${projectId}/tasks`, {
      title,
      description,
      priority,
      repositoryId
    });
  }

  public getTaskById(id: string): Observable<Task> {
    return this.http.get<Task>(`${this.API_URL}/${id}`);
  }

  public cancelTask(id: string): Observable<Task> {
    return this.http.put<Task>(`${this.API_URL}/${id}/cancel`, {});
  }
}