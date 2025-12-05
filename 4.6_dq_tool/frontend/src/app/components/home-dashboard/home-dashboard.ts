import { Component, OnInit, ViewChildren, QueryList, ChangeDetectorRef } from '@angular/core';
import { DqProject } from '../../models/dq-project.model';
import { DqProjectService } from '../../services/dq-project.service';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatCardHeader } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { Chart, ArcElement, Tooltip, Legend, PieController, BarController, BarElement, CategoryScale, LinearScale } from 'chart.js';

// Register Chart.js components
Chart.register(ArcElement, Tooltip, Legend, PieController, BarController, BarElement, CategoryScale, LinearScale);

@Component({
  selector: 'app-home-dashboard',
  imports: [MatTableModule, MatCardModule, MatIconModule, CommonModule, BaseChartDirective],
  templateUrl: './home-dashboard.html',
  styleUrl: './home-dashboard.css'
})
export class HomeDashboardComponent implements OnInit {
  @ViewChildren(BaseChartDirective) charts?: QueryList<BaseChartDirective>;
  projects: DqProject[] = [];
  displayedColumns: string[] = ['name', 'startedDate', 'finishedDate', 'status'];

  // Pie chart data
  public pieChartData: ChartData<'pie'> = {
    labels: ['Successful', 'Failed', 'In Progress'],
    datasets: [{
      data: [0, 0, 0],
      backgroundColor: ['#4CAF50', '#F44336', '#FFC107']
    }]
  };

  public pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      },
      title: {
        display: true,
        text: 'Project Status Distribution'
      }
    }
  };

  // Bar chart data for last 7 days
  public barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        label: 'Successful',
        data: [],
        backgroundColor: '#4CAF50'
      },
      {
        label: 'Failed',
        data: [],
        backgroundColor: '#F44336'
      }
    ]
  };

  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      },
      title: {
        display: true,
        text: 'Projects Trend (Last 7 Days)'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 1
        }
      }
    }
  };

  constructor(
    private dqProjectService: DqProjectService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.dqProjectService.getAllProjects().subscribe((data: DqProject[]) => {
      this.projects = data;
      this.updateChartData();
      this.updateBarChartData();
      this.charts?.forEach(c => c.update());
      this.cdr.detectChanges();
    });
  }

  updateChartData(): void {
    const successCount = this.projects.filter(p => p.status === 'SUCCESS').length;
    const failedCount = this.projects.filter(p => p.status === 'FAILED').length;
    const inProgressCount = this.projects.filter(p => p.status === 'STARTED').length;

    this.pieChartData = {
      labels: ['Successful', 'Failed', 'In Progress'],
      datasets: [{
        data: [successCount, failedCount, inProgressCount],
        backgroundColor: ['#4CAF50', '#F44336', '#FFC107']
      }]
    };
  }

  updateBarChartData(): void {
    // Get last 7 days
    const last7Days: string[] = [];
    const successData: number[] = [];
    const failedData: number[] = [];

    for (let i = 6; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dateStr = date.toISOString().split('T')[0]; // YYYY-MM-DD format
      last7Days.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));

      // Count SUCCESS and FAILED projects for this day
      const dayProjects = this.projects.filter(p => {
        if (!p.finishedDate) return false;
        const projectDate = new Date(p.finishedDate).toISOString().split('T')[0];
        return projectDate === dateStr;
      });

      successData.push(dayProjects.filter(p => p.status === 'SUCCESS').length);
      failedData.push(dayProjects.filter(p => p.status === 'FAILED').length);
    }

    this.barChartData = {
      labels: last7Days,
      datasets: [
        {
          label: 'Successful',
          data: successData,
          backgroundColor: '#4CAF50'
        },
        {
          label: 'Failed',
          data: failedData,
          backgroundColor: '#F44336'
        }
      ]
    };
  }
}
