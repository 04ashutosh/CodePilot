import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { TaskService } from '../../core/services/task.service';
import { Workspace } from '../../core/models/user.model';
import { Project, Task, TaskPriority } from '../../core/models/project.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  workspaces: Workspace[] = [];
  activeWorkspace: Workspace | null = null;
  
  projects: Project[] = [];
  activeProject: Project | null = null;
  
  tasks: Task[] = [];
  activeTask: Task | null = null;

  // New Project Modal Form
  showNewProjectModal = false;
  newProjectName = '';
  newProjectDescription = '';

  // New Task Modal Form
  showNewTaskModal = false;
  newTaskTitle = '';
  newTaskDescription = '';
  newTaskPriority: TaskPriority = 'MEDIUM';

  // State flags
  isLoadingProjects = false;
  isLoadingTasks = false;
  isSubmittingProject = false;
  isSubmittingTask = false;

  // Simulated Steps for visual demonstration of AI agent execution
  simulatedSteps = [
    { name: 'Task Submitted', status: 'COMPLETED', order: 1, desc: 'Task queued for execution.' },
    { name: 'Repo Analysis', status: 'COMPLETED', order: 2, desc: 'Analyzing files, technology stack, and imports.' },
    { name: 'Semantic Search', status: 'COMPLETED', order: 3, desc: 'Searching vector memory for relevant context chunks.' },
    { name: 'Agent Planning', status: 'IN_PROGRESS', order: 4, desc: 'AI Orchestrator drafting code modifications plan.' },
    { name: 'Code Generation', status: 'PENDING', order: 5, desc: 'Applying model edits to generated code chunks.' },
    { name: 'Sandbox Validation', status: 'PENDING', order: 6, desc: 'Running compile checks and test suites in isolated sandbox.' }
  ];

  constructor(
    private authService: AuthService,
    private projectService: ProjectService,
    private taskService: TaskService
  ) {}

  ngOnInit(): void {
    this.loadWorkspaces();
  }

  loadWorkspaces(): void {
    this.authService.getWorkspaces().subscribe({
      next: (workspaces) => {
        this.workspaces = workspaces;
        if (workspaces.length > 0) {
          this.selectWorkspace(workspaces[0]);
        }
      },
      error: (err) => console.error('Error fetching workspaces:', err)
    });
  }

  selectWorkspace(workspace: Workspace): void {
    this.activeWorkspace = workspace;
    this.loadProjects(workspace.id);
  }

  loadProjects(workspaceId: string): void {
    this.isLoadingProjects = true;
    this.projects = [];
    this.activeProject = null;
    this.tasks = [];
    this.activeTask = null;

    this.projectService.getProjects(workspaceId).subscribe({
      next: (projects) => {
        this.projects = projects;
        this.isLoadingProjects = false;
        if (projects.length > 0) {
          this.selectProject(projects[0]);
        }
      },
      error: (err) => {
        this.isLoadingProjects = false;
        console.error('Error fetching projects:', err);
      }
    });
  }

  selectProject(project: Project): void {
    this.activeProject = project;
    this.loadTasks(project.id);
  }

  loadTasks(projectId: string): void {
    this.isLoadingTasks = true;
    this.tasks = [];
    this.activeTask = null;

    this.taskService.getProjectTasks(projectId).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.isLoadingTasks = false;
        if (tasks.length > 0) {
          this.selectTask(tasks[0]);
        }
      },
      error: (err) => {
        this.isLoadingTasks = false;
        console.error('Error fetching tasks:', err);
      }
    });
  }

  selectTask(task: Task): void {
    this.activeTask = task;
  }

  createNewProject(): void {
    if (!this.newProjectName || !this.activeWorkspace) return;
    this.isSubmittingProject = true;

    this.projectService.createProject(
      this.newProjectName,
      this.newProjectDescription,
      this.activeWorkspace.id
    ).subscribe({
      next: (project) => {
        this.projects.push(project);
        this.selectProject(project);
        this.closeProjectModal();
      },
      error: (err) => {
        this.isSubmittingProject = false;
        console.error('Error creating project:', err);
      }
    });
  }

  createNewTask(): void {
    if (!this.newTaskTitle || !this.activeProject) return;
    this.isSubmittingTask = true;

    this.taskService.createProjectTask(
      this.activeProject.id,
      this.newTaskTitle,
      this.newTaskDescription,
      this.newTaskPriority
    ).subscribe({
      next: (task) => {
        this.tasks.unshift(task);
        this.selectTask(task);
        this.closeTaskModal();
      },
      error: (err) => {
        this.isSubmittingTask = false;
        console.error('Error creating task:', err);
      }
    });
  }

  cancelTask(task: Task): void {
    this.taskService.cancelTask(task.id).subscribe({
      next: (updatedTask) => {
        const idx = this.tasks.findIndex(t => t.id === task.id);
        if (idx !== -1) {
          this.tasks[idx] = updatedTask;
        }
        if (this.activeTask && this.activeTask.id === task.id) {
          this.activeTask = updatedTask;
        }
      },
      error: (err) => console.error('Error cancelling task:', err)
    });
  }

  openProjectModal(): void {
    this.showNewProjectModal = true;
    this.newProjectName = '';
    this.newProjectDescription = '';
  }

  closeProjectModal(): void {
    this.showNewProjectModal = false;
    this.isSubmittingProject = false;
  }

  openTaskModal(): void {
    this.showNewTaskModal = true;
    this.newTaskTitle = '';
    this.newTaskDescription = '';
    this.newTaskPriority = 'MEDIUM';
  }

  closeTaskModal(): void {
    this.showNewTaskModal = false;
    this.isSubmittingTask = false;
  }

  getStatusClass(status: string): string {
    return `badge-${status.toLowerCase()}`;
  }

  getPriorityClass(priority: string): string {
    return `priority-${priority.toLowerCase()}`;
  }
}