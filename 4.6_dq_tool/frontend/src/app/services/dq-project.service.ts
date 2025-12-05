import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DqProject } from '../models/dq-project.model';

@Injectable({
  providedIn: 'root'
})
export class DqProjectService {
  private apiUrl = 'http://localhost:8081/api/projects';

  constructor(private http: HttpClient) { }

  getAllProjects(): Observable<DqProject[]> {
    return this.http.get<DqProject[]>(this.apiUrl);
  }

  createProject(project: DqProject): Observable<DqProject> {
    return this.http.post<DqProject>(this.apiUrl, project);
  }

  updateProject(id: number, project: DqProject): Observable<DqProject> {
    return this.http.put<DqProject>(`${this.apiUrl}/${id}`, project);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
