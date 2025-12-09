import { Routes } from '@angular/router';
import { MainLayoutComponent } from './components/layout/main-layout/main-layout';
import { ProjectsComponent } from './components/projects/projects';
import { IntegrationComponent } from './components/integration/integration';
import { AlertsComponent } from './components/alerts/alerts';
import { ReportsComponent } from './components/reports/reports';
import { FlowComponent } from './components/flow/flow';
import { DqRulesComponent } from './components/dq-rules/dq-rules';
import { HomeDashboardComponent } from './components/home-dashboard/home-dashboard';

export const routes: Routes = [
    {
        path: '',
        component: MainLayoutComponent,
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: HomeDashboardComponent },
            { path: 'projects', component: ProjectsComponent },
            { path: 'rules', component: DqRulesComponent },
            { path: 'integration', component: IntegrationComponent },
            { path: 'flow', component: FlowComponent },
            { path: 'alerts', component: AlertsComponent },
            { path: 'reports', component: ReportsComponent },
            // { path: 'settings', component: SettingsComponent },
            // { path: 'users', component: UsersComponent },
            // { path: 'roles', component: RolesComponent },
        ]
    }
];
