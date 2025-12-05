import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DqIntegration } from './dq-integration.model';

@Injectable({
    providedIn: 'root'
})
export class DqIntegrationService {
    private apiUrl = 'http://localhost:8081/api/integrations';

    constructor(private http: HttpClient) { }

    getAllIntegrations(): Observable<DqIntegration[]> {
        return this.http.get<DqIntegration[]>(this.apiUrl);
    }

    createIntegration(integration: DqIntegration): Observable<DqIntegration> {
        return this.http.post<DqIntegration>(this.apiUrl, integration);
    }

    updateIntegration(id: number, integration: DqIntegration): Observable<DqIntegration> {
        return this.http.put<DqIntegration>(`${this.apiUrl}/${id}`, integration);
    }

    deleteIntegration(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
