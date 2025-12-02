import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { MainLayoutComponent } from './layout/main-layout/main-layout';
import { ProjectsComponent } from './projects/projects';
import { IntegrationComponent } from './integration/integration';
import { AlertsComponent } from './alerts/alerts';
import { ReportsComponent } from './reports/reports';
import { KestraComponent } from './kestra/kestra';
import { DqRulesComponent } from './dq-rules/dq-rules';
import { HomeDashboardComponent } from './home-dashboard/home-dashboard';

export const routes: Routes = [
    {
        path: '',
        component: MainLayoutComponent,
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: HomeDashboardComponent },
            { path: 'projects', component: DashboardComponent },
            { path: 'rules', component: DqRulesComponent },
            { path: 'integration', component: IntegrationComponent },
            { path: 'alerts', component: AlertsComponent },
            { path: 'reports', component: ReportsComponent },
            { path: 'kestra', component: KestraComponent }
        ]
    }
];
