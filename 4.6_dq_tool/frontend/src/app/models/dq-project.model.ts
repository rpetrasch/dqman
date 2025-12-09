export interface DqProject {
    id?: number;
    name: string;
    description: string;
    status: string;
    createdDate?: string;
    startedDate?: string;
    finishedDate?: string;
    flow?: { id?: number; name: string };
}
