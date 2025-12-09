import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DataIntegration } from '../models/data-integration.model';

@Injectable({
    providedIn: 'root'
})
export class DataIntegrationService {
    private apiUrl = 'http://localhost:8081/api/integrations';

    constructor(private http: HttpClient) { }

    getAllIntegrations(): Observable<DataIntegration[]> {
        return this.http.get<DataIntegration[]>(this.apiUrl);
    }

    createIntegration(integration: DataIntegration): Observable<DataIntegration> {
        return this.http.post<DataIntegration>(this.apiUrl, integration);
    }

    updateIntegration(id: number, integration: DataIntegration): Observable<DataIntegration> {
        return this.http.put<DataIntegration>(`${this.apiUrl}/${id}`, integration);
    }

    deleteIntegration(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    getMetadata(id: number): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/metadata/${id}`);
    }
}
