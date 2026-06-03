export interface Project {
  id: string;
  name: string;
  description?: string;
  workspaceId: string;
  ownerId: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}
export type TaskStatus = 
  | 'PENDING' 
  | 'PLANNING' 
  | 'ANALYZING' 
  | 'RETRIEVING' 
  | 'GENERATING' 
  | 'VALIDATING' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Task {
    id: string;
    projectId: string;
    repositoryId?: string;
    title: string;
    description: string;
    status: TaskStatus;
    priority: TaskPriority;
    createdBy: string; // userId of the creator
    assignedTo?: string; // userId of the assignee
    result?: any; // This can be structured based on the expected output of the task
    diffContent?: string; // For storing diff content if applicable
    explanation?: string; // For storing explanation of the task result if applicable
    startedAt?: string;
    completedAt?: string;
    createdAt: string;
    updatedAt: string;    
}



