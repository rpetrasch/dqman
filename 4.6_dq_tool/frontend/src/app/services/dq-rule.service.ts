import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DqRule } from '../models/dq-rule.model';

@Injectable({
  providedIn: 'root'
})
export class DqRuleService {
  private apiUrl = 'http://localhost:8081/api/rules';

  constructor(private http: HttpClient) { }

  getAllRules(): Observable<DqRule[]> {
    return this.http.get<DqRule[]>(this.apiUrl);
  }

  createRule(rule: DqRule): Observable<DqRule> {
    return this.http.post<DqRule>(this.apiUrl, rule);
  }

  updateRule(id: number, rule: DqRule): Observable<DqRule> {
    return this.http.put<DqRule>(`${this.apiUrl}/${id}`, rule);
  }

  deleteRule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
